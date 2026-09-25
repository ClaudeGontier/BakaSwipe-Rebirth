package com.bakaswipe.app.data

import com.bakaswipe.app.BuildConfig
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

class MalException(val code: Int, msg: String) : Exception(msg)

/** Client MyAnimeList API v2. Appels bloquants : à lancer sur Dispatchers.IO. */
class MalApi(private val prefs: Prefs) {

    companion object {
        const val REDIRECT = "bakaswipe://oauth"
        private const val BASE = "https://api.myanimelist.net/v2"
        private const val FIELDS =
            "id,title,alternative_titles,main_picture,start_season,mean,genres,num_episodes,media_type,synopsis,studios"
        private const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val rng = SecureRandom()

    fun clientId(): String = prefs.clientId.ifBlank { BuildConfig.MAL_CLIENT_ID }

    private fun randomString(n: Int) = buildString { repeat(n) { append(CHARS[rng.nextInt(CHARS.length)]) } }

    // ---------------- OAuth2 (PKCE "plain", seul mode supporté par MAL) ----------------

    fun buildAuthUrl(): String {
        val verifier = randomString(96)
        val state = randomString(16)
        prefs.pkceVerifier = verifier
        prefs.pkceState = state
        return "https://myanimelist.net/v1/oauth2/authorize".toHttpUrl().newBuilder()
            .addQueryParameter("response_type", "code")
            .addQueryParameter("client_id", clientId())
            .addQueryParameter("code_challenge", verifier)
            .addQueryParameter("code_challenge_method", "plain")
            .addQueryParameter("state", state)
            .addQueryParameter("redirect_uri", REDIRECT)
            .build().toString()
    }

    fun exchangeCode(code: String, state: String?) {
        if (state != prefs.pkceState) throw MalException(0, "State OAuth invalide, recommence la connexion")
        tokenRequest(
            FormBody.Builder()
                .add("client_id", clientId())
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT)
                .add("code_verifier", prefs.pkceVerifier)
                .build()
        )
    }

    /**
     * Synchronisé : MAL fait tourner le refresh token, deux refresh concurrents
     * feraient échouer le second. Si le token a déjà changé, un autre appel l'a renouvelé.
     */
    @Synchronized
    private fun refresh(expired: String): Boolean {
        if (prefs.accessToken != expired) return true
        val rt = prefs.refreshToken
        if (rt.isBlank()) return false
        return try {
            tokenRequest(
                FormBody.Builder()
                    .add("client_id", clientId())
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", rt)
                    .build()
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun tokenRequest(body: FormBody) {
        val req = Request.Builder().url("https://myanimelist.net/v1/oauth2/token").post(body).build()
        http.newCall(req).execute().use { r ->
            val txt = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw MalException(r.code, "Token refusé (${r.code}) : ${txt.take(200)}")
            val j = JSONObject(txt)
            prefs.accessToken = j.getString("access_token")
            prefs.refreshToken = j.optString("refresh_token", prefs.refreshToken)
        }
    }

    // ---------------- Requêtes authentifiées ----------------

    private fun call(retry: Boolean = true, build: (Request.Builder) -> Request.Builder): String {
        val token = prefs.accessToken
        val req = build(Request.Builder())
            .header("Authorization", "Bearer $token")
            .build()
        http.newCall(req).execute().use { r ->
            if (r.code == 401 && retry && refresh(token)) return call(false, build)
            val txt = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw MalException(r.code, "MAL HTTP ${r.code} : ${txt.take(200)}")
            return txt
        }
    }

    private fun getJson(url: HttpUrl): JSONObject = JSONObject(call { it.url(url).get() })

    private fun url(path: String, vararg q: Pair<String, String>): HttpUrl {
        val b = "$BASE$path".toHttpUrl().newBuilder()
        q.forEach { (k, v) -> b.addQueryParameter(k, v) }
        return b.build()
    }

    private fun parseNodes(j: JSONObject): List<Anime> {
        val arr = j.optJSONArray("data") ?: return emptyList()
        return (0 until arr.length()).map { Anime.fromNode(arr.getJSONObject(it).getJSONObject("node")) }
    }

    fun me(): String = getJson(url("/users/@me")).optString("name")

    fun ranking(type: String, limit: Int, offset: Int, nsfw: Boolean): List<Anime> = parseNodes(
        getJson(
            url(
                "/anime/ranking",
                "ranking_type" to type, "limit" to "$limit", "offset" to "$offset",
                "fields" to FIELDS, "nsfw" to "$nsfw",
            )
        )
    )

    /** Saison triée par nombre de membres => les premiers résultats sont les plus populaires. */
    fun season(year: Int, season: String, limit: Int, nsfw: Boolean): List<Anime> = parseNodes(
        getJson(
            url(
                "/anime/season/$year/$season",
                "sort" to "anime_num_list_users", "limit" to "$limit",
                "fields" to FIELDS, "nsfw" to "$nsfw",
            )
        )
    )

    /** Liste complète de l'utilisateur (pagination suivie automatiquement). */
    fun myList(full: Boolean = true): List<ListEntry> {
        val out = mutableListOf<ListEntry>()
        val fields = if (full) "list_status,$FIELDS" else "list_status"
        var next: HttpUrl? = url("/users/@me/animelist", "fields" to fields, "limit" to "1000", "nsfw" to "true")
        while (next != null) {
            val j = getJson(next)
            val arr = j.optJSONArray("data") ?: break
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val ls = o.optJSONObject("list_status")
                out += ListEntry(
                    anime = Anime.fromNode(o.getJSONObject("node")),
                    status = MalStatus.from(ls?.optString("status")),
                    score = ls?.optInt("score") ?: 0,
                    watched = ls?.optInt("num_episodes_watched") ?: 0,
                    updatedAt = ls?.optString("updated_at"),
                )
            }
            next = j.optJSONObject("paging")?.optString("next")?.takeIf { it.isNotBlank() }?.toHttpUrlOrNull()
        }
        return out
    }

    fun updateStatus(id: Int, status: MalStatus, score: Int?, watched: Int?) {
        val b = FormBody.Builder().add("status", status.api)
        if (score != null) b.add("score", "$score")
        if (watched != null) b.add("num_watched_episodes", "$watched")
        call { it.url("$BASE/anime/$id/my_list_status").patch(b.build()) }
    }

    /** "Pas vu" = aucun statut sur MAL => suppression de l'entrée. 404 = déjà absent, OK. */
    fun deleteStatus(id: Int) {
        try {
            call { it.url("$BASE/anime/$id/my_list_status").delete() }
        } catch (e: MalException) {
            if (e.code != 404) throw e
        }
    }
}

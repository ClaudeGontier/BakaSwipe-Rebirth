package com.bakaswipe.app.data

import android.content.Context
import org.json.JSONArray
import java.util.Calendar

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("bakaswipe", Context.MODE_PRIVATE)

    private fun str(key: String) = sp.getString(key, "") ?: ""
    private fun put(key: String, v: String) = sp.edit().putString(key, v).apply()

    var clientId: String
        get() = str("client_id")
        set(v) = put("client_id", v)
    var accessToken: String
        get() = str("access_token")
        set(v) = put("access_token", v)
    var refreshToken: String
        get() = str("refresh_token")
        set(v) = put("refresh_token", v)
    var pkceVerifier: String
        get() = str("pkce_verifier")
        set(v) = put("pkce_verifier", v)
    var pkceState: String
        get() = str("pkce_state")
        set(v) = put("pkce_state", v)

    fun loadFilters(): Filters {
        val d = Filters(yearMax = Calendar.getInstance().get(Calendar.YEAR))
        return Filters(
            mode = DeckMode.entries.firstOrNull { it.name == sp.getString("mode", null) } ?: d.mode,
            yearFilter = sp.getBoolean("year_filter", d.yearFilter),
            yearMin = sp.getInt("year_min", d.yearMin),
            yearMax = sp.getInt("year_max", d.yearMax),
            mediaTypes = sp.getStringSet("types", null)?.toSet() ?: d.mediaTypes,
            nsfw = sp.getBoolean("nsfw", d.nsfw),
            englishTitles = sp.getBoolean("english", d.englishTitles),
        )
    }

    fun saveFilters(f: Filters) {
        sp.edit()
            .putString("mode", f.mode.name)
            .putBoolean("year_filter", f.yearFilter)
            .putInt("year_min", f.yearMin)
            .putInt("year_max", f.yearMax)
            .putStringSet("types", f.mediaTypes)
            .putBoolean("nsfw", f.nsfw)
            .putBoolean("english", f.englishTitles)
            .apply()
    }

    /** Liste locale "Pas vu" (MAL ne stocke pas ce statut). Plus récent en premier. */
    fun loadSkipped(): List<Anime> = try {
        val a = JSONArray(sp.getString("skipped", "[]") ?: "[]")
        (0 until a.length()).map { Anime.fromNode(a.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }

    fun saveSkipped(list: List<Anime>) {
        put("skipped", JSONArray(list.map { it.toJson() }).toString())
    }
}

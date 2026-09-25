package com.bakaswipe.app.data

import org.json.JSONArray
import org.json.JSONObject

data class Anime(
    val id: Int,
    val title: String,
    val titleEn: String?,
    val picture: String?,
    val year: Int?,
    val season: String?,
    val mean: Double?,
    val episodes: Int,
    val mediaType: String?,
    val genres: List<String>,
    val synopsis: String?,
    val studios: List<String>,
) {
    fun displayTitle(en: Boolean): String = if (en && !titleEn.isNullOrBlank()) titleEn else title

    /** Clé de format normalisée pour les filtres (tv, movie, ova, ona, special, music). */
    fun typeKey(): String = when (mediaType) {
        "tv_special" -> "special"
        "cm", "pv" -> "music"
        null, "", "unknown" -> "tv"
        else -> mediaType
    }

    /** Sérialisé au même format qu'un "node" MAL, pour réutiliser fromNode(). */
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        titleEn?.let { put("alternative_titles", JSONObject().put("en", it)) }
        picture?.let { put("main_picture", JSONObject().put("large", it)) }
        if (year != null) put("start_season", JSONObject().put("year", year).put("season", season ?: ""))
        mean?.let { put("mean", it) }
        put("num_episodes", episodes)
        mediaType?.let { put("media_type", it) }
        put("genres", JSONArray(genres.map { JSONObject().put("name", it) }))
        synopsis?.let { put("synopsis", it) }
        put("studios", JSONArray(studios.map { JSONObject().put("name", it) }))
    }

    companion object {
        fun fromNode(o: JSONObject): Anime {
            val pic = o.optJSONObject("main_picture")
            val ss = o.optJSONObject("start_season")
            fun names(key: String): List<String> {
                val a = o.optJSONArray(key) ?: return emptyList()
                return (0 until a.length()).map { a.getJSONObject(it).optString("name") }
            }
            return Anime(
                id = o.getInt("id"),
                title = o.optString("title"),
                titleEn = o.optJSONObject("alternative_titles")?.optString("en")?.takeIf { it.isNotBlank() },
                picture = pic?.optString("large")?.takeIf { it.isNotBlank() }
                    ?: pic?.optString("medium")?.takeIf { it.isNotBlank() },
                year = ss?.optInt("year")?.takeIf { it > 0 },
                season = ss?.optString("season")?.takeIf { it.isNotBlank() },
                mean = if (o.has("mean")) o.optDouble("mean") else null,
                episodes = o.optInt("num_episodes"),
                mediaType = o.optString("media_type").takeIf { it.isNotBlank() },
                genres = names("genres"),
                synopsis = o.optString("synopsis").takeIf { it.isNotBlank() },
                studios = names("studios"),
            )
        }
    }
}

enum class MalStatus(val api: String, val label: String) {
    COMPLETED("completed", "Completed"),
    WATCHING("watching", "Watching"),
    PLAN("plan_to_watch", "Plan to Watch"),
    ON_HOLD("on_hold", "On-Hold"),
    DROPPED("dropped", "Dropped");

    companion object {
        fun from(s: String?): MalStatus? = entries.firstOrNull { it.api == s }
    }
}

/** status == null  =>  entrée locale "Pas vu" (rien sur MAL). */
data class ListEntry(
    val anime: Anime,
    val status: MalStatus?,
    val score: Int,
    val watched: Int,
    val updatedAt: String?,
)

enum class DeckMode(val label: String, val help: String) {
    POPULAR("Populaires", "Que des animes connus (top popularité)."),
    MIX("Mix", "Populaires + moins connus, bon équilibre."),
    RANDOM("Full random", "N'importe quoi du catalogue MAL, obscur inclus."),
}

val ALL_TYPES = listOf("tv", "movie", "ova", "ona", "special", "music")

data class Filters(
    val mode: DeckMode = DeckMode.MIX,
    val yearFilter: Boolean = false,
    val yearMin: Int = 2000,
    val yearMax: Int = 2026,
    val mediaTypes: Set<String> = setOf("tv", "movie", "ova", "ona", "special"),
    val nsfw: Boolean = false,
    val englishTitles: Boolean = false,
)

fun seasonFr(s: String?): String = when (s) {
    "winter" -> "Hiver"
    "spring" -> "Printemps"
    "summer" -> "Été"
    "fall" -> "Automne"
    else -> ""
}

fun scoreLabel(s: Int): String = when (s) {
    10 -> "Chef-d'œuvre"
    9 -> "Excellent"
    8 -> "Très bon"
    7 -> "Bon"
    6 -> "Correct"
    5 -> "Moyen"
    4 -> "Mauvais"
    3 -> "Très mauvais"
    2 -> "Horrible"
    1 -> "Catastrophique"
    else -> "Sans note"
}

package com.bakaswipe.app.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bakaswipe.app.data.Anime
import com.bakaswipe.app.data.DeckMode
import com.bakaswipe.app.data.Filters
import com.bakaswipe.app.data.ListEntry
import com.bakaswipe.app.data.MalApi
import com.bakaswipe.app.data.MalException
import com.bakaswipe.app.data.MalStatus
import com.bakaswipe.app.data.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    private val api = MalApi(prefs)

    var loggedIn by mutableStateOf(prefs.accessToken.isNotBlank()); private set
    var userName by mutableStateOf<String?>(null); private set
    var authError by mutableStateOf<String?>(null); private set
    var filters by mutableStateOf(prefs.loadFilters()); private set
    var toast by mutableStateOf<String?>(null)

    // --- Deck de swipe ---
    val deck = mutableStateListOf<Anime>()
    var deckLoading by mutableStateOf(false); private set
    var deckError by mutableStateOf<String?>(null); private set
    var lastAction by mutableStateOf<Pair<Anime, MalStatus?>?>(null); private set

    // --- Listes ---
    var library by mutableStateOf<List<ListEntry>?>(null); private set
    var libraryLoading by mutableStateOf(false); private set
    var skipped by mutableStateOf(prefs.loadSkipped()); private set

    private val listedIds = mutableSetOf<Int>()
    private val seenIds = mutableSetOf<Int>()
    private var fillJob: Job? = null
    private val writeLock = Mutex()

    init {
        if (loggedIn) bootstrap()
    }

    // ================= Auth =================

    fun startLogin(clientId: String): String? {
        if (clientId.isBlank()) {
            authError = "Renseigne ton Client ID MAL"
            return null
        }
        authError = null
        prefs.clientId = clientId.trim()
        return api.buildAuthUrl()
    }

    fun handleRedirect(uri: Uri) {
        val code = uri.getQueryParameter("code")
        if (code == null) {
            authError = uri.getQueryParameter("error_description") ?: uri.getQueryParameter("error") ?: "Connexion annulée"
            return
        }
        val state = uri.getQueryParameter("state")
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { api.exchangeCode(code, state) }
                authError = null
                loggedIn = true
                bootstrap()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                authError = e.message
            }
        }
    }

    fun logout() {
        prefs.accessToken = ""
        prefs.refreshToken = ""
        fillJob?.cancel()
        deckLoading = false
        deck.clear(); listedIds.clear(); seenIds.clear()
        library = null; userName = null; lastAction = null
        loggedIn = false
    }

    private fun bootstrap() {
        viewModelScope.launch {
            try {
                val (name, ids) = withContext(Dispatchers.IO) {
                    api.me() to api.myList(full = false).map { it.anime.id }
                }
                userName = name
                listedIds.clear(); listedIds += ids
                refill()
            } catch (e: CancellationException) {
                throw e
            } catch (e: MalException) {
                if (e.code == 401) {
                    logout(); authError = "Session expirée, reconnecte-toi"
                } else deckError = e.message
            } catch (e: Exception) {
                deckError = e.message ?: "Erreur réseau"
            }
        }
    }

    // ================= Deck =================

    fun refill(force: Boolean = false) {
        if (!loggedIn || fillJob?.isActive == true) return
        if (!force && deck.size >= 6) return
        fillJob = viewModelScope.launch {
            deckLoading = true
            deckError = null
            try {
                var attempts = 0
                while (deck.size < 12 && attempts < 10) {
                    attempts++
                    val f = filters
                    val batch = withContext(Dispatchers.IO) { fetchBatch(f) }
                    val skippedIds = skipped.map { it.id }.toSet()
                    val fresh = batch.filter { a ->
                        a.id !in listedIds && a.id !in seenIds && a.id !in skippedIds &&
                            a.typeKey() in f.mediaTypes &&
                            (!f.yearFilter || (a.year != null && a.year in f.yearMin..f.yearMax))
                    }.shuffled().take(5)
                    fresh.forEach { seenIds += it.id }
                    deck.addAll(fresh)
                }
                if (deck.isEmpty()) deckError = "Aucun anime trouvé avec ces filtres, élargis-les un peu 🤷"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                deckError = e.message ?: "Erreur réseau"
            }
            deckLoading = false
        }
    }

    /** Tirage semi-aléatoire selon le mode + filtre d'années. */
    private fun fetchBatch(f: Filters): List<Anime> = if (f.yearFilter) {
        val year = Random.nextInt(f.yearMin, f.yearMax + 1)
        val season = listOf("winter", "spring", "summer", "fall").random()
        val limit = when (f.mode) {
            DeckMode.POPULAR -> 25
            DeckMode.MIX -> 80
            DeckMode.RANDOM -> 400
        }
        api.season(year, season, limit, f.nsfw)
    } else {
        val depth = when (f.mode) {
            DeckMode.POPULAR -> 500
            DeckMode.MIX -> 3000
            DeckMode.RANDOM -> 14000
        }
        val offset = Random.nextInt(0, depth / 50) * 50
        api.ranking("bypopularity", 50, offset, f.nsfw)
    }

    fun updateFilters(f: Filters) {
        filters = f
        prefs.saveFilters(f)
        fillJob?.cancel()
        seenIds.removeAll(deck.map { it.id }.toSet())
        deck.clear()
        deckLoading = false
        refill(force = true)
    }

    // ================= Actions de swipe =================

    fun markCompleted(a: Anime, score: Int) =
        swipeWrite(a, MalStatus.COMPLETED) { api.updateStatus(a.id, MalStatus.COMPLETED, score, a.episodes.takeIf { it > 0 }) }

    fun markStatus(a: Anime, s: MalStatus) = swipeWrite(a, s) { api.updateStatus(a.id, s, null, null) }

    fun markSkipped(a: Anime) {
        deck.remove(a)
        skipped = listOf(a) + skipped.filterNot { it.id == a.id }
        prefs.saveSkipped(skipped)
        lastAction = a to null
        refill()
    }

    private fun swipeWrite(a: Anime, s: MalStatus, block: () -> Unit) {
        deck.remove(a)
        listedIds += a.id
        lastAction = a to s
        refill()
        viewModelScope.launch {
            try {
                writeLock.withLock { withContext(Dispatchers.IO) { block() } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast = "Échec MAL : ${e.message}"
                listedIds -= a.id
                if (lastAction?.first?.id == a.id) lastAction = null
                // Déjà remis dans le deck si le swipe a été annulé entre-temps
                if (deck.none { it.id == a.id }) deck.add(0, a)
            }
        }
    }

    fun undo() {
        val (a, s) = lastAction ?: return
        lastAction = null
        if (s == null) {
            skipped = skipped.filterNot { it.id == a.id }
            prefs.saveSkipped(skipped)
        } else {
            listedIds -= a.id
            viewModelScope.launch {
                try {
                    writeLock.withLock { withContext(Dispatchers.IO) { api.deleteStatus(a.id) } }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    toast = "Annulation échouée : ${e.message}"
                }
            }
        }
        if (deck.none { it.id == a.id }) deck.add(0, a)
    }

    // ================= Listes =================

    fun loadLibrary() {
        if (!loggedIn || libraryLoading) return
        viewModelScope.launch {
            libraryLoading = true
            try {
                val lib = withContext(Dispatchers.IO) { api.myList(full = true) }
                library = lib
                listedIds.clear(); listedIds += lib.map { it.anime.id }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast = "Impossible de charger ta liste : ${e.message}"
            }
            libraryLoading = false
        }
    }

    /**
     * Modifie une entrée depuis les listes.
     * status == null => retirer (de MAL, ou de "Pas vu" si l'entrée est locale).
     */
    fun editEntry(e: ListEntry, status: MalStatus?, score: Int) {
        val a = e.anime
        val wasSkipped = e.status == null
        if (wasSkipped && status == null) {
            removeSkipped(a.id)
            return
        }
        viewModelScope.launch {
            try {
                writeLock.withLock {
                    withContext(Dispatchers.IO) {
                        if (status == null) api.deleteStatus(a.id)
                        else api.updateStatus(
                            a.id, status, score,
                            if (status == MalStatus.COMPLETED && a.episodes > 0) a.episodes else null,
                        )
                    }
                }
                if (status == null) {
                    // Retiré de MAL => passe en "Pas vu" pour ne pas revenir dans le deck
                    listedIds -= a.id
                    skipped = listOf(a) + skipped.filterNot { it.id == a.id }
                    prefs.saveSkipped(skipped)
                } else {
                    listedIds += a.id
                    // Retiré des "Pas vu" seulement une fois l'écriture MAL réussie
                    if (wasSkipped) removeSkipped(a.id)
                }
                loadLibrary()
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                toast = "Échec : ${ex.message}"
            }
        }
    }

    private fun removeSkipped(id: Int) {
        skipped = skipped.filterNot { it.id == id }
        prefs.saveSkipped(skipped)
    }

    fun clearSkipped() {
        skipped = emptyList()
        prefs.saveSkipped(skipped)
    }
}

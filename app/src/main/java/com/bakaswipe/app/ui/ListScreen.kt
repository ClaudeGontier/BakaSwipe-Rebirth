package com.bakaswipe.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bakaswipe.app.data.ListEntry
import com.bakaswipe.app.data.MalStatus

private val TABS: List<Pair<String, MalStatus?>> =
    MalStatus.entries.map { it.label to it } + ("Pas vu" to null)

@Composable
fun ListScreen(vm: AppViewModel) {
    LaunchedEffect(Unit) { vm.loadLibrary() }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sort by rememberSaveable { mutableIntStateOf(0) } // 0 récent, 1 note, 2 titre, 3 année
    var query by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<ListEntry?>(null) }
    val en = vm.filters.englishTitles

    val lib = vm.library.orEmpty()
    val skippedEntries = vm.skipped.map { ListEntry(it, null, 0, 0, null) }
    fun entriesFor(s: MalStatus?) = if (s == null) skippedEntries else lib.filter { it.status == s }

    val status = TABS[tab].second
    val q = query.trim().lowercase()
    val shown = entriesFor(status)
        .filter { q.isEmpty() || it.anime.title.lowercase().contains(q) || (it.anime.titleEn?.lowercase()?.contains(q) == true) }
        .let { l ->
            when (sort) {
                1 -> l.sortedWith(compareByDescending<ListEntry> { it.score }.thenBy { it.anime.title })
                2 -> l.sortedBy { it.anime.displayTitle(en).lowercase() }
                3 -> l.sortedByDescending { it.anime.year ?: 0 }
                else -> if (status == null) l else l.sortedByDescending { it.updatedAt ?: "" }
            }
        }

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
            TABS.forEachIndexed { i, (label, s) ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = { Text("$label (${entriesFor(s).size})") },
                )
            }
        }
        if (vm.libraryLoading) LinearProgressIndicator(Modifier.fillMaxWidth())

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Rechercher…") }, singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { vm.loadLibrary() }) { Text("↻") }
        }
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Récent", "Note", "Titre", "Année").forEachIndexed { i, l ->
                FilterChip(selected = sort == i, onClick = { sort = i }, label = { Text(l) })
            }
        }

        if (shown.isEmpty() && !vm.libraryLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Rien ici pour l'instant", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(shown, key = { it.anime.id }) { e -> EntryRow(e, en) { editing = e } }
            }
        }
    }

    editing?.let { e ->
        EditEntryDialog(
            entry = e, en = en,
            onDismiss = { editing = null },
            onSave = { s, score -> editing = null; vm.editEntry(e, s, score) },
        )
    }
}

@Composable
private fun EntryRow(e: ListEntry, en: Boolean, onClick: () -> Unit) {
    val a = e.anime
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = a.picture, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 54.dp, height = 76.dp).clip(RoundedCornerShape(10.dp)),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(a.displayTitle(en), fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val eps = when {
                e.status == null -> if (a.episodes > 0) "${a.episodes} ép." else null
                a.episodes > 0 -> "${e.watched}/${a.episodes} ép."
                e.watched > 0 -> "${e.watched} ép."
                else -> null
            }
            Text(
                listOfNotNull(a.year?.toString(), a.mediaType?.uppercase(), eps).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (e.score > 0) {
            Box(
                Modifier.size(42.dp).background(StarYellow.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("${e.score}", color = StarYellow, fontWeight = FontWeight.Black) }
        }
    }
}

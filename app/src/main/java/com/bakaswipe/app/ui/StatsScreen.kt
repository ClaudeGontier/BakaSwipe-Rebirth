package com.bakaswipe.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bakaswipe.app.data.MalStatus
import com.bakaswipe.app.data.scoreLabel
import java.util.Locale

@Composable
fun StatsScreen(vm: AppViewModel) {
    LaunchedEffect(Unit) { if (vm.library == null) vm.loadLibrary() }
    val lib = vm.library
    if (lib == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val scored = lib.filter { it.score > 0 }
    val mean = if (scored.isEmpty()) 0.0 else scored.map { it.score }.average()
    val episodes = lib.sumOf { it.watched }
    val completed = lib.filter { it.status == MalStatus.COMPLETED }
    val dist = (1..10).associateWith { s -> scored.count { it.score == s } }
    val maxBar = (dist.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val genres = completed.flatMap { it.anime.genres }.groupingBy { it }.eachCount()
        .entries.sortedByDescending { it.value }.take(8)
    val decades = completed.mapNotNull { it.anime.year?.let { y -> y / 10 * 10 } }.groupingBy { it }.eachCount()
        .entries.sortedBy { it.key }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(vm.userName?.let { "Stats de $it" } ?: "Stats", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigStat("${lib.size}", "dans ta liste", Modifier.weight(1f))
            BigStat(if (scored.isEmpty()) "–" else String.format(Locale.US, "%.2f", mean), "note moyenne", Modifier.weight(1f), StarYellow)
            BigStat("$episodes", "épisodes vus", Modifier.weight(1f), Cyan)
        }

        Panel("Par statut") {
            MalStatus.entries.forEach { s -> KeyValue(s.label, "${lib.count { it.status == s }}") }
            KeyValue("Pas vu (local)", "${vm.skipped.size}")
        }

        Panel("Répartition des notes") {
            if (scored.isEmpty()) Text("Aucune note pour l'instant", color = MaterialTheme.colorScheme.onSurfaceVariant)
            (10 downTo 1).forEach { s ->
                val n = dist[s] ?: 0
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("$s", Modifier.width(26.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    Box(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Box(
                            Modifier
                                .fillMaxWidth(n.toFloat() / maxBar)
                                .height(18.dp)
                                .background(if (n > 0) StarYellow else Color.Transparent, RoundedCornerShape(6.dp)),
                        )
                    }
                    Text("$n", Modifier.width(34.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (scored.isNotEmpty()) {
                val top = dist.maxByOrNull { it.value }?.key ?: 0
                Spacer(Modifier.height(6.dp))
                Text("Note la plus donnée : $top (${scoreLabel(top)})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (genres.isNotEmpty()) {
            Panel("Genres favoris (Completed)") {
                genres.forEach { (g, n) -> KeyValue(g, "$n") }
            }
        }
        if (decades.isNotEmpty()) {
            Panel("Par décennie (Completed)") {
                decades.forEach { (d, n) -> KeyValue("Années $d", "$n") }
            }
        }
    }
}

@Composable
private fun BigStat(value: String, label: String, modifier: Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Panel(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun KeyValue(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(k, Modifier.weight(1f))
        Text(v, fontWeight = FontWeight.Bold)
    }
}

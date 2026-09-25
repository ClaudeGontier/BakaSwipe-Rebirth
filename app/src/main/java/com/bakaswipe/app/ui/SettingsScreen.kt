package com.bakaswipe.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bakaswipe.app.data.ALL_TYPES
import com.bakaswipe.app.data.DeckMode
import java.util.Calendar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel) {
    var f by remember(vm.filters) { mutableStateOf(vm.filters) }
    val maxYear = Calendar.getInstance().get(Calendar.YEAR) + 1

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Tirage des animes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)

        Section("Mode")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckMode.entries.forEach { m ->
                FilterChip(selected = f.mode == m, onClick = { f = f.copy(mode = m) }, label = { Text(m.label) })
            }
        }
        Text(f.mode.help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        HorizontalDivider()
        SwitchRow("Filtrer par année de sortie", f.yearFilter) { f = f.copy(yearFilter = it) }
        RangeSlider(
            value = f.yearMin.toFloat()..f.yearMax.toFloat(),
            onValueChange = { r -> f = f.copy(yearMin = r.start.roundToInt(), yearMax = r.endInclusive.roundToInt()) },
            valueRange = 1960f..maxYear.toFloat(),
            enabled = f.yearFilter,
        )
        Text(
            if (f.yearFilter) "Sortis entre ${f.yearMin} et ${f.yearMax}" else "Toutes les années",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Avant 2000" to (1960 to 1999), "2000–2015" to (2000 to 2015), "Après 2015" to (2016 to maxYear)).forEach { (l, r) ->
                OutlinedButton(onClick = { f = f.copy(yearFilter = true, yearMin = r.first, yearMax = r.second) }) { Text(l) }
            }
        }

        HorizontalDivider()
        Section("Formats")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ALL_TYPES.forEach { t ->
                val on = t in f.mediaTypes
                FilterChip(
                    selected = on,
                    onClick = {
                        val next = if (on) f.mediaTypes - t else f.mediaTypes + t
                        if (next.isNotEmpty()) f = f.copy(mediaTypes = next)
                    },
                    label = { Text(t.uppercase()) },
                )
            }
        }

        HorizontalDivider()
        SwitchRow("Titres anglais", f.englishTitles) { f = f.copy(englishTitles = it) }
        SwitchRow("Inclure le contenu NSFW", f.nsfw) { f = f.copy(nsfw = it) }

        Button(
            onClick = { vm.updateFilters(f) },
            enabled = f != vm.filters,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) { Text("Appliquer", fontWeight = FontWeight.Bold) }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Section("Compte")
        Text("Connecté en tant que ${vm.userName ?: "…"}")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.logout() }) { Text("Déconnexion") }
            TextButton(onClick = { vm.clearSkipped() }, enabled = vm.skipped.isNotEmpty()) {
                Text("Vider les « Pas vu » (${vm.skipped.size})", color = SwipeRed)
            }
        }
    }
}

@Composable
private fun Section(t: String) {
    Text(t, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

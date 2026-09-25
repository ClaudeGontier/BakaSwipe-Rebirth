package com.bakaswipe.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bakaswipe.app.data.ListEntry
import com.bakaswipe.app.data.MalStatus
import com.bakaswipe.app.data.scoreLabel

/** Grille 1–10 façon MAL + libellé. 0 = sans note. */
@Composable
fun ScorePicker(score: Int, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0..1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in 1..5) {
                    val n = row * 5 + c
                    val sel = score == n
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(if (sel) StarYellow else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .border(1.dp, if (sel) StarYellow else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable { onChange(if (sel) 0 else n) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "$n", fontWeight = FontWeight.Bold,
                            color = if (sel) Color(0xFF2A1E00) else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        Text(
            if (score == 0) "Sans note" else "$score · ${scoreLabel(score)}",
            color = if (score == 0) MaterialTheme.colorScheme.onSurfaceVariant else StarYellow,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun ScoreDialog(title: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var score by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Completed ✓") },
        text = {
            Column {
                Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                ScorePicker(score) { score = it }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(score) }) { Text("Valider") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/** Sélecteur du swipe vers le haut : les 4 autres statuts MAL. */
@Composable
fun StatusDialog(title: String, onDismiss: () -> Unit, onPick: (MalStatus) -> Unit) {
    val options = listOf(
        MalStatus.WATCHING to "▶  En cours",
        MalStatus.PLAN to "🕒  À voir plus tard",
        MalStatus.ON_HOLD to "⏸  En pause",
        MalStatus.DROPPED to "✕  Abandonné",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEach { (s, label) ->
                    OutlinedButton(
                        onClick = { onPick(s) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text(s.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/** Édition d'une entrée depuis les listes (statut + note, ou retrait). */
@Composable
fun EditEntryDialog(entry: ListEntry, en: Boolean, onDismiss: () -> Unit, onSave: (MalStatus?, Int) -> Unit) {
    val isSkipped = entry.status == null
    var status by remember { mutableStateOf(entry.status ?: MalStatus.COMPLETED) }
    var remove by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(entry.score) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.anime.displayTitle(en), maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                MalStatus.entries.forEach { s ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = !remove && status == s, onClick = { status = s; remove = false })
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = !remove && status == s, onClick = null)
                        Text(s.label, Modifier.padding(start = 8.dp))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().selectable(selected = remove, onClick = { remove = true }).padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = remove, onClick = null)
                    Text(
                        if (isSkipped) "Retirer des « Pas vu »" else "Pas vu (retirer de MAL)",
                        Modifier.padding(start = 8.dp),
                        color = SwipeRed,
                    )
                }
                if (!remove) {
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    ScorePicker(score) { score = it }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(if (remove) null else status, score) }) { Text("Enregistrer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

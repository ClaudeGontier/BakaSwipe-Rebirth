package com.bakaswipe.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bakaswipe.app.data.Anime
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

enum class SwipeDir { LEFT, RIGHT, UP }

@Composable
fun SwipeScreen(vm: AppViewModel) {
    LaunchedEffect(Unit) { vm.refill() }

    val top = vm.deck.firstOrNull()
    val next = vm.deck.getOrNull(1)
    val en = vm.filters.englishTitles
    var pendingComplete by remember { mutableStateOf<Anime?>(null) }
    var pendingStatus by remember { mutableStateOf<Anime?>(null) }
    var resetTick by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("BakaSwipe", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                val f = vm.filters
                Text(
                    f.mode.label + if (f.yearFilter) " · ${f.yearMin}–${f.yearMax}" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { vm.undo() }, enabled = vm.lastAction != null) { Text("↶ Annuler") }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                top == null && (vm.deckLoading || vm.deckError == null) -> CircularProgressIndicator()
                top == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(vm.deckError ?: "", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { vm.refill(force = true) }) { Text("Réessayer") }
                }
                else -> {
                    if (next != null) {
                        key(next.id) {
                            AnimeCard(
                                next, en,
                                Modifier.fillMaxSize().graphicsLayer { scaleX = 0.94f; scaleY = 0.94f; translationY = 28f; alpha = 0.7f },
                            )
                        }
                    }
                    key(top.id) {
                        SwipeableCard(top, en, resetTick) { dir ->
                            when (dir) {
                                SwipeDir.LEFT -> vm.markSkipped(top)
                                SwipeDir.RIGHT -> pendingComplete = top
                                SwipeDir.UP -> pendingStatus = top
                            }
                        }
                    }
                }
            }
        }

        // Boutons (alternative au swipe)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundAction("✕", SwipeRed, 60, "Pas vu", enabled = top != null) { top?.let { vm.markSkipped(it) } }
            RoundAction("↑", StarYellow, 50, "Statut", enabled = top != null) { pendingStatus = top }
            RoundAction("✓", SwipeGreen, 60, "Vu", enabled = top != null) { pendingComplete = top }
        }
    }

    pendingComplete?.let { a ->
        ScoreDialog(
            title = a.displayTitle(en),
            onDismiss = { pendingComplete = null; resetTick++ },
            onConfirm = { score -> pendingComplete = null; vm.markCompleted(a, score) },
        )
    }
    pendingStatus?.let { a ->
        StatusDialog(
            title = a.displayTitle(en),
            onDismiss = { pendingStatus = null; resetTick++ },
            onPick = { s -> pendingStatus = null; vm.markStatus(a, s) },
        )
    }
}

@Composable
private fun RoundAction(symbol: String, color: Color, sizeDp: Int, label: String, enabled: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(sizeDp.dp).border(2.dp, color.copy(alpha = 0.6f), CircleShape),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = color,
            ),
        ) { Text(symbol, fontSize = (sizeDp / 2.4).sp, fontWeight = FontWeight.Black, color = color) }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwipeableCard(anime: Anime, en: Boolean, resetTick: Int, onSwiped: (SwipeDir) -> Unit) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val callback by rememberUpdatedState(onSwiped)
    var widthPx by remember { mutableStateOf(1f) }

    // Dialogue annulé => la carte revient au centre
    LaunchedEffect(resetTick) { offset.animateTo(Offset.Zero, spring()) }

    val o = offset.value
    val threshold = widthPx * 0.28f
    val hint: SwipeDir? = when {
        o.y < -threshold && abs(o.y) > abs(o.x) -> SwipeDir.UP
        o.x > threshold -> SwipeDir.RIGHT
        o.x < -threshold -> SwipeDir.LEFT
        else -> null
    }
    val progress = min(1f, maxOf(abs(o.x), abs(o.y)) / threshold.coerceAtLeast(1f))

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = offset.value.x
                translationY = offset.value.y
                rotationZ = offset.value.x / 28f
            }
            .pointerInput(Unit) {
                // Taille relue à chaque geste (elle change après une rotation)
                detectDragGestures(
                    onDragStart = { widthPx = size.width.toFloat() },
                    onDragEnd = {
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val cur = offset.value
                        val th = w * 0.28f
                        val dir = when {
                            cur.y < -th && abs(cur.y) > abs(cur.x) -> SwipeDir.UP
                            cur.x > th -> SwipeDir.RIGHT
                            cur.x < -th -> SwipeDir.LEFT
                            else -> null
                        }
                        scope.launch {
                            if (dir == null) {
                                offset.animateTo(Offset.Zero, spring())
                            } else {
                                val target = when (dir) {
                                    SwipeDir.LEFT -> Offset(-w * 1.5f, cur.y)
                                    SwipeDir.RIGHT -> Offset(w * 1.5f, cur.y)
                                    SwipeDir.UP -> Offset(cur.x, -h * 1.3f)
                                }
                                offset.animateTo(target, tween(220))
                                callback(dir)
                            }
                        }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(Offset.Zero, spring()) } },
                ) { change, drag ->
                    change.consume()
                    scope.launch { offset.snapTo(offset.value + drag) }
                }
            },
    ) {
        AnimeCard(anime, en, Modifier.fillMaxSize())
        val (label, color, align) = when (hint ?: dominant(o)) {
            SwipeDir.RIGHT -> Triple("VU ✓", SwipeGreen, Alignment.TopStart)
            SwipeDir.LEFT -> Triple("PAS VU ✕", SwipeRed, Alignment.TopEnd)
            SwipeDir.UP -> Triple("STATUT ↑", StarYellow, Alignment.BottomCenter)
            null -> Triple("", Color.Transparent, Alignment.Center)
        }
        if (label.isNotEmpty() && progress > 0.15f) {
            Text(
                label,
                Modifier
                    .align(align)
                    .padding(28.dp)
                    .graphicsLayer {
                        alpha = progress
                        rotationZ = if (align == Alignment.TopStart) -14f else if (align == Alignment.TopEnd) 14f else 0f
                    }
                    .border(4.dp, color, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                color = color,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

private fun dominant(o: Offset): SwipeDir? = when {
    o == Offset.Zero -> null
    o.y < 0 && abs(o.y) > abs(o.x) -> SwipeDir.UP
    o.x > 0 -> SwipeDir.RIGHT
    o.x < 0 -> SwipeDir.LEFT
    else -> null
}

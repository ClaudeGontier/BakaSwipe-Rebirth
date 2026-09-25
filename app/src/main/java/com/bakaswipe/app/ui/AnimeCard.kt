package com.bakaswipe.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bakaswipe.app.data.Anime
import com.bakaswipe.app.data.seasonFr
import java.util.Locale

@Composable
fun AnimeCard(a: Anime, en: Boolean, modifier: Modifier = Modifier) {
    var expanded by remember(a.id) { mutableStateOf(false) }
    Box(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { expanded = !expanded },
    ) {
        AsyncImage(
            model = a.picture,
            contentDescription = a.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.35f to Color.Transparent,
                    0.75f to Color(0xCC070B22),
                    1f to Color(0xF5070B22),
                )
            )
        )
        a.mean?.let {
            Text(
                "★ " + String.format(Locale.US, "%.2f", it),
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .background(Color(0xCC070B22), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                color = StarYellow, fontWeight = FontWeight.Bold,
            )
        }
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp).animateContentSize(),
        ) {
            Text(
                a.displayTitle(en),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
            val alt = if (en) a.title else a.titleEn
            if (alt != null && alt != a.displayTitle(en)) {
                Text(alt, color = Color(0xFFB4BEEA), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val season = a.year?.let { "${seasonFr(a.season)} $it".trim() }
                listOfNotNull(
                    season,
                    a.mediaType?.uppercase(),
                    if (a.episodes > 0) "${a.episodes} ép." else null,
                ).forEach { Pill(it) }
            }
            if (a.genres.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    a.genres.take(5).joinToString("  ·  "),
                    color = Cyan, style = MaterialTheme.typography.labelLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            a.synopsis?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    color = Color(0xFFD5DBF5),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (expanded) 14 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (a.studios.isNotEmpty() && expanded) {
                Spacer(Modifier.height(6.dp))
                Text("Studio : " + a.studios.joinToString(), color = Color(0xFF9AA5D6), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun Pill(text: String) {
    Text(
        text,
        Modifier
            .background(Color(0x33FFFFFF), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
    )
}

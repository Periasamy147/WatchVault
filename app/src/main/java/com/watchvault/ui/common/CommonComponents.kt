package com.watchvault.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.watchvault.data.entity.WatchPhoto
import com.watchvault.ui.theme.LocalVaultColors
import com.watchvault.ui.theme.WatchVaultExtraType
import java.io.File

/**
 * Compact stat tile: a large numeral over a small label, no boxed/colored container by default —
 * kept quiet so it reads as typography, not a Material "card". [accentColor] lets callers pick
 * out a figure (e.g. gold for value) without adding a background.
 */
@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, accentColor: Color? = null) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = accentColor ?: MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = WatchVaultExtraType.metadata,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

/**
 * The single shared way a watch photo (or its absence) is rendered anywhere in the app — Home's
 * featured/preview cards and Collection's cards all go through this so the "no photo" look is
 * consistent, never a plain grey box or the launcher icon.
 */
@Composable
fun WatchPhotoOrPlaceholder(
    photo: WatchPhoto?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (photo != null) {
        AsyncImage(
            model = File(photo.localPath),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        WatchSilhouettePlaceholder(modifier)
    }
}

/** Minimal watch-silhouette placeholder in the app's gold/muted palette, used wherever a watch
 *  has no photo on file yet. */
@Composable
fun WatchSilhouettePlaceholder(modifier: Modifier = Modifier) {
    val vaultColors = LocalVaultColors.current
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, vaultColors.border),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Watch,
            contentDescription = null,
            tint = vaultColors.gold.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxSize().padding(24.dp)
        )
    }
}

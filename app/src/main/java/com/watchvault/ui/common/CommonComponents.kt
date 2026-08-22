package com.watchvault.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.watchvault.data.entity.WatchPhoto
import com.watchvault.ui.theme.LocalVaultColors
import com.watchvault.ui.theme.Radius
import com.watchvault.ui.theme.Spacing
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

/**
 * The one section-header treatment used across screens: an uppercase, letter-spaced label
 * (matching the "OWNERSHIP"/"SERVICE" dividers on Watch Detail) with an optional trailing action
 * (e.g. "See all"). [trailingText]/[onTrailingClick] are both-or-neither; passing only one is a
 * no-op for the trailing slot.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    onTrailingClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title.uppercase(),
            style = WatchVaultExtraType.sectionLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (trailingText != null && onTrailingClick != null) {
            Text(
                trailingText,
                style = MaterialTheme.typography.labelMedium,
                color = LocalVaultColors.current.gold,
                modifier = Modifier.clickable(onClick = onTrailingClick)
            )
        }
    }
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

/** Small pill-shaped status/priority tag — the one place in the app a true pill shape is used,
 *  reserved for exactly this kind of short at-a-glance label (e.g. "At Target", "Grail"). */
@Composable
fun StatusChip(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    val vaultColors = LocalVaultColors.current
    val tint = color ?: vaultColors.gold
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = Spacing.xs, vertical = Spacing.xxs)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

/** Which layout a [WatchCard] renders as. GRID is a vertical card (photo over text, used in the
 *  Collection grid and Home's recently-added strip). LIST is a horizontal row (photo beside text,
 *  used in the Collection list view and Wishlist). */
enum class WatchCardVariant { GRID, LIST }

/**
 * The one card used everywhere a watch (owned or wishlist) is shown as a compact preview: a photo,
 * brand, model, a primary value figure and an optional secondary line/status tag. Collection,
 * Wishlist and Home's recently-added strip all go through this instead of each maintaining their
 * own near-duplicate card composable.
 */
@Composable
fun WatchCard(
    photo: WatchPhoto?,
    brand: String,
    model: String,
    modifier: Modifier = Modifier,
    variant: WatchCardVariant = WatchCardVariant.GRID,
    primaryValueText: String? = null,
    primaryValueColor: Color? = null,
    secondaryText: String? = null,
    secondaryColor: Color? = null,
    statusLabel: String? = null,
    onClick: () -> Unit
) {
    val vaultColors = LocalVaultColors.current
    val goldColor = vaultColors.gold

    when (variant) {
        WatchCardVariant.GRID -> Column(
            modifier = modifier
                .clip(RoundedCornerShape(Radius.card))
                .border(1.dp, vaultColors.border, RoundedCornerShape(Radius.card))
                .clickable(onClick = onClick)
        ) {
            Box {
                WatchPhotoOrPlaceholder(photo = photo, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                if (statusLabel != null) {
                    StatusChip(statusLabel, modifier = Modifier.padding(Spacing.xs))
                }
            }
            Column(modifier = Modifier.padding(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(brand.uppercase(), style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(model, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (primaryValueText != null) {
                    Text(primaryValueText, style = MaterialTheme.typography.labelMedium, color = primaryValueColor ?: goldColor)
                }
                if (secondaryText != null) {
                    Text(secondaryText, style = MaterialTheme.typography.labelSmall, color = secondaryColor ?: MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        WatchCardVariant.LIST -> Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.card))
                .border(1.dp, vaultColors.border, RoundedCornerShape(Radius.card))
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WatchPhotoOrPlaceholder(
                photo = photo,
                modifier = Modifier.size(76.dp).clip(RoundedCornerShape(topStart = Radius.card, bottomStart = Radius.card))
            )
            Column(modifier = Modifier.weight(1f).padding(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(brand.uppercase(), style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(model, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (statusLabel != null) {
                        StatusChip(statusLabel)
                    }
                }
                if (primaryValueText != null) {
                    Text(primaryValueText, style = MaterialTheme.typography.labelMedium, color = primaryValueColor ?: goldColor)
                }
                if (secondaryText != null) {
                    Text(secondaryText, style = MaterialTheme.typography.labelSmall, color = secondaryColor ?: MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * The one empty-state treatment used across screens: an icon, a headline, a short body line and
 * one primary action, with an optional quiet secondary action underneath. Home/Collection/
 * Wishlist/Activity all render their "nothing here yet" case through this rather than each
 * hand-rolling their own layout.
 */
@Composable
fun EmptyState(
    headline: String,
    body: String,
    modifier: Modifier = Modifier,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WatchSilhouettePlaceholder(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(36.dp)))
        Column(
            modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
        ) {
            Text(headline, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        if (primaryActionLabel != null && onPrimaryAction != null) {
            PrimaryButton(text = primaryActionLabel, onClick = onPrimaryAction, modifier = Modifier.fillMaxWidth())
        }
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            SecondaryButton(
                text = secondaryActionLabel,
                onClick = onSecondaryAction,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs)
            )
        }
    }
}

/** Thin, consistently-shaped wrapper around [Button] — corner radius and padding match [Radius.card]
 *  everywhere a primary action button appears, instead of each screen's default Material shape. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(Radius.card),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = modifier
    ) {
        Text(text)
    }
}

/** Thin, consistently-shaped wrapper around [OutlinedButton] — the secondary/quiet counterpart to
 *  [PrimaryButton]. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(Radius.card),
        modifier = modifier
    ) {
        Text(text)
    }
}

/** A scannable two-column specification grid — "Movement  Automatic" beside "Case  Stainless
 *  steel" on the same row — rather than one full-width label/value row per fact. Only [rows]
 *  actually passed in render; callers are expected to have already filtered out unknown fields. */
@Composable
fun WatchSpecGrid(rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        rows.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                pair.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(value, style = MaterialTheme.typography.bodyMedium)
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (pair.size == 1) {
                    Column(modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}

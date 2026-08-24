package com.watchvault.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.watchvault.data.entity.WatchPhoto
import com.watchvault.ui.theme.LocalVaultColors
import com.watchvault.ui.theme.Motion
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

/** Which layout a [WatchCard] renders as. GRID is a vertical, image-dominant card (used in the
 *  Collection grid and Home's recently-added strip). LIST is a horizontal row (used in the
 *  Collection list view and Wishlist), kept dense but still image-led. */
enum class WatchCardVariant { GRID, LIST }

/**
 * The one card used everywhere a watch (owned or wishlist) is shown as a compact preview.
 * Photography is the point: no border, no card background — the image fills its frame edge to
 * edge and the text sits directly on the canvas beneath/beside it, the way an editorial photo
 * caption reads rather than a bordered database row. Collection, Wishlist and Home's
 * recently-added strip all go through this instead of each maintaining their own card.
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(Motion.quick), label = "cardPress")

    when (variant) {
        WatchCardVariant.GRID -> Column(
            modifier = modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(Radius.card))
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
        ) {
            Box {
                WatchPhotoOrPlaceholder(
                    photo = photo,
                    modifier = Modifier.fillMaxWidth().aspectRatio(0.84f).clip(RoundedCornerShape(Radius.card))
                )
                if (statusLabel != null) {
                    Capsule(statusLabel, variant = CapsuleVariant.ACCENT, modifier = Modifier.padding(Spacing.xs))
                }
            }
            Column(modifier = Modifier.padding(top = Spacing.xs), verticalArrangement = Arrangement.spacedBy(1.dp)) {
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
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(Radius.card))
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            WatchPhotoOrPlaceholder(
                photo = photo,
                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(Radius.card))
            )
            Column(modifier = Modifier.weight(1f).padding(vertical = Spacing.xxs), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(brand.uppercase(), style = WatchVaultExtraType.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(model, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (statusLabel != null) {
                        Capsule(statusLabel, variant = CapsuleVariant.ACCENT)
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

/**
 * High-emphasis action ("Save Watch", "Add to Collection"). [loading] swaps the label for a small
 * spinner and disables the button — used for saves/network calls so the user gets a state instead
 * of a second tap landing on a dead control.
 */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(Radius.card),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = modifier
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(text)
        }
    }
}

/** Supporting action, one step down from [PrimaryButton] — an outlined control with the same
 *  shape language. */
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

/** Lowest-emphasis action — a bare text label, no container. For things like "Enter manually" or
 *  "Skip" that must never compete visually with the screen's primary/secondary buttons. */
@Composable
fun TertiaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(text)
    }
}

/** A destructive action ("Delete", "Remove Photo") — same shape as [SecondaryButton] but tinted
 *  with the app's danger color so it reads as irreversible before the user taps it. */
@Composable
fun DestructiveButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val vaultColors = LocalVaultColors.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(Radius.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, vaultColors.danger.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = vaultColors.danger),
        modifier = modifier
    ) {
        Text(text)
    }
}

/** A circular icon-only tap target (favorite, share, more, close) with a consistent 40dp touch
 *  target regardless of the icon's intrinsic size, and a subtle scrim-friendly background variant
 *  for use over photography ([onImage] = true). */
@Composable
fun IconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onImage: Boolean = false,
    tint: Color? = null
) {
    val vaultColors = LocalVaultColors.current
    val background = if (onImage) Color.Black.copy(alpha = 0.35f) else Color.Transparent
    val contentColor = tint ?: if (onImage) Color.White else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription, tint = contentColor)
        }
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

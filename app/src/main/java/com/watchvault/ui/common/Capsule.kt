package com.watchvault.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.watchvault.ui.theme.LocalVaultColors
import com.watchvault.ui.theme.Motion
import com.watchvault.ui.theme.WatchVaultExtraType

/**
 * The single small-container language for the app: metadata tags, movement/case facts, wishlist
 * priority, status, filters. A capsule is a compact fact or control, never a layout device — long
 * text, statistics or whole sections must not be wrapped in one.
 */
enum class CapsuleVariant { NEUTRAL, SUBTLE, ACCENT, SUCCESS, WARNING, DESTRUCTIVE, SELECTED, OUTLINED }

private data class CapsuleColors(val background: Color, val content: Color, val border: Color?)

@Composable
private fun colorsFor(variant: CapsuleVariant): CapsuleColors {
    val vault = LocalVaultColors.current
    val scheme = MaterialTheme.colorScheme
    return when (variant) {
        CapsuleVariant.NEUTRAL -> CapsuleColors(scheme.surfaceVariant, scheme.onSurfaceVariant, null)
        CapsuleVariant.SUBTLE -> CapsuleColors(scheme.onSurface.copy(alpha = 0.06f), scheme.onSurface.copy(alpha = 0.75f), null)
        // ACCENT is reserved for genuinely special status (a "Grail" wishlist priority) — kept
        // in the vault's warm gold rather than the interactive blue, since it marks a premium
        // distinction, not something tappable.
        CapsuleVariant.ACCENT -> CapsuleColors(vault.gold.copy(alpha = 0.14f), vault.gold, null)
        CapsuleVariant.SUCCESS -> CapsuleColors(vault.success.copy(alpha = 0.14f), vault.success, null)
        CapsuleVariant.WARNING -> CapsuleColors(vault.warning.copy(alpha = 0.16f), vault.warning, null)
        CapsuleVariant.DESTRUCTIVE -> CapsuleColors(vault.danger.copy(alpha = 0.14f), vault.danger, null)
        // SELECTED marks an active filter/sort/toggle choice — an interactive state, so it uses
        // the theme's blue accent (colorScheme.primary), never gold.
        CapsuleVariant.SELECTED -> CapsuleColors(scheme.primary, scheme.onPrimary, null)
        CapsuleVariant.OUTLINED -> CapsuleColors(Color.Transparent, scheme.onSurfaceVariant, vault.border)
    }
}

/**
 * A compact fact or control: "Automatic", "39mm", "Grail", a filter option. [onClick] turns it
 * into a toggle (filters, priority pickers) with checkbox semantics; omit it for a purely
 * informational tag (watch-card status overlays, spec facts).
 */
@Composable
fun Capsule(
    text: String,
    modifier: Modifier = Modifier,
    variant: CapsuleVariant = CapsuleVariant.SUBTLE,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = colorsFor(variant)
    val background by animateColorAsState(colors.background, tween(Motion.quick), label = "capsuleBg")
    val content by animateColorAsState(colors.content, tween(Motion.quick), label = "capsuleContent")

    var rowModifier = modifier
        .clip(CircleShape)
        .background(background)
    if (colors.border != null) {
        rowModifier = rowModifier.border(1.dp, colors.border, CircleShape)
    }
    if (onClick != null) {
        rowModifier = rowModifier
            .semantics { role = Role.Checkbox; selected = variant == CapsuleVariant.SELECTED }
            .clickable(onClick = onClick)
    }
    rowModifier = rowModifier.padding(horizontal = 12.dp, vertical = 6.dp)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        }
        Text(text, style = WatchVaultExtraType.capsule, color = content)
    }
}

package com.watchvault.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.watchvault.data.settings.SeedColor
import com.watchvault.data.settings.ThemeMode

/**
 * Extra semantic colors the stock Material3 [ColorScheme] has no slots for, but a "premium
 * personal watch vault" identity needs: a reserved gold accent for valuation/milestone figures,
 * plus success/warning/danger for gains/losses and destructive actions. Additive to the normal
 * MaterialTheme.colorScheme — read via [LocalVaultColors].
 */
data class VaultExtendedColors(
    val gold: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val border: Color,
    val surfaceElevated: Color
)

private val VaultColorsLight = VaultExtendedColors(
    gold = vault_gold,
    success = vault_success,
    warning = vault_warning,
    danger = vault_danger,
    border = vault_light_border,
    surfaceElevated = vault_light_surfaceElevated
)

private val VaultColorsDark = VaultExtendedColors(
    gold = vault_gold,
    success = vault_success,
    warning = vault_warning,
    danger = vault_danger,
    border = vault_dark_border,
    surfaceElevated = vault_dark_surfaceElevated
)

val LocalVaultColors = staticCompositionLocalOf { VaultColorsDark }

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline
)

private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline
)

private fun mix(a: Color, b: Color, fraction: Float): Color = Color(
    red = a.red + (b.red - a.red) * fraction,
    green = a.green + (b.green - a.green) * fraction,
    blue = a.blue + (b.blue - a.blue) * fraction,
    alpha = 1f
)

/**
 * Builds a full Material 3 ColorScheme from a single seed color when dynamic color is
 * unavailable or disabled. This is a lightweight, dependency-free tonal approximation
 * (blend-toward-white/black) rather than the full Material color-utilities palette
 * generator, but it produces a coherent, accessible light/dark pair for each swatch.
 */
private fun schemeFromSeed(seed: Color, darkTheme: Boolean, isVault: Boolean): ColorScheme {
    val white = Color.White
    val black = Color.Black
    return if (!darkTheme) {
        lightColorScheme(
            primary = seed,
            onPrimary = white,
            primaryContainer = mix(seed, white, 0.8f),
            onPrimaryContainer = mix(seed, black, 0.65f),
            secondary = mix(seed, black, 0.15f),
            onSecondary = white,
            secondaryContainer = mix(seed, white, 0.85f),
            onSecondaryContainer = mix(seed, black, 0.6f),
            tertiary = if (isVault) vault_gold else mix(seed, black, 0.3f),
            onTertiary = white,
            tertiaryContainer = if (isVault) mix(vault_gold, white, 0.75f) else mix(seed, white, 0.75f),
            onTertiaryContainer = if (isVault) mix(vault_gold, black, 0.55f) else mix(seed, black, 0.55f),
            background = if (isVault) vault_light_background else md_theme_light_background,
            onBackground = if (isVault) vault_light_onBackground else md_theme_light_onBackground,
            surface = if (isVault) vault_light_surface else md_theme_light_surface,
            onSurface = if (isVault) vault_light_onBackground else md_theme_light_onSurface,
            surfaceVariant = if (isVault) vault_light_surfaceElevated else md_theme_light_surfaceVariant,
            onSurfaceVariant = if (isVault) vault_light_onSurfaceVariant else md_theme_light_onSurfaceVariant,
            outline = if (isVault) vault_light_border else md_theme_light_outline,
            error = vault_danger,
            onError = white
        )
    } else {
        darkColorScheme(
            primary = if (isVault) vault_primary else mix(seed, white, 0.45f),
            onPrimary = white,
            primaryContainer = if (isVault) mix(vault_primary, black, 0.35f) else mix(seed, black, 0.35f),
            onPrimaryContainer = if (isVault) vault_dark_onBackground else mix(seed, white, 0.85f),
            secondary = if (isVault) vault_dark_onSurfaceVariant else mix(seed, white, 0.35f),
            onSecondary = black,
            secondaryContainer = if (isVault) vault_dark_surfaceElevated else mix(seed, black, 0.45f),
            onSecondaryContainer = if (isVault) vault_dark_onBackground else mix(seed, white, 0.8f),
            tertiary = vault_gold,
            onTertiary = black,
            tertiaryContainer = if (isVault) mix(vault_gold, black, 0.55f) else mix(seed, black, 0.4f),
            onTertiaryContainer = if (isVault) vault_gold else mix(seed, white, 0.85f),
            background = if (isVault) vault_dark_background else md_theme_dark_background,
            onBackground = if (isVault) vault_dark_onBackground else md_theme_dark_onBackground,
            surface = if (isVault) vault_dark_surface else md_theme_dark_surface,
            onSurface = if (isVault) vault_dark_onBackground else md_theme_dark_onSurface,
            surfaceVariant = if (isVault) vault_dark_surfaceElevated else md_theme_dark_surfaceVariant,
            onSurfaceVariant = if (isVault) vault_dark_onSurfaceVariant else md_theme_dark_onSurfaceVariant,
            outline = if (isVault) vault_dark_border else md_theme_dark_outline,
            error = vault_danger,
            onError = black
        )
    }
}

/**
 * Top-level app theme. Recomputes the ColorScheme from persisted [ThemeMode]/dynamic-color/
 * seed-color state, so flipping a setting in Settings restyles the whole app immediately —
 * this composable is expected to be re-invoked whenever the caller's `collectAsState()` on
 * the DataStore-backed settings flow emits.
 */
@Composable
fun WatchVaultTheme(
    themeMode: ThemeMode,
    useDynamicColor: Boolean,
    seedColor: SeedColor,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val amoled = themeMode == ThemeMode.AMOLED

    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalView.current.context

    val baseColorScheme = when {
        useDynamicColor && dynamicSupported ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> schemeFromSeed(Color(seedColor.argb), darkTheme, isVault = seedColor == SeedColor.VAULT)
    }

    // AMOLED mode swaps in a true black background/surface on top of whichever scheme was
    // otherwise selected, so it composes with dynamic color and every seed swatch, not just Vault.
    val colorScheme = if (amoled && darkTheme) {
        baseColorScheme.copy(
            background = vault_amoled_background,
            surface = vault_amoled_background
        )
    } else {
        baseColorScheme
    }

    val extendedColors = if (darkTheme) VaultColorsDark else VaultColorsLight

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalVaultColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WatchVaultTypography,
            shapes = WatchVaultShapes,
            content = content
        )
    }
}

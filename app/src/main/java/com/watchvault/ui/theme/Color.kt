package com.watchvault.ui.theme

import androidx.compose.ui.graphics.Color

// Static fallback palette used pre-Android-12 or when dynamic color is toggled off.
// Generated as simple light/dark tonal pairs per Material 3 seed-color convention.

val md_theme_light_primary = Color(0xFF3B6EF6)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFDCE1FF)
val md_theme_light_onPrimaryContainer = Color(0xFF00174B)
val md_theme_light_secondary = Color(0xFF5B5D72)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFDFE1F9)
val md_theme_light_onSecondaryContainer = Color(0xFF181A2C)
val md_theme_light_tertiary = Color(0xFF77536D)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFFFD7EE)
val md_theme_light_onTertiaryContainer = Color(0xFF2D1229)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = Color(0xFFFEFBFF)
val md_theme_light_onBackground = Color(0xFF1B1B1F)
val md_theme_light_surface = Color(0xFFFEFBFF)
val md_theme_light_onSurface = Color(0xFF1B1B1F)
val md_theme_light_surfaceVariant = Color(0xFFE3E1EC)
val md_theme_light_onSurfaceVariant = Color(0xFF46464F)
val md_theme_light_outline = Color(0xFF767680)

val md_theme_dark_primary = Color(0xFFB7C4FF)
val md_theme_dark_onPrimary = Color(0xFF00297A)
val md_theme_dark_primaryContainer = Color(0xFF1E4AC9)
val md_theme_dark_onPrimaryContainer = Color(0xFFDCE1FF)
val md_theme_dark_secondary = Color(0xFFC4C5DD)
val md_theme_dark_onSecondary = Color(0xFF2D2F42)
val md_theme_dark_secondaryContainer = Color(0xFF434559)
val md_theme_dark_onSecondaryContainer = Color(0xFFDFE1F9)
val md_theme_dark_tertiary = Color(0xFFE6B9D6)
val md_theme_dark_onTertiary = Color(0xFF45263E)
val md_theme_dark_tertiaryContainer = Color(0xFF5E3C55)
val md_theme_dark_onTertiaryContainer = Color(0xFFFFD7EE)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = Color(0xFF1B1B1F)
val md_theme_dark_onBackground = Color(0xFFE4E1E6)
val md_theme_dark_surface = Color(0xFF1B1B1F)
val md_theme_dark_onSurface = Color(0xFFE4E1E6)
val md_theme_dark_surfaceVariant = Color(0xFF46464F)
val md_theme_dark_onSurfaceVariant = Color(0xFFC7C5D0)
val md_theme_dark_outline = Color(0xFF90909A)

// --- "Vault" premium palette (Phase A theme-v2) ---
// Restrained, dark, jewel/metal-toned identity for the app: an AMOLED-friendly near-black
// dark base with a cool functional-blue accent and a warm gold accent reserved for valuation
// and milestone moments. Offered as a named seed-color option (SeedColor.VAULT) and used as
// the default swatch for anyone who has never explicitly picked one.

val vault_dark_background = Color(0xFF0E0D0C)
val vault_dark_surface = Color(0xFF1B1917)
val vault_dark_surfaceElevated = Color(0xFF24211E)
val vault_dark_border = Color(0xFF3A3632)
val vault_dark_onBackground = Color(0xFFF5F2ED)
val vault_dark_onSurfaceVariant = Color(0xFFB5B0AA)
val vault_dark_muted = Color(0xFF817B74)

// True AMOLED variant: pure black background, otherwise identical to the dark base.
val vault_amoled_background = Color(0xFF000000)

// Light-mode counterpart: same relationships (near-white base, dark text) at adjusted tone
// so Light/Dark/System/AMOLED all stay coherent with the same identity.
val vault_light_background = Color(0xFFFAF8F4)
val vault_light_surface = Color(0xFFFFFFFF)
val vault_light_surfaceElevated = Color(0xFFF1ECE3)
val vault_light_border = Color(0xFFD9D2C6)
val vault_light_onBackground = Color(0xFF211E1A)
val vault_light_onSurfaceVariant = Color(0xFF5B564D)
val vault_light_muted = Color(0xFF8B8579)

// Functional accents — shared between light/dark; only their container/on-colors adapt.
val vault_primary = Color(0xFF3F86F5) // "Blue" functional accent
val vault_gold = Color(0xFFC6A15B) // "Warm Gold" — reserved for value figures, selection, premium badges
val vault_gold_light = Color(0xFFD8BD7A) // "Light Gold" — highlights/gradient partner for Warm Gold
val vault_success = Color(0xFF62B76A)
val vault_warning = Color(0xFFD8A84E)
val vault_danger = Color(0xFFD66B6B)

package com.watchvault.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_preferences")

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** Pixel-style seed color swatches offered when dynamic color is off or unavailable. */
enum class SeedColor(val label: String, val argb: Long) {
    BLUE("Blue", 0xFF3B6EF6),
    GREEN("Green", 0xFF1E9E5A),
    PURPLE("Purple", 0xFF7C4DFF),
    ORANGE("Orange", 0xFFF2892E),
    RED("Red", 0xFFE3483B),
    TEAL("Teal", 0xFF14A6A0),
    PINK("Pink", 0xFFE0559C),
    SLATE("Slate", 0xFF5B6B79)
}

data class ThemeSettings(
    val useDynamicColor: Boolean = true,
    val seedColor: SeedColor = SeedColor.BLUE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

/** Persists appearance choices so they survive process death / device restart. */
class ThemePreferencesRepository(private val context: Context) {

    private object Keys {
        val DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val SEED_COLOR = stringPreferencesKey("seed_color")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val themeSettings: Flow<ThemeSettings> = context.themeDataStore.data.map { prefs ->
        ThemeSettings(
            useDynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            seedColor = prefs[Keys.SEED_COLOR]?.let { name ->
                runCatching { SeedColor.valueOf(name) }.getOrDefault(SeedColor.BLUE)
            } ?: SeedColor.BLUE,
            themeMode = prefs[Keys.THEME_MODE]?.let { name ->
                runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
            } ?: ThemeMode.SYSTEM
        )
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        context.themeDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setSeedColor(seedColor: SeedColor) {
        context.themeDataStore.edit { it[Keys.SEED_COLOR] = seedColor.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }
}

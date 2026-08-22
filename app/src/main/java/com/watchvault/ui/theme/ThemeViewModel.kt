package com.watchvault.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchvault.data.settings.SeedColor
import com.watchvault.data.settings.ThemeMode
import com.watchvault.data.settings.ThemePreferencesRepository
import com.watchvault.data.settings.ThemeSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(private val repository: ThemePreferencesRepository) : ViewModel() {

    val themeSettings: StateFlow<ThemeSettings> = repository.themeSettings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeSettings()
    )

    fun setUseDynamicColor(enabled: Boolean) = viewModelScope.launch { repository.setUseDynamicColor(enabled) }
    fun setSeedColor(seedColor: SeedColor) = viewModelScope.launch { repository.setSeedColor(seedColor) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }
}

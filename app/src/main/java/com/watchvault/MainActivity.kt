package com.watchvault

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchvault.di.GenericViewModelFactory
import com.watchvault.di.LocalAppContainer
import com.watchvault.ui.navigation.WatchVaultApp
import com.watchvault.ui.theme.ThemeViewModel
import com.watchvault.ui.theme.WatchVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestHighestRefreshRate()

        val container = (application as WatchVaultApplication).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                val themeViewModel: ThemeViewModel = viewModel(
                    factory = GenericViewModelFactory { ThemeViewModel(container.themePreferencesRepository) }
                )
                val themeSettings by themeViewModel.themeSettings.collectAsState()

                WatchVaultTheme(
                    themeMode = themeSettings.themeMode,
                    useDynamicColor = themeSettings.useDynamicColor,
                    seedColor = themeSettings.seedColor
                ) {
                    WatchVaultApp()
                }
            }
        }
    }

    /** Opts into the display's highest refresh rate (90/120Hz panels) instead of the OS default
     *  of 60Hz for apps that don't ask. Safe no-op on 60Hz-only hardware. */
    private fun requestHighestRefreshRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else windowManager.defaultDisplay
        val bestMode = display?.supportedModes?.maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply { preferredDisplayModeId = bestMode.modeId }
    }
}

package com.watchvault.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watchvault.data.settings.SeedColor
import com.watchvault.data.settings.ThemeMode
import com.watchvault.di.GenericViewModelFactory
import com.watchvault.di.LocalAppContainer
import com.watchvault.ui.theme.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenImportExport: () -> Unit) {
    val container = LocalAppContainer.current
    val themeViewModel: ThemeViewModel = viewModel(
        factory = GenericViewModelFactory { ThemeViewModel(container.themePreferencesRepository) }
    )
    val settings by themeViewModel.themeSettings.collectAsState()
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Dynamic color (Material You)")
                    if (!dynamicSupported) Text("Requires Android 12+", style = MaterialTheme.typography.labelSmall)
                }
                Switch(
                    checked = settings.useDynamicColor && dynamicSupported,
                    enabled = dynamicSupported,
                    onCheckedChange = themeViewModel::setUseDynamicColor
                )
            }

            if (!settings.useDynamicColor || !dynamicSupported) {
                Text("Seed color")
                SeedColorGrid(selected = settings.seedColor, onSelect = themeViewModel::setSeedColor)
            }

            HorizontalDivider()
            Text("Theme mode", style = MaterialTheme.typography.titleMedium)
            ThemeMode.values().forEach { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { themeViewModel.setThemeMode(mode) },
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    RadioButton(selected = settings.themeMode == mode, onClick = { themeViewModel.setThemeMode(mode) })
                    Text(
                        when (mode) {
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                            ThemeMode.AMOLED -> "AMOLED (pure black)"
                            ThemeMode.SYSTEM -> "System"
                        }
                    )
                }
            }

            HorizontalDivider()
            Text("Import / Export", style = MaterialTheme.typography.titleMedium)
            Text(
                "Backups, MyInnos migration, and restore live on their own screen.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable(onClick = onOpenImportExport)
            )

            HorizontalDivider()
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text("WatchVault 1.0.0 — an offline-first, personal wristwatch collection and wishlist app.")
            Text("No account, no cloud sync, no ads. Your data stays on this device unless you export it.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SeedColorGrid(selected: SeedColor, onSelect: (SeedColor) -> Unit) {
    val rows = SeedColor.values().toList().chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { seedColor ->
                    val isSelected = seedColor == selected
                    Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onSelect(seedColor) }
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(seedColor.argb))
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                        )
                        Text(seedColor.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

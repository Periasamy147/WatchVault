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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.watchvault.ui.common.SectionHeader
import com.watchvault.ui.theme.Spacing
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.screenH),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                SectionHeader("Appearance")

                SettingsRow(
                    label = "Dynamic color (Material You)",
                    value = if (!dynamicSupported) "Requires Android 12+" else null,
                    trailing = {
                        Switch(
                            checked = settings.useDynamicColor && dynamicSupported,
                            enabled = dynamicSupported,
                            onCheckedChange = themeViewModel::setUseDynamicColor
                        )
                    }
                )

                if (!settings.useDynamicColor || !dynamicSupported) {
                    SettingsRow(label = "Seed color")
                    SeedColorGrid(selected = settings.seedColor, onSelect = themeViewModel::setSeedColor)
                }

                ThemeMode.values().forEach { mode ->
                    SettingsRow(
                        label = when (mode) {
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                            ThemeMode.AMOLED -> "AMOLED (pure black)"
                            ThemeMode.SYSTEM -> "System"
                        },
                        onClick = { themeViewModel.setThemeMode(mode) },
                        trailing = {
                            RadioButton(selected = settings.themeMode == mode, onClick = { themeViewModel.setThemeMode(mode) })
                        }
                    )
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                SectionHeader("Data")
                SettingsRow(
                    label = "Import, Export, Backup",
                    value = "MyInnos migration, full backup export, and restore",
                    onClick = onOpenImportExport,
                    showChevron = true
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                SectionHeader("About")
                SettingsRow(label = "WatchVault", value = "1.0.0")
                Text(
                    "An offline-first, personal wristwatch collection and wishlist app. No account, no cloud sync, no ads. Your data stays on this device unless you export it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.xs)
                )
            }
        }
    }
}

/**
 * The one settings-row treatment used across every group (Appearance/Data/About): a label,
 * optional secondary value line, and either a supplied trailing control or a chevron — consistent
 * height/padding regardless of which group it's in.
 */
@Composable
private fun SettingsRow(
    label: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (value != null) {
                Text(value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when {
            trailing != null -> trailing()
            showChevron -> Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

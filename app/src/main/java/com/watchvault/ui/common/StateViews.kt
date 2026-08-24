package com.watchvault.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.watchvault.ui.theme.LocalVaultColors
import com.watchvault.ui.theme.Spacing

/**
 * Full-screen recoverable-failure state — the inline counterpart to [ErrorSheet] (which reacts to
 * a single action like a URL fetch). Used when a whole screen has nothing to show because a load
 * failed, never showing the underlying exception message.
 */
@Composable
fun InlineErrorState(
    modifier: Modifier = Modifier,
    headline: String = "Something went wrong.",
    body: String = "Your data is safe on this device. Try again.",
    onRetry: (() -> Unit)? = null
) {
    val vaultColors = LocalVaultColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = vaultColors.danger)
        Column(
            modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
        ) {
            Text(headline, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        if (onRetry != null) {
            SecondaryButton(text = "Retry", onClick = onRetry)
        }
    }
}

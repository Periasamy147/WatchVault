package com.watchvault.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.watchvault.ui.theme.LocalVaultColors
import com.watchvault.ui.theme.Spacing

/**
 * The one way a user-facing failure is ever shown: a human headline, a plain-language body
 * sentence and up to two actions — never a raw exception message, stack trace or HTTP status
 * number. Callers classify the failure into that human copy themselves (see
 * `UrlFetchFailureCategory` in WishAddEditViewModel for an example) and keep any technical detail
 * out of what's passed here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorSheet(
    headline: String,
    body: String,
    onDismiss: () -> Unit,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    val vaultColors = LocalVaultColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = vaultColors.danger,
                modifier = Modifier.padding(bottom = Spacing.xxs)
            )
            Text(headline, style = MaterialTheme.typography.titleLarge)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (primaryActionLabel != null && onPrimaryAction != null) {
                PrimaryButton(text = primaryActionLabel, onClick = onPrimaryAction, modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm))
            }
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                SecondaryButton(text = secondaryActionLabel, onClick = onSecondaryAction, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

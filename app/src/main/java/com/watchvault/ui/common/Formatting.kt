package com.watchvault.ui.common

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatMoney(amount: Double?, currency: String?, assumed: Boolean = false): String {
    if (amount == null) return "—"
    val formatted = NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 2 }.format(amount)
    val currencyLabel = currency ?: "?"
    val suffix = if (assumed) " (assumed)" else ""
    return "$currencyLabel $formatted$suffix"
}

fun formatDate(epochMillis: Long?): String {
    if (epochMillis == null) return "—"
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMillis))
}

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

/** Signed percentage, e.g. "+12.4%" or "-3.1%". Returns "—" when there's nothing to divide by. */
fun formatPercent(value: Double?): String {
    if (value == null) return "—"
    val sign = if (value > 0) "+" else ""
    val formatted = NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 1 }.format(value)
    return "$sign$formatted%"
}

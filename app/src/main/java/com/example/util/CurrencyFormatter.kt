package com.example.util

import com.example.model.Currency
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {

    fun formatAmount(
        amount: Double,
        currency: Currency,
        includeSymbol: Boolean = false
    ): String {
        val decimals = currency.decimalDigits
        val pattern = if (decimals == 0) {
            "#,##0"
        } else {
            "#,##0." + "0".repeat(decimals)
        }

        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat(pattern, symbols)
        val formattedNumber = df.format(amount)

        return if (includeSymbol) {
            "${currency.symbol}$formattedNumber"
        } else {
            formattedNumber
        }
    }

    fun formatRate(rate: Double): String {
        return if (rate >= 100.0) {
            String.format(Locale.US, "%.2f", rate)
        } else if (rate >= 1.0) {
            String.format(Locale.US, "%.4f", rate)
        } else if (rate >= 0.001) {
            String.format(Locale.US, "%.5f", rate)
        } else {
            String.format(Locale.US, "%.6f", rate)
        }
    }

    fun formatTimestamp(epochMs: Long): String {
        val now = System.currentTimeMillis()
        val diffMs = now - epochMs

        return when {
            diffMs < 60_000 -> "Just now"
            diffMs < 3600_000 -> "${diffMs / 60_000}m ago"
            diffMs < 86400_000 -> {
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                "Today at ${sdf.format(Date(epochMs))}"
            }
            diffMs < 172800_000 -> {
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                "Yesterday at ${sdf.format(Date(epochMs))}"
            }
            else -> {
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                sdf.format(Date(epochMs))
            }
        }
    }
}

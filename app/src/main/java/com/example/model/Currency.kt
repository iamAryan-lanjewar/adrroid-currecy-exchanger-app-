package com.example.model

data class Currency(
    val code: String,
    val name: String,
    val symbol: String,
    val flagEmoji: String,
    val decimalDigits: Int = 2
) {
    fun matchesQuery(query: String): Boolean {
        val trimmed = query.trim().lowercase()
        return code.lowercase().contains(trimmed) ||
                name.lowercase().contains(trimmed) ||
                symbol.lowercase().contains(trimmed)
    }

    companion object {
        val ALL: List<Currency> = listOf(
            Currency("USD", "US Dollar", "$", "🇺🇸", 2),
            Currency("EUR", "Euro", "€", "🇪🇺", 2),
            Currency("JPY", "Japanese Yen", "¥", "🇯🇵", 0),
            Currency("GBP", "British Pound", "£", "🇬🇧", 2),
            Currency("CAD", "Canadian Dollar", "CA$", "🇨🇦", 2),
            Currency("AUD", "Australian Dollar", "A$", "🇦🇺", 2),
            Currency("CHF", "Swiss Franc", "CHF", "🇨🇭", 2),
            Currency("CNY", "Chinese Yuan", "¥", "🇨🇳", 2),
            Currency("INR", "Indian Rupee", "₹", "🇮🇳", 2),
            Currency("MXN", "Mexican Peso", "Mex$", "🇲🇽", 2),
            Currency("SGD", "Singapore Dollar", "S$", "🇸🇬", 2),
            Currency("THB", "Thai Baht", "฿", "🇹🇭", 2),
            Currency("KRW", "South Korean Won", "₩", "🇰🇷", 0),
            Currency("BRL", "Brazilian Real", "R$", "🇧🇷", 2),
            Currency("NZD", "New Zealand Dollar", "NZ$", "🇳🇿", 2),
            Currency("AED", "UAE Dirham", "AED", "🇦🇪", 2),
            Currency("TRY", "Turkish Lira", "₺", "🇹🇷", 2),
            Currency("VND", "Vietnamese Dong", "₫", "🇻🇳", 0),
            Currency("IDR", "Indonesian Rupiah", "Rp", "🇮🇩", 0),
            Currency("PHP", "Philippine Peso", "₱", "🇵🇭", 2),
            Currency("MYR", "Malaysian Ringgit", "RM", "🇲🇾", 2),
            Currency("TWD", "New Taiwan Dollar", "NT$", "🇹🇼", 2),
            Currency("SEK", "Swedish Krona", "kr", "🇸🇪", 2),
            Currency("NOK", "Norwegian Krone", "kr", "🇳🇴", 2),
            Currency("DKK", "Danish Krone", "kr", "🇩🇰", 2),
            Currency("PLN", "Polish Zloty", "zł", "🇵🇱", 2),
            Currency("ZAR", "South African Rand", "R", "🇿🇦", 2),
            Currency("EGP", "Egyptian Pound", "E£", "🇪🇬", 2),
            Currency("CZK", "Czech Koruna", "Kč", "🇨🇿", 2),
            Currency("HUF", "Hungarian Forint", "Ft", "🇭🇺", 0),
            Currency("ILS", "Israeli Shekel", "₪", "🇮🇱", 2),
            Currency("CLP", "Chilean Peso", "CLP$", "🇨🇱", 0),
            Currency("COP", "Colombian Peso", "COL$", "🇨🇴", 0),
            Currency("SAR", "Saudi Riyal", "SAR", "🇸🇦", 2),
            Currency("HKD", "Hong Kong Dollar", "HK$", "🇭🇰", 2),
            Currency("MAD", "Moroccan Dirham", "MAD", "🇲🇦", 2),
            Currency("PEN", "Peruvian Sol", "S/.", "🇵🇪", 2),
            Currency("ISK", "Icelandic Króna", "kr", "🇮🇸", 0),
            Currency("QAR", "Qatari Riyal", "QR", "🇶🇦", 2),
            Currency("KWD", "Kuwaiti Dinar", "KD", "🇰🇼", 3)
        )

        val DEFAULT_FAVORITES = listOf("EUR", "JPY", "GBP", "THB", "CAD", "AUD", "MXN", "CHF")

        fun findByCode(code: String): Currency {
            return ALL.find { it.code.equals(code, ignoreCase = true) }
                ?: Currency(code.uppercase(), code.uppercase(), code.uppercase(), "🌐", 2)
        }
    }
}

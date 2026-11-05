package com.example.model

enum class RateSource {
    LIVE_API,
    CACHED_LOCAL,
    OFFLINE_BUNDLE,
    CUSTOM_OVERRIDE
}

enum class SyncStatus {
    LIVE,       // Synced recently (<24h)
    CACHED,     // Synced offline cache (>24h or currently offline)
    SYNCING,    // In-flight refresh
    ERROR       // Network error occurred, using cache
}

data class ExchangeRates(
    val baseCode: String = "USD",
    val rates: Map<String, Double> = BASELINE_RATES,
    val lastUpdatedEpochMs: Long = System.currentTimeMillis(),
    val source: RateSource = RateSource.OFFLINE_BUNDLE
) {
    fun getRate(fromCode: String, toCode: String): Double {
        if (fromCode.equals(toCode, ignoreCase = true)) return 1.0

        val fromToUSD = if (fromCode.equals(baseCode, ignoreCase = true)) {
            1.0
        } else {
            rates[fromCode.uppercase()]?.let { 1.0 / it } ?: 1.0
        }

        val usdToTarget = if (toCode.equals(baseCode, ignoreCase = true)) {
            1.0
        } else {
            rates[toCode.uppercase()] ?: 1.0
        }

        return fromToUSD * usdToTarget
    }

    companion object {
        // High-fidelity baseline rates against USD for 100% offline out-of-the-box readiness
        val BASELINE_RATES: Map<String, Double> = mapOf(
            "USD" to 1.0,
            "EUR" to 0.924,
            "JPY" to 153.42,
            "GBP" to 0.789,
            "CAD" to 1.385,
            "AUD" to 1.522,
            "CHF" to 0.882,
            "CNY" to 7.228,
            "INR" to 84.18,
            "MXN" to 18.78,
            "SGD" to 1.342,
            "THB" to 36.45,
            "KRW" to 1385.0,
            "BRL" to 5.46,
            "NZD" to 1.662,
            "AED" to 3.673,
            "TRY" to 34.25,
            "VND" to 25420.0,
            "IDR" to 15750.0,
            "PHP" to 58.20,
            "MYR" to 4.41,
            "TWD" to 32.15,
            "SEK" to 10.65,
            "NOK" to 10.95,
            "DKK" to 6.89,
            "PLN" to 4.02,
            "ZAR" to 18.15,
            "EGP" to 49.10,
            "CZK" to 23.35,
            "HUF" to 376.5,
            "ILS" to 3.73,
            "CLP" to 955.0,
            "COP" to 4250.0,
            "SAR" to 3.75,
            "HKD" to 7.78,
            "MAD" to 9.85,
            "PEN" to 3.77,
            "ISK" to 138.5,
            "QAR" to 3.64,
            "KWD" to 0.307
        )
    }
}

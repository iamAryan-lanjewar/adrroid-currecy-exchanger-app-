package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Currency
import com.example.model.FeeConfig
import org.json.JSONObject

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("roamrate_prefs", Context.MODE_PRIVATE)

    fun getBaseCurrency(): String =
        prefs.getString(KEY_BASE_CURRENCY, "USD") ?: "USD"

    fun setBaseCurrency(code: String) {
        prefs.edit().putString(KEY_BASE_CURRENCY, code.uppercase()).apply()
    }

    fun getTargetCurrency(): String =
        prefs.getString(KEY_TARGET_CURRENCY, "EUR") ?: "EUR"

    fun setTargetCurrency(code: String) {
        prefs.edit().putString(KEY_TARGET_CURRENCY, code.uppercase()).apply()
    }

    fun getFavorites(): List<String> {
        val raw = prefs.getString(KEY_FAVORITES, null)
        if (raw.isNullOrBlank()) {
            return Currency.DEFAULT_FAVORITES
        }
        return raw.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
    }

    fun saveFavorites(list: List<String>) {
        val joined = list.distinct().joinToString(",")
        prefs.edit().putString(KEY_FAVORITES, joined).apply()
    }

    fun addFavorite(code: String): List<String> {
        val current = getFavorites().toMutableList()
        val upper = code.uppercase()
        if (!current.contains(upper)) {
            current.add(upper)
            saveFavorites(current)
        }
        return current
    }

    fun removeFavorite(code: String): List<String> {
        val current = getFavorites().toMutableList()
        val upper = code.uppercase()
        current.remove(upper)
        saveFavorites(current)
        return current
    }

    fun getFeeConfig(): FeeConfig {
        val percent = prefs.getFloat(KEY_FEE_PERCENT, 2.5f).toDouble()
        val enabled = prefs.getBoolean(KEY_FEE_ENABLED, false)
        val name = prefs.getString(KEY_FEE_PRESET_NAME, "Typical Bank (2.5%)") ?: "Typical Bank (2.5%)"
        return FeeConfig(markupPercent = percent, enabled = enabled, presetName = name)
    }

    fun saveFeeConfig(config: FeeConfig) {
        prefs.edit()
            .putFloat(KEY_FEE_PERCENT, config.markupPercent.toFloat())
            .putBoolean(KEY_FEE_ENABLED, config.enabled)
            .putString(KEY_FEE_PRESET_NAME, config.presetName)
            .apply()
    }

    fun getCachedRates(): Pair<Map<String, Double>, Long>? {
        val rawJson = prefs.getString(KEY_CACHED_RATES_JSON, null) ?: return null
        val timestamp = prefs.getLong(KEY_CACHED_RATES_TIME, 0L)
        return try {
            val json = JSONObject(rawJson)
            val map = mutableMapOf<String, Double>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.getDouble(key)
            }
            if (map.isNotEmpty()) Pair(map, timestamp) else null
        } catch (_: Exception) {
            null
        }
    }

    fun saveCachedRates(rates: Map<String, Double>, timestamp: Long) {
        try {
            val json = JSONObject()
            for ((key, value) in rates) {
                json.put(key, value)
            }
            prefs.edit()
                .putString(KEY_CACHED_RATES_JSON, json.toString())
                .putLong(KEY_CACHED_RATES_TIME, timestamp)
                .apply()
        } catch (_: Exception) {
            // ignore
        }
    }

    fun getCustomRate(pairKey: String): Double? {
        val value = prefs.getFloat("custom_rate_$pairKey", -1f)
        return if (value > 0f) value.toDouble() else null
    }

    fun setCustomRate(pairKey: String, rate: Double?) {
        if (rate == null || rate <= 0.0) {
            prefs.edit().remove("custom_rate_$pairKey").apply()
        } else {
            prefs.edit().putFloat("custom_rate_$pairKey", rate.toFloat()).apply()
        }
    }

    companion object {
        private const val KEY_BASE_CURRENCY = "key_base_currency"
        private const val KEY_TARGET_CURRENCY = "key_target_currency"
        private const val KEY_FAVORITES = "key_favorites"
        private const val KEY_FEE_PERCENT = "key_fee_percent"
        private const val KEY_FEE_ENABLED = "key_fee_enabled"
        private const val KEY_FEE_PRESET_NAME = "key_fee_preset_name"
        private const val KEY_CACHED_RATES_JSON = "key_cached_rates_json"
        private const val KEY_CACHED_RATES_TIME = "key_cached_rates_time"
    }
}

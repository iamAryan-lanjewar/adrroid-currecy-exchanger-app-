package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.model.ExchangeRates
import com.example.model.RateSource
import com.example.model.SyncStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class RatesRepository(
    private val context: Context,
    val preferencesManager: PreferencesManager = PreferencesManager(context)
) {
    private val _rates = MutableStateFlow(loadInitialRates())
    val rates: StateFlow<ExchangeRates> = _rates.asStateFlow()

    private val _syncStatus = MutableStateFlow(determineInitialSyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val ratesApi: RatesApi by lazy {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://open.er-api.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RatesApi::class.java)
    }

    private fun loadInitialRates(): ExchangeRates {
        val cached = preferencesManager.getCachedRates()
        return if (cached != null && cached.first.isNotEmpty()) {
            ExchangeRates(
                baseCode = "USD",
                rates = cached.first,
                lastUpdatedEpochMs = cached.second,
                source = RateSource.CACHED_LOCAL
            )
        } else {
            ExchangeRates(
                baseCode = "USD",
                rates = ExchangeRates.BASELINE_RATES,
                lastUpdatedEpochMs = System.currentTimeMillis() - (3600_000 * 4), // 4 hours ago default
                source = RateSource.OFFLINE_BUNDLE
            )
        }
    }

    private fun determineInitialSyncStatus(): SyncStatus {
        val cached = preferencesManager.getCachedRates() ?: return SyncStatus.CACHED
        val ageMs = System.currentTimeMillis() - cached.second
        return if (ageMs < 24 * 3600_000) {
            SyncStatus.LIVE
        } else {
            SyncStatus.CACHED
        }
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun refreshRates(): Boolean = withContext(Dispatchers.IO) {
        _syncStatus.value = SyncStatus.SYNCING
        try {
            val response = ratesApi.getLatestRates("USD")
            val newRates = response.rates
            if (response.result.equals("success", ignoreCase = true) && !newRates.isNullOrEmpty()) {
                val timestamp = if ((response.timeLastUpdateUnix ?: 0L) > 0) {
                    response.timeLastUpdateUnix!! * 1000L
                } else {
                    System.currentTimeMillis()
                }

                preferencesManager.saveCachedRates(newRates, timestamp)
                _rates.value = ExchangeRates(
                    baseCode = "USD",
                    rates = newRates,
                    lastUpdatedEpochMs = timestamp,
                    source = RateSource.LIVE_API
                )
                _syncStatus.value = SyncStatus.LIVE
                true
            } else {
                _syncStatus.value = SyncStatus.CACHED
                false
            }
        } catch (_: Exception) {
            _syncStatus.value = SyncStatus.CACHED
            false
        }
    }
}

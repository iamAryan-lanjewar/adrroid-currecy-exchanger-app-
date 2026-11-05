package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RatesRepository
import com.example.model.Currency
import com.example.model.ExchangeRates
import com.example.model.FeeConfig
import com.example.model.RateSource
import com.example.model.SyncStatus
import com.example.util.CurrencyFormatter
import com.example.util.MathExpressionEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CurrencyPickerMode {
    SELECT_BASE,
    SELECT_TARGET,
    ADD_TO_WATCHLIST
}

data class ConverterUiState(
    val inputExpression: String = "100",
    val evaluatedInputAmount: Double = 100.0,
    val baseCurrency: Currency = Currency.findByCode("USD"),
    val targetCurrency: Currency = Currency.findByCode("EUR"),
    val watchlistCurrencies: List<Currency> = emptyList(),
    val exchangeRates: ExchangeRates = ExchangeRates(),
    val syncStatus: SyncStatus = SyncStatus.CACHED,
    val feeConfig: FeeConfig = FeeConfig(2.5, false, "Typical Bank (2.5%)"),
    val customRateOverride: Double? = null,
    val isCustomRateActive: Boolean = false,
    val effectiveRate: Double = 0.924,
    val convertedAmount: Double = 92.40,
    val feeAmount: Double = 0.0,
    val totalWithFee: Double = 92.40,
    val lastUpdatedText: String = "Offline cached",
    val isCurrencyPickerOpen: Boolean = false,
    val pickerMode: CurrencyPickerMode = CurrencyPickerMode.SELECT_BASE,
    val isFeeSheetOpen: Boolean = false,
    val isCustomRateDialogOpen: Boolean = false,
    val isSplitSelectorOpen: Boolean = false,
    val toastMessage: String? = null
)

class ConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RatesRepository(application)
    private val prefs = repository.preferencesManager

    private val _inputExpression = MutableStateFlow("100")
    private val _baseCurrency = MutableStateFlow(Currency.findByCode(prefs.getBaseCurrency()))
    private val _targetCurrency = MutableStateFlow(Currency.findByCode(prefs.getTargetCurrency()))
    private val _favoriteCodes = MutableStateFlow(prefs.getFavorites())
    private val _feeConfig = MutableStateFlow(prefs.getFeeConfig())
    private val _isCurrencyPickerOpen = MutableStateFlow(false)
    private val _pickerMode = MutableStateFlow(CurrencyPickerMode.SELECT_BASE)
    private val _isFeeSheetOpen = MutableStateFlow(false)
    private val _isCustomRateDialogOpen = MutableStateFlow(false)
    private val _isSplitSelectorOpen = MutableStateFlow(false)
    private val _toastMessage = MutableStateFlow<String?>(null)

    // Current pair custom rate
    private val _customRate = MutableStateFlow<Double?>(null)

    init {
        updateCustomRateFromStorage()
        // Proactively refresh live rates on launch in background
        viewModelScope.launch {
            repository.refreshRates()
        }
    }

    private fun pairKey(): String =
        "${_baseCurrency.value.code}_${_targetCurrency.value.code}"

    private fun updateCustomRateFromStorage() {
        _customRate.value = prefs.getCustomRate(pairKey())
    }

    val uiState: StateFlow<ConverterUiState> = combine(
        _inputExpression,
        _baseCurrency,
        _targetCurrency,
        _favoriteCodes,
        repository.rates,
        repository.syncStatus,
        _feeConfig,
        _customRate,
        _isCurrencyPickerOpen,
        _pickerMode,
        _isFeeSheetOpen,
        _isCustomRateDialogOpen,
        _isSplitSelectorOpen,
        _toastMessage
    ) { args ->
        val expr = args[0] as String
        val base = args[1] as Currency
        val target = args[2] as Currency
        val favCodes = args[3] as List<String>
        val rates = args[4] as ExchangeRates
        val sync = args[5] as SyncStatus
        val fee = args[6] as FeeConfig
        val customRate = args[7] as? Double
        val pickerOpen = args[8] as Boolean
        val pickerMode = args[9] as CurrencyPickerMode
        val feeSheetOpen = args[10] as Boolean
        val customRateOpen = args[11] as Boolean
        val splitOpen = args[12] as Boolean
        val toast = args[13] as? String

        val evaluatedInput = MathExpressionEvaluator.evaluate(expr)

        val marketRate = rates.getRate(base.code, target.code)
        val isCustom = customRate != null && customRate > 0.0
        val effectiveRate = if (isCustom) customRate!! else marketRate

        val convertedAmount = evaluatedInput * effectiveRate
        val feeAmt = fee.calculateFee(convertedAmount)
        val totalAmt = fee.calculateTotal(convertedAmount)

        val watchlist = favCodes
            .filter { it != base.code } // don't show the base itself in watch favorites
            .map { Currency.findByCode(it) }

        val lastUpdatedText = when (sync) {
            SyncStatus.LIVE -> "Live Rates (${CurrencyFormatter.formatTimestamp(rates.lastUpdatedEpochMs)})"
            SyncStatus.CACHED -> "Cached Rates (${CurrencyFormatter.formatTimestamp(rates.lastUpdatedEpochMs)})"
            SyncStatus.SYNCING -> "Updating rates..."
            SyncStatus.ERROR -> "Offline (Cached ${CurrencyFormatter.formatTimestamp(rates.lastUpdatedEpochMs)})"
        }

        ConverterUiState(
            inputExpression = expr,
            evaluatedInputAmount = evaluatedInput,
            baseCurrency = base,
            targetCurrency = target,
            watchlistCurrencies = watchlist,
            exchangeRates = rates,
            syncStatus = sync,
            feeConfig = fee,
            customRateOverride = customRate,
            isCustomRateActive = isCustom,
            effectiveRate = effectiveRate,
            convertedAmount = convertedAmount,
            feeAmount = feeAmt,
            totalWithFee = totalAmt,
            lastUpdatedText = lastUpdatedText,
            isCurrencyPickerOpen = pickerOpen,
            pickerMode = pickerMode,
            isFeeSheetOpen = feeSheetOpen,
            isCustomRateDialogOpen = customRateOpen,
            isSplitSelectorOpen = splitOpen,
            toastMessage = toast
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConverterUiState()
    )

    // Keypad Input Handlers
    fun onDigitPress(digit: String) {
        val current = _inputExpression.value
        if (current == "0") {
            _inputExpression.value = digit
        } else {
            _inputExpression.value = current + digit
        }
    }

    fun onDecimalPress() {
        val current = _inputExpression.value
        // Only append decimal if last number token doesn't already contain a decimal
        val lastNumberToken = current.split(Regex("[+\\-×÷/*%]")).lastOrNull() ?: ""
        if (!lastNumberToken.contains('.')) {
            _inputExpression.value = if (current.isEmpty()) "0." else "$current."
        }
    }

    fun onOperatorPress(operator: String) {
        val current = _inputExpression.value
        if (current.isEmpty()) {
            if (operator == "−") _inputExpression.value = "−"
            return
        }

        val lastChar = current.last()
        if (lastChar in listOf('+', '−', '×', '÷', '-', '*', '/')) {
            // Replace previous operator
            _inputExpression.value = current.dropLast(1) + operator
        } else {
            _inputExpression.value = current + operator
        }
    }

    fun onPercentagePress() {
        val current = _inputExpression.value
        if (current.isNotEmpty() && current.last().isDigit()) {
            _inputExpression.value = "$current%"
        }
    }

    fun onBackspace() {
        val current = _inputExpression.value
        if (current.isNotEmpty()) {
            _inputExpression.value = current.dropLast(1).ifEmpty { "0" }
        }
    }

    fun onClear() {
        _inputExpression.value = "0"
    }

    fun onEquals() {
        val current = _inputExpression.value
        val evaluated = MathExpressionEvaluator.evaluate(current)
        _inputExpression.value = MathExpressionEvaluator.formatEvaluated(evaluated)
    }

    fun onQuickSplit(people: Int) {
        if (people <= 1) return
        val current = _inputExpression.value
        val evaluated = MathExpressionEvaluator.evaluate(current)
        val splitVal = evaluated / people
        _inputExpression.value = MathExpressionEvaluator.formatEvaluated(splitVal)
        _isSplitSelectorOpen.value = false
        _toastMessage.value = "Split by $people: ${MathExpressionEvaluator.formatEvaluated(splitVal)}"
    }

    fun onQuickDiscount(percent: Int) {
        val current = _inputExpression.value
        val evaluated = MathExpressionEvaluator.evaluate(current)
        val discounted = evaluated * (1.0 - (percent / 100.0))
        _inputExpression.value = MathExpressionEvaluator.formatEvaluated(discounted)
        _toastMessage.value = "-$percent% applied"
    }

    // Currency Swap & Selection
    fun onSwapCurrencies() {
        val oldBase = _baseCurrency.value
        val oldTarget = _targetCurrency.value
        _baseCurrency.value = oldTarget
        _targetCurrency.value = oldBase
        prefs.setBaseCurrency(oldTarget.code)
        prefs.setTargetCurrency(oldBase.code)
        updateCustomRateFromStorage()
    }

    fun onSelectCurrency(currency: Currency, mode: CurrencyPickerMode) {
        when (mode) {
            CurrencyPickerMode.SELECT_BASE -> {
                if (currency.code == _targetCurrency.value.code) {
                    onSwapCurrencies()
                } else {
                    _baseCurrency.value = currency
                    prefs.setBaseCurrency(currency.code)
                    updateCustomRateFromStorage()
                }
            }
            CurrencyPickerMode.SELECT_TARGET -> {
                if (currency.code == _baseCurrency.value.code) {
                    onSwapCurrencies()
                } else {
                    _targetCurrency.value = currency
                    prefs.setTargetCurrency(currency.code)
                    updateCustomRateFromStorage()
                }
            }
            CurrencyPickerMode.ADD_TO_WATCHLIST -> {
                val updated = prefs.addFavorite(currency.code)
                _favoriteCodes.value = updated
            }
        }
        _isCurrencyPickerOpen.value = false
    }

    fun onPromoteWatchlistTarget(targetCurrency: Currency) {
        _targetCurrency.value = targetCurrency
        prefs.setTargetCurrency(targetCurrency.code)
        updateCustomRateFromStorage()
    }

    fun onToggleFavorite(currencyCode: String) {
        val current = _favoriteCodes.value
        val updated = if (current.contains(currencyCode.uppercase())) {
            prefs.removeFavorite(currencyCode)
        } else {
            prefs.addFavorite(currencyCode)
        }
        _favoriteCodes.value = updated
    }

    // Fee Settings
    fun onUpdateFeeConfig(config: FeeConfig) {
        _feeConfig.value = config
        prefs.saveFeeConfig(config)
        _isFeeSheetOpen.value = false
    }

    fun onToggleFeeEnabled() {
        val current = _feeConfig.value
        val updated = current.copy(enabled = !current.enabled)
        _feeConfig.value = updated
        prefs.saveFeeConfig(updated)
    }

    // Custom Rate Override
    fun onSetCustomRate(rate: Double?) {
        val key = pairKey()
        prefs.setCustomRate(key, rate)
        _customRate.value = rate
        _isCustomRateDialogOpen.value = false
        _toastMessage.value = if (rate != null) "Custom rate saved" else "Reverted to market rate"
    }

    // Dialog / Sheet Visibility
    fun openCurrencyPicker(mode: CurrencyPickerMode) {
        _pickerMode.value = mode
        _isCurrencyPickerOpen.value = true
    }

    fun closeCurrencyPicker() {
        _isCurrencyPickerOpen.value = false
    }

    fun openFeeSheet() {
        _isFeeSheetOpen.value = true
    }

    fun closeFeeSheet() {
        _isFeeSheetOpen.value = false
    }

    fun openCustomRateDialog() {
        _isCustomRateDialogOpen.value = true
    }

    fun closeCustomRateDialog() {
        _isCustomRateDialogOpen.value = false
    }

    fun toggleSplitSelector() {
        _isSplitSelectorOpen.value = !_isSplitSelectorOpen.value
    }

    fun dismissToast() {
        _toastMessage.value = null
    }

    fun refreshRates() {
        viewModelScope.launch {
            val success = repository.refreshRates()
            _toastMessage.value = if (success) "Rates updated successfully" else "Using offline cached rates"
        }
    }
}

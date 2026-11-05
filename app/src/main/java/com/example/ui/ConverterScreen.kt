package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Currency
import com.example.ui.components.ConversionCard
import com.example.ui.components.CurrencyPickerDialog
import com.example.ui.components.CustomRateDialog
import com.example.ui.components.ErgonomicKeypad
import com.example.ui.components.FeeSettingsSheet
import com.example.ui.components.SplitBillDialog
import com.example.ui.components.TravelerStatusHeader
import com.example.ui.components.WatchlistBar
import com.example.util.CurrencyFormatter
import com.example.viewmodel.ConverterViewModel
import com.example.viewmodel.CurrencyPickerMode

@Composable
fun ConverterScreen(
    viewModel: ConverterViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Toast & Snackbar feedback
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.dismissToast()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // SCROLLABLE TOP SECTION (Status Header, Conversion Card, Glance Watchlist)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Traveler Status Header (Live / Offline Status, Fee quick chip, Refresh)
                TravelerStatusHeader(
                    syncStatus = uiState.syncStatus,
                    lastUpdatedText = uiState.lastUpdatedText,
                    feePercent = uiState.feeConfig.markupPercent,
                    isFeeEnabled = uiState.feeConfig.enabled,
                    isCustomRateActive = uiState.isCustomRateActive,
                    onRefresh = { viewModel.refreshRates() },
                    onOpenFeeSheet = { viewModel.openFeeSheet() },
                    onOpenCustomRate = { viewModel.openCustomRateDialog() }
                )

                // Main Conversion Card (Base & Target with live math & fee breakdown)
                ConversionCard(
                    baseCurrency = uiState.baseCurrency,
                    targetCurrency = uiState.targetCurrency,
                    inputExpression = uiState.inputExpression,
                    evaluatedInputAmount = uiState.evaluatedInputAmount,
                    convertedAmount = uiState.convertedAmount,
                    feeConfig = uiState.feeConfig,
                    feeAmount = uiState.feeAmount,
                    totalWithFee = uiState.totalWithFee,
                    effectiveRate = uiState.effectiveRate,
                    isCustomRateActive = uiState.isCustomRateActive,
                    onSwap = { viewModel.onSwapCurrencies() },
                    onOpenBaseCurrencyPicker = { viewModel.openCurrencyPicker(CurrencyPickerMode.SELECT_BASE) },
                    onOpenTargetCurrencyPicker = { viewModel.openCurrencyPicker(CurrencyPickerMode.SELECT_TARGET) },
                    onCopyTargetResult = {
                        val displayAmount = if (uiState.feeConfig.enabled) uiState.totalWithFee else uiState.convertedAmount
                        val text = CurrencyFormatter.formatAmount(
                            displayAmount,
                            uiState.targetCurrency,
                            includeSymbol = true
                        )
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(context, "Copied $text to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )

                // Glanceable Multi-Currency Live Watchlist
                WatchlistBar(
                    baseCurrency = uiState.baseCurrency,
                    activeTargetCurrency = uiState.targetCurrency,
                    watchlistCurrencies = uiState.watchlistCurrencies,
                    evaluatedInputAmount = uiState.evaluatedInputAmount,
                    rates = uiState.exchangeRates,
                    onPromoteToTarget = { viewModel.onPromoteWatchlistTarget(it) },
                    onAddFavorite = { viewModel.openCurrencyPicker(CurrencyPickerMode.ADD_TO_WATCHLIST) }
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            // BOTTOM ANCHORED SECTION (Ergonomic Thumb-Zone Keypad)
            ErgonomicKeypad(
                onDigitPress = { viewModel.onDigitPress(it) },
                onDecimalPress = { viewModel.onDecimalPress() },
                onOperatorPress = { viewModel.onOperatorPress(it) },
                onPercentagePress = { viewModel.onPercentagePress() },
                onBackspace = { viewModel.onBackspace() },
                onClear = { viewModel.onClear() },
                onEquals = { viewModel.onEquals() },
                onSwapCurrencies = { viewModel.onSwapCurrencies() },
                onOpenSplitSelector = { viewModel.toggleSplitSelector() },
                onQuickDiscount = { viewModel.onQuickDiscount(it) }
            )
        }
    }

    // MODAL SHEETS & DIALOGS
    // 1. Currency Picker
    if (uiState.isCurrencyPickerOpen) {
        val currentSelected = when (uiState.pickerMode) {
            CurrencyPickerMode.SELECT_BASE -> uiState.baseCurrency
            CurrencyPickerMode.SELECT_TARGET -> uiState.targetCurrency
            CurrencyPickerMode.ADD_TO_WATCHLIST -> uiState.targetCurrency
        }
        val favoriteCodes = uiState.watchlistCurrencies.map { it.code }

        CurrencyPickerDialog(
            mode = uiState.pickerMode,
            currentSelectedCurrency = currentSelected,
            favoriteCodes = favoriteCodes,
            onSelectCurrency = { viewModel.onSelectCurrency(it, uiState.pickerMode) },
            onToggleFavorite = { viewModel.onToggleFavorite(it) },
            onDismiss = { viewModel.closeCurrencyPicker() }
        )
    }

    // 2. Card Markup & ATM Fee Settings Sheet
    if (uiState.isFeeSheetOpen) {
        FeeSettingsSheet(
            currentConfig = uiState.feeConfig,
            onSaveConfig = { viewModel.onUpdateFeeConfig(it) },
            onDismiss = { viewModel.closeFeeSheet() }
        )
    }

    // 3. Custom Rate Override Dialog
    if (uiState.isCustomRateDialogOpen) {
        val marketRate = uiState.exchangeRates.getRate(
            uiState.baseCurrency.code,
            uiState.targetCurrency.code
        )
        CustomRateDialog(
            baseCurrency = uiState.baseCurrency,
            targetCurrency = uiState.targetCurrency,
            marketRate = marketRate,
            currentCustomRate = uiState.customRateOverride,
            onSaveRate = { viewModel.onSetCustomRate(it) },
            onDismiss = { viewModel.closeCustomRateDialog() }
        )
    }

    // 4. Split Bill Dialog
    if (uiState.isSplitSelectorOpen) {
        SplitBillDialog(
            totalAmountBase = uiState.evaluatedInputAmount,
            baseCurrency = uiState.baseCurrency,
            targetCurrency = uiState.targetCurrency,
            effectiveRate = uiState.effectiveRate,
            onApplySplit = { viewModel.onQuickSplit(it) },
            onDismiss = { viewModel.toggleSplitSelector() }
        )
    }
}

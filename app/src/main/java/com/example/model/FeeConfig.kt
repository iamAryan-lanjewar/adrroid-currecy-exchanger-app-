package com.example.model

data class FeeConfig(
    val markupPercent: Double = 2.5,
    val enabled: Boolean = false,
    val presetName: String = "Typical Bank (2.5%)"
) {
    fun calculateFee(convertedAmount: Double): Double {
        if (!enabled || markupPercent <= 0.0) return 0.0
        return convertedAmount * (markupPercent / 100.0)
    }

    fun calculateTotal(convertedAmount: Double): Double {
        return convertedAmount + calculateFee(convertedAmount)
    }

    companion object {
        val PRESETS = listOf(
            FeeConfig(0.0, true, "Zero Fee (Wise / Revolut 0.0%)"),
            FeeConfig(1.5, true, "Standard Card (Visa/MC 1.5%)"),
            FeeConfig(2.5, true, "Typical Bank Card (2.5%)"),
            FeeConfig(3.5, true, "ATM / Airport DCC (3.5%)")
        )
    }
}

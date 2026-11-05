package com.example

import com.example.model.Currency
import com.example.model.ExchangeRates
import com.example.model.FeeConfig
import com.example.util.CurrencyFormatter
import com.example.util.MathExpressionEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testMathEvaluatorBasicArithmetic() {
        assertEquals(150.0, MathExpressionEvaluator.evaluate("100 + 50"), 0.001)
        assertEquals(70.0, MathExpressionEvaluator.evaluate("100 - 30"), 0.001)
        assertEquals(240.0, MathExpressionEvaluator.evaluate("60 * 4"), 0.001)
        assertEquals(30.0, MathExpressionEvaluator.evaluate("120 / 4"), 0.001)
    }

    @Test
    fun testMathEvaluatorOperatorPrecedence() {
        // 50 + 20 * 2 = 90
        assertEquals(90.0, MathExpressionEvaluator.evaluate("50 + 20 × 2"), 0.001)
        // 100 - 20 / 2 = 90
        assertEquals(90.0, MathExpressionEvaluator.evaluate("100 − 20 ÷ 2"), 0.001)
    }

    @Test
    fun testMathEvaluatorPercentageAndDiscounts() {
        // 100 - 20% = 80
        assertEquals(80.0, MathExpressionEvaluator.evaluate("100 - 20%"), 0.001)
        // 50 + 10% = 55
        assertEquals(55.0, MathExpressionEvaluator.evaluate("50 + 10%"), 0.001)
    }

    @Test
    fun testMathEvaluatorTrailingOperatorSafety() {
        // Incomplete typing shouldn't crash
        assertEquals(120.0, MathExpressionEvaluator.evaluate("120 + "), 0.001)
        assertEquals(0.0, MathExpressionEvaluator.evaluate(""), 0.001)
    }

    @Test
    fun testFeeCalculations() {
        val feeConfig = FeeConfig(markupPercent = 2.5, enabled = true)
        val amount = 100.0
        val fee = feeConfig.calculateFee(amount)
        val total = feeConfig.calculateTotal(amount)

        assertEquals(2.5, fee, 0.001)
        assertEquals(102.5, total, 0.001)
    }

    @Test
    fun testCrossRateCalculation() {
        val rates = ExchangeRates(
            baseCode = "USD",
            rates = mapOf(
                "USD" to 1.0,
                "EUR" to 0.9,
                "JPY" to 150.0
            )
        )
        // 1 EUR to JPY = (1 / 0.9) * 150 = 166.666
        val eurToJpy = rates.getRate("EUR", "JPY")
        assertEquals(166.666, eurToJpy, 0.01)
    }

    @Test
    fun testCurrencyFormatting() {
        val usd = Currency.findByCode("USD")
        val jpy = Currency.findByCode("JPY")

        assertEquals("1,234.56", CurrencyFormatter.formatAmount(1234.56, usd))
        assertEquals("1,235", CurrencyFormatter.formatAmount(1234.56, jpy))
    }
}

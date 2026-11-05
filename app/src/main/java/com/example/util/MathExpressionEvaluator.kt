package com.example.util

import java.util.Stack
import kotlin.math.round

object MathExpressionEvaluator {

    /**
     * Evaluates a mathematical expression string that may contain:
     * digits 0-9, decimal point, +, -, ×, *, ÷, /, and %.
     * Handles order of operations (* and / before + and -) and percentage adjustments.
     * Returns the evaluated Double value. Returns 0.0 if empty or invalid.
     */
    fun evaluate(rawExpr: String): Double {
        if (rawExpr.isBlank()) return 0.0

        val sanitized = sanitize(rawExpr)
        if (sanitized.isBlank()) return 0.0

        return try {
            evalSanitized(sanitized)
        } catch (_: Exception) {
            // If trailing operator like "120+", strip trailing non-digits and try evaluating prefix
            val trimmed = sanitized.trimEnd('+', '-', '*', '/', '%')
            if (trimmed.isNotBlank()) {
                try {
                    evalSanitized(trimmed)
                } catch (_: Exception) {
                    0.0
                }
            } else {
                0.0
            }
        }
    }

    private fun sanitize(input: String): String {
        return input
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(" ", "")
    }

    private fun evalSanitized(expr: String): Double {
        // Tokenize into numbers and operators
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()

        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            if (c.isDigit() || c == '.') {
                sb.append(c)
            } else if (c == '%') {
                if (sb.isNotEmpty()) {
                    tokens.add(sb.toString())
                    sb.clear()
                }
                tokens.add("%")
            } else if (c in listOf('+', '-', '*', '/')) {
                if (sb.isNotEmpty()) {
                    tokens.add(sb.toString())
                    sb.clear()
                } else if (c == '-' && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/"))) {
                    // Unary minus
                    sb.append(c)
                    i++
                    continue
                }
                tokens.add(c.toString())
            }
            i++
        }
        if (sb.isNotEmpty()) {
            tokens.add(sb.toString())
        }

        if (tokens.isEmpty()) return 0.0

        // Handle percentage: e.g. "100 - 20%" -> "100 - (100 * 0.20)" = 80
        // Or "200 * 15%" -> "200 * 0.15" = 30
        val resolvedTokens = mutableListOf<String>()
        var idx = 0
        while (idx < tokens.size) {
            val token = tokens[idx]
            if (token == "%") {
                if (resolvedTokens.isNotEmpty()) {
                    val prevNumStr = resolvedTokens.removeAt(resolvedTokens.size - 1)
                    val prevNum = prevNumStr.toDoubleOrNull() ?: 0.0
                    val baseForPercent = if (resolvedTokens.size >= 2) {
                        resolvedTokens[resolvedTokens.size - 2].toDoubleOrNull() ?: 1.0
                    } else {
                        1.0
                    }
                    val op = if (resolvedTokens.isNotEmpty()) resolvedTokens.last() else ""
                    val percentValue = if (op == "+" || op == "-") {
                        baseForPercent * (prevNum / 100.0)
                    } else {
                        prevNum / 100.0
                    }
                    resolvedTokens.add(percentValue.toString())
                }
            } else {
                resolvedTokens.add(token)
            }
            idx++
        }

        // Shunting-yard algorithm for standard arithmetic
        val outputQueue = mutableListOf<String>()
        val opStack = Stack<String>()

        fun precedence(op: String): Int = when (op) {
            "*", "/" -> 2
            "+", "-" -> 1
            else -> 0
        }

        for (token in resolvedTokens) {
            val num = token.toDoubleOrNull()
            if (num != null) {
                outputQueue.add(token)
            } else if (token in listOf("+", "-", "*", "/")) {
                while (!opStack.isEmpty() && precedence(opStack.peek()) >= precedence(token)) {
                    outputQueue.add(opStack.pop())
                }
                opStack.push(token)
            }
        }
        while (!opStack.isEmpty()) {
            outputQueue.add(opStack.pop())
        }

        // Evaluate RPN
        val valStack = Stack<Double>()
        for (token in outputQueue) {
            val num = token.toDoubleOrNull()
            if (num != null) {
                valStack.push(num)
            } else if (valStack.size >= 2) {
                val b = valStack.pop()
                val a = valStack.pop()
                val res = when (token) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> if (b == 0.0) 0.0 else a / b
                    else -> 0.0
                }
                valStack.push(res)
            }
        }

        return if (valStack.isNotEmpty()) valStack.pop() else 0.0
    }

    /**
     * Formats evaluated double into clean string (e.g. 120.0 -> "120", 45.50 -> "45.5")
     */
    fun formatEvaluated(value: Double): String {
        return if (value == round(value) && value < 1e12 && value > -1e12) {
            value.toLong().toString()
        } else {
            // Round to 4 decimal places max for formula evaluation preview
            val rounded = (round(value * 10000.0) / 10000.0)
            if (rounded == round(rounded)) {
                rounded.toLong().toString()
            } else {
                rounded.toString()
            }
        }
    }
}

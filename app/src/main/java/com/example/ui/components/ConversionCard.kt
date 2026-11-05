package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Currency
import com.example.model.FeeConfig
import com.example.ui.theme.AmberGold
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CurrencyFormatter
import com.example.util.MathExpressionEvaluator

@Composable
fun ConversionCard(
    baseCurrency: Currency,
    targetCurrency: Currency,
    inputExpression: String,
    evaluatedInputAmount: Double,
    convertedAmount: Double,
    feeConfig: FeeConfig,
    feeAmount: Double,
    totalWithFee: Double,
    effectiveRate: Double,
    isCustomRateActive: Boolean,
    onSwap: () -> Unit,
    onOpenBaseCurrencyPicker: () -> Unit,
    onOpenTargetCurrencyPicker: () -> Unit,
    onCopyTargetResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // BASE CURRENCY SECTION (Input & Formula)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Currency Selector Pill
                CurrencySelectorPill(
                    currency = baseCurrency,
                    label = "You Pay / Enter",
                    onClick = onOpenBaseCurrencyPicker,
                    testTag = "base_currency_selector"
                )

                // Input Expression & Evaluated Math Preview
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                ) {
                    val formattedExpr = if (inputExpression.isEmpty()) "0" else inputExpression
                    Text(
                        text = formattedExpr,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (formattedExpr.length > 10) 22.sp else 28.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.testTag("base_input_expression")
                    )

                    // If expression has math operators (+, -, *, /), show live intermediate calculation
                    val hasMathOperator = inputExpression.any { it in listOf('+', '−', '×', '÷', '-', '*', '/', '%') }
                    if (hasMathOperator) {
                        Text(
                            text = "= ${CurrencyFormatter.formatAmount(evaluatedInputAmount, baseCurrency, includeSymbol = true)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                color = TextSecondary,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("evaluated_math_preview")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CENTRAL EXCHANGE RATE & QUICK SWAP DIVIDER
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Background subtle divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )

                // Middle interactive rate badge + Quick Swap button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            1.dp,
                            if (isCustomRateActive) AmberGold.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1 ${baseCurrency.code} = ${CurrencyFormatter.formatRate(effectiveRate)} ${targetCurrency.code}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCustomRateActive) AmberGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Quick Swap Icon Button with 48dp minimum touch target
                    IconButton(
                        onClick = onSwap,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("quick_swap_currencies_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Swap Currencies",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TARGET CURRENCY SECTION (Converted result & Fee Breakdown)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Target Currency Selector Pill
                CurrencySelectorPill(
                    currency = targetCurrency,
                    label = "You Receive / Cost",
                    onClick = onOpenTargetCurrencyPicker,
                    testTag = "target_currency_selector"
                )

                // Converted Amount with copy button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    val displayAmount = if (feeConfig.enabled) totalWithFee else convertedAmount
                    val formattedResult = CurrencyFormatter.formatAmount(
                        displayAmount,
                        targetCurrency,
                        includeSymbol = true
                    )

                    Text(
                        text = formattedResult,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (formattedResult.length > 10) 24.sp else 32.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .testTag("target_converted_amount")
                    )

                    IconButton(
                        onClick = onCopyTargetResult,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("copy_converted_result_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Converted Amount",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Traveler Fee Breakdown Banner (when card markup or ATM fee is active)
            AnimatedVisibility(
                visible = feeConfig.enabled && feeConfig.markupPercent > 0.0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Card Markup (+${feeConfig.markupPercent}%):",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        )
                        Text(
                            text = "+${CurrencyFormatter.formatAmount(feeAmount, targetCurrency, includeSymbol = true)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net without fee:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = CurrencyFormatter.formatAmount(convertedAmount, targetCurrency, includeSymbol = true),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencySelectorPill(
    currency: Currency,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontSize = 11.sp
            ),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = currency.flagEmoji,
                fontSize = 18.sp
            )
            Text(
                text = currency.code,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Currency",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

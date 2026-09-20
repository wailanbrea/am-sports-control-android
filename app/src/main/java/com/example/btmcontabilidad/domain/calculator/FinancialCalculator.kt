package com.example.btmcontabilidad.domain.calculator

import com.example.btmcontabilidad.domain.model.BalanceState
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object FinancialCalculator {

    @Volatile
    private var configuredCurrencyCode: String = "USD"

    val ZERO: BigDecimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)

    fun roundMoney(amount: BigDecimal): BigDecimal {
        return amount.setScale(2, RoundingMode.HALF_UP)
    }

    fun setCurrencyCode(currencyCode: String) {
        configuredCurrencyCode = currencyCode.trim().uppercase().ifBlank { "USD" }
    }

    fun currencySymbol(): String = when (configuredCurrencyCode) {
        "DOP" -> "RD$"
        "EUR" -> "€"
        else -> "US$"
    }

    fun determineBalanceState(balance: BigDecimal): BalanceState {
        val rounded = roundMoney(balance)
        return when {
            rounded > BigDecimal.ZERO -> BalanceState.POR_COBRAR
            rounded < BigDecimal.ZERO -> BalanceState.POR_ENVIAR
            else -> BalanceState.SALDADA
        }
    }

    fun formatCurrency(
        amount: BigDecimal,
        currencySymbol: String? = null
    ): String {
        val rounded = roundMoney(amount)
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val formattedNumber = formatter.format(rounded.abs())
        val prefix = if (rounded.compareTo(BigDecimal.ZERO) < 0) "- " else ""
        return "$prefix${currencySymbol ?: currencySymbol()} $formattedNumber"
    }
}

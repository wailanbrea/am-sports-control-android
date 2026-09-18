package com.example.btmcontabilidad.domain.model

import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.serializers.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
enum class CashMovementType(val label: String) {
    INCOME("Entrada"),
    EXPENSE("Retiro / Gasto"),
    BRANCH_TRANSFER("Transferencia a banca")
}

@Serializable
data class CashMovement(
    val id: String,
    val type: CashMovementType,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val businessDate: String,
    val reason: String,
    val branchId: String? = null,
    val reference: String? = null,
    val notes: String? = null,
    val createdAt: String? = null
)

@Serializable
data class CashBox(
    val currencyCode: String,
    @Serializable(with = BigDecimalSerializer::class)
    val currentBalance: BigDecimal,
    val entries: List<CashMovement> = emptyList()
) {
    val roundedBalance: BigDecimal
        get() = FinancialCalculator.roundMoney(currentBalance)
}

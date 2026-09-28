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
    @Serializable(with = BigDecimalSerializer::class)
    val balanceBefore: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val balanceAfter: BigDecimal? = null,
    val businessDate: String,
    val reason: String,
    val branchId: String? = null,
    val reference: String? = null,
    val notes: String? = null,
    val createdAt: String? = null,
    val createdBy: String? = null,
    val branchCode: String? = null,
    val branchName: String? = null
)

@Serializable
data class CashBoxEntity(
    val id: Long,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class)
    val balance: BigDecimal,
    val currencyCode: String,
    val isDefault: Boolean = false,
    val description: String? = null
)

@Serializable
data class CashBox(
    val id: Long? = null,
    val name: String? = null,
    val currencyCode: String,
    @Serializable(with = BigDecimalSerializer::class)
    val currentBalance: BigDecimal,
    val entries: List<CashMovement> = emptyList()
) {
    val roundedBalance: BigDecimal
        get() = FinancialCalculator.roundMoney(currentBalance)
}

package com.example.btmcontabilidad.domain.model

import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.serializers.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class Branch(
    val id: String,
    val code: String,
    val name: String,
    val description: String? = null,
    val route: String,
    val operatorName: String,
    val phone: String? = null,
    val ownerName: String? = null,
    val ownerPhone: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val currentBalance: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val commissionRate: BigDecimal = BigDecimal.ZERO,
    val status: BranchStatus = BranchStatus.ACTIVE,
    val collectorUserId: Long? = null,
    val collectorName: String? = null
) {

    val balanceState: BalanceState
        get() = FinancialCalculator.determineBalanceState(currentBalance)
}

typealias Banca = Branch

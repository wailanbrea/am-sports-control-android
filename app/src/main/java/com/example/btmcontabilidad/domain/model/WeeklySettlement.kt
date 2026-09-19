package com.example.btmcontabilidad.domain.model

import com.example.btmcontabilidad.domain.model.serializers.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class WeeklySettlement(
    val id: String,
    val branchId: String,
    val weekStart: String,
    val weekEnd: String,
    @Serializable(with = BigDecimalSerializer::class)
    val salesAmount: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val prizesAmount: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val cashDeliveredAmount: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val weeklyBalance: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val balanceBefore: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val balanceAfter: BigDecimal,
    val notes: String? = null,
    val status: String = "confirmed"
)

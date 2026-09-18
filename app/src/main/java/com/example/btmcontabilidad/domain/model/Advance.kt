package com.example.btmcontabilidad.domain.model

import com.example.btmcontabilidad.domain.model.serializers.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class Advance(
    val id: String,
    val branchId: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val reason: String,
    val businessDate: String, // ISO yyyy-MM-dd
    val notes: String? = null,
    val status: AdvanceStatus = AdvanceStatus.REGISTERED
)

typealias Adelanto = Advance

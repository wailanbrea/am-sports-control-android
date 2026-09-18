package com.example.btmcontabilidad.domain.model

import com.example.btmcontabilidad.domain.model.serializers.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class LedgerEntry(
    val id: String,
    val branchId: String,
    val sourceType: LedgerSourceType,
    val sourceId: String,
    val entryType: LedgerEntryType,
    @Serializable(with = BigDecimalSerializer::class)
    val signedAmount: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val balanceBefore: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val balanceAfter: BigDecimal,
    val businessDate: String,
    val description: String,
    val createdBy: String = "SISTEMA",
    val reversalOfEntryId: String? = null
)

typealias LedgerTransaction = LedgerEntry

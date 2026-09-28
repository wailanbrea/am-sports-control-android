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
    val reversalOfEntryId: String? = null,
    val branchCode: String? = null,
    val branchName: String? = null
)

typealias LedgerTransaction = LedgerEntry

fun LedgerEntry.isMoneyDelivery(): Boolean {
    val desc = description.lowercase()
    val src = sourceType.name.lowercase()
    return src.contains("money_delivery") || src.contains("moneydelivery") ||
            desc.contains("dinero llevado") || desc.contains("llevado para") ||
            desc.contains("llevado a la banca")
}

fun LedgerEntry.isCollection(): Boolean {
    val desc = description.lowercase()
    val src = sourceType.name.lowercase()
    return src.contains("collection") || desc.contains("cobro")
}

fun LedgerEntry.isNegativeMovement(): Boolean {
    val desc = description.lowercase()
    val src = sourceType.name.lowercase()
    return isMoneyDelivery() ||
            src.contains("advance") ||
            desc.contains("pérdida") ||
            desc.contains("perdida") ||
            desc.contains("adelanto") ||
            entryType.name.equals("result_negative", ignoreCase = true) ||
            (signedAmount < BigDecimal.ZERO && !isCollection())
}

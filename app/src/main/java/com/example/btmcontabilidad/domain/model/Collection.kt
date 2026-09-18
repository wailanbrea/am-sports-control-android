package com.example.btmcontabilidad.domain.model

import com.example.btmcontabilidad.domain.model.serializers.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class Collection(
    val id: String,
    val branchId: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    val reference: String? = null,
    val businessDate: String, // ISO yyyy-MM-dd
    val notes: String? = null,
    val status: CollectionStatus = CollectionStatus.REGISTERED
)

typealias Cobro = Collection

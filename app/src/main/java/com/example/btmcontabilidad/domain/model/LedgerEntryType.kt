package com.example.btmcontabilidad.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class LedgerEntryType(val label: String) {
    DEBIT("Cargo / Débito"),    // Increases debt / balance
    CREDIT("Abono / Crédito")   // Decreases debt / balance
}

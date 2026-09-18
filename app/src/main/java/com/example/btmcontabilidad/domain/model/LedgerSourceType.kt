package com.example.btmcontabilidad.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class LedgerSourceType(val label: String) {
    COLLECTION("Cobro Recibido"),
    ADVANCE("Adelanto por Pérdidas"),
    ADJUSTMENT("Ajuste Manual")
}

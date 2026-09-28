package com.example.btmcontabilidad.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class LedgerSourceType(val label: String) {
    COLLECTION("Cobro Recibido"),
    MONEY_DELIVERY("Dinero Llevado a Banca"),
    MANUAL_RESULT("Resultado Operativo"),
    ADVANCE("Adelanto por Pérdidas"),
    WEEKLY_SETTLEMENT("Cuadre Semanal"),
    ADJUSTMENT("Ajuste Manual")
}

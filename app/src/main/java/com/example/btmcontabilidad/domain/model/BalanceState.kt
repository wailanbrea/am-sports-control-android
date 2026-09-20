package com.example.btmcontabilidad.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class BalanceState(val label: String) {
    POR_COBRAR("Por Cobrar"),           // balance > 0 (la banca le debe a la empresa)
    POR_ENVIAR("Por Enviar (Premios)"), // balance < 0 (la empresa le debe llevar para premios)
    SALDADA("Saldada")                  // balance == 0 (al día)
}

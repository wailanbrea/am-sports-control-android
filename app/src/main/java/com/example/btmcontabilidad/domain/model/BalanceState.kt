package com.example.btmcontabilidad.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class BalanceState(val label: String) {
    POR_COBRAR("Por Cobrar"),      // balance > 0
    A_FAVOR_BANCA("A Favor Banca"), // balance < 0
    SALDADA("Saldada")              // balance == 0
}

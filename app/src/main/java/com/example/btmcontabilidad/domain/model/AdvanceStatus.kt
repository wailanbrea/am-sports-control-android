package com.example.btmcontabilidad.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AdvanceStatus(val label: String) {
    REGISTERED("Registrado"),
    APPROVED("Aprobado"),
    CANCELLED("Cancelado")
}

package com.example.btmcontabilidad.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PaymentMethod(val label: String) {
    EFECTIVO("Efectivo"),
    TRANSFERENCIA("Transferencia bancaria"),
    OTRO("Otro")
}

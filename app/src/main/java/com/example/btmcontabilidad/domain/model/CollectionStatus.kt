package com.example.btmcontabilidad.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class CollectionStatus(val label: String) {
    REGISTERED("Registrado"),
    VERIFIED("Verificado"),
    CANCELLED("Cancelado")
}

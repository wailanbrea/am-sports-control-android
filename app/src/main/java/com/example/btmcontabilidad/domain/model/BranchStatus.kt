package com.example.btmcontabilidad.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class BranchStatus(val label: String) {
    ACTIVE("Activa"),
    INACTIVE("Inactiva"),
    SUSPENDED("Suspendida")
}

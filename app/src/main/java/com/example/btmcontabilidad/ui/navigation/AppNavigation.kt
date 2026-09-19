package com.example.btmcontabilidad.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute

@Serializable
data object DashboardRoute : AppRoute

@Serializable
data object BancasRoute : AppRoute

@Serializable
data class BranchDetailRoute(val branchId: String) : AppRoute

@Serializable
data class BranchFormRoute(val branchId: String? = null) : AppRoute

@Serializable
data class RegisterCollectionRoute(val initialBranchId: String? = null) : AppRoute

@Serializable
data class RegisterAdvanceRoute(val initialBranchId: String? = null) : AppRoute

@Serializable
data class RegisterWeeklySettlementRoute(val branchId: String, val previousBalance: String) : AppRoute

@Serializable
data object LedgerRoute : AppRoute

@Serializable
data object ReportsRoute : AppRoute

@Serializable
data object CashBoxRoute : AppRoute

@Serializable
data object ExportRoute : AppRoute

@Serializable
data object CollectionListRoute : AppRoute

@Serializable
data object AdvanceListRoute : AppRoute

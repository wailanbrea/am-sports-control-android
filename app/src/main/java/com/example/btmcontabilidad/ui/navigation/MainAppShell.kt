package com.example.btmcontabilidad.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.btmcontabilidad.ui.screens.AdvanceListScreen
import com.example.btmcontabilidad.ui.screens.BancasScreen
import com.example.btmcontabilidad.ui.screens.BranchDetailScreen
import com.example.btmcontabilidad.ui.screens.BranchFormScreen
import com.example.btmcontabilidad.ui.screens.CashBoxScreen
import com.example.btmcontabilidad.ui.screens.CollectionListScreen
import com.example.btmcontabilidad.ui.screens.DashboardScreen
import com.example.btmcontabilidad.ui.screens.ExportScreen
import com.example.btmcontabilidad.ui.screens.LedgerScreen
import com.example.btmcontabilidad.ui.screens.RegisterAdvanceScreen
import com.example.btmcontabilidad.ui.screens.RegisterCollectionScreen
import com.example.btmcontabilidad.ui.screens.RegisterManualResultScreen
import com.example.btmcontabilidad.ui.screens.RegisterMoneyDeliveryScreen
import com.example.btmcontabilidad.ui.screens.RegisterWeeklySettlementScreen
import com.example.btmcontabilidad.ui.screens.ReportsScreen
import com.example.btmcontabilidad.ui.theme.DeepNavy
import com.example.btmcontabilidad.ui.theme.PrimaryBlue
import java.math.BigDecimal

enum class BottomTab(
    val label: String,
    val icon: ImageVector,
    val route: AppRoute
) {
    INICIO("Inicio", Icons.Default.Home, DashboardRoute),
    BANCAS("Bancas", Icons.Default.Store, BancasRoute),
    COBROS("Cobros", Icons.Default.Payments, RegisterCollectionRoute()),
    MOVIMIENTOS("Movs.", Icons.AutoMirrored.Filled.ListAlt, LedgerRoute),
    MAS("Más", Icons.Default.MoreHoriz, ReportsRoute)
}

@Composable
fun MainAppShell(onLogout: () -> Unit = {}) {
    val backStack = remember { mutableStateListOf<AppRoute>(DashboardRoute) }
    val currentRoute = backStack.lastOrNull() ?: DashboardRoute

    val currentTab = when (currentRoute) {
        is DashboardRoute -> BottomTab.INICIO
        is BancasRoute -> BottomTab.BANCAS
        is RegisterCollectionRoute -> BottomTab.COBROS
        is LedgerRoute -> BottomTab.MOVIMIENTOS
        is ReportsRoute -> BottomTab.MAS
        else -> null
    }

    Scaffold(
        bottomBar = {
            if (currentTab != null) {
                NavigationBar(
                    containerColor = DeepNavy,
                    contentColor = PrimaryBlue
                ) {
                    BottomTab.entries.forEach { tab ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    backStack.clear()
                                    backStack.add(tab.route)
                                }
                            },
                            icon = {
                                Icon(imageVector = tab.icon, contentDescription = tab.label)
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryBlue,
                                selectedTextColor = Color.White,
                                unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                unselectedTextColor = Color.White.copy(alpha = 0.5f),
                                indicatorColor = PrimaryBlue.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { route ->
            NavEntry(key = route) {
                when (route) {
                    is DashboardRoute -> DashboardScreen(
                        onNavigateToRegisterCollection = { backStack.add(RegisterCollectionRoute()) },
                        onNavigateToRegisterAdvance = { backStack.add(RegisterAdvanceRoute()) },
                        onNavigateToMoneyDelivery = { backStack.add(RegisterMoneyDeliveryRoute()) },
                        onNavigateToManualResult = { backStack.add(RegisterManualResultRoute()) },
                        onNavigateToLedger = { backStack.add(LedgerRoute) },
                        onNavigateToBancas = { backStack.add(BancasRoute) },
                        onNavigateToProfile = { backStack.add(ReportsRoute) }
                    )

                    is BancasRoute -> BancasScreen(
                        onBranchSelected = { branchId -> backStack.add(BranchDetailRoute(branchId)) },
                        onCreateBranch = { backStack.add(BranchFormRoute()) },
                        onRegisterCollection = { branchId -> backStack.add(RegisterCollectionRoute(branchId)) },
                        onRegisterWeeklySettlement = { branchId, balance ->
                            backStack.add(RegisterManualResultRoute(branchId, balance))
                        },
                        onTransferToBranch = { branchId -> backStack.add(RegisterMoneyDeliveryRoute(branchId)) }
                    )

                    is BranchDetailRoute -> BranchDetailScreen(
                        branchId = route.branchId,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onEdit = { branchId -> backStack.add(BranchFormRoute(branchId)) },
                        onDeleted = { backStack.removeLastOrNull() },
                        onRegisterCollection = { branchId -> backStack.add(RegisterCollectionRoute(branchId)) },
                        onRegisterAdvance = { branchId -> backStack.add(RegisterAdvanceRoute(branchId)) },
                        onRegisterWeeklySettlement = { branchId, balance ->
                            backStack.add(RegisterManualResultRoute(branchId, balance))
                        },
                        onTransferToBranch = { branchId ->
                            backStack.add(RegisterMoneyDeliveryRoute(branchId))
                        },
                        onRegisterManualResult = { branchId, balance ->
                            backStack.add(RegisterManualResultRoute(branchId, balance))
                        },
                        onRegisterMoneyDelivery = { branchId, suggested ->
                            backStack.add(RegisterMoneyDeliveryRoute(branchId, suggested))
                        }
                    )

                    is BranchFormRoute -> BranchFormScreen(
                        branchId = route.branchId,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onSaved = { branchId ->
                            backStack.removeLastOrNull()
                            if (backStack.lastOrNull() is BranchDetailRoute) backStack.removeLastOrNull()
                            backStack.add(BranchDetailRoute(branchId))
                        }
                    )

                    is RegisterCollectionRoute -> RegisterCollectionScreen(
                        initialBranchId = route.initialBranchId,
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )

                    is RegisterAdvanceRoute -> RegisterAdvanceScreen(
                        initialBranchId = route.initialBranchId,
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )

                    is RegisterManualResultRoute -> RegisterManualResultScreen(
                        branchId = route.branchId,
                        initialBalance = route.previousBalance.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onNavigateToMoneyDelivery = { bId, suggested, resultId ->
                            backStack.removeLastOrNull()
                            backStack.add(RegisterMoneyDeliveryRoute(bId, suggested, resultId))
                        },
                        onSaved = {
                            backStack.removeLastOrNull()
                            backStack.add(BranchDetailRoute(route.branchId))
                        }
                    )

                    is RegisterMoneyDeliveryRoute -> RegisterMoneyDeliveryScreen(
                        branchId = route.branchId,
                        suggestedAmount = route.suggestedAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        manualResultId = route.manualResultId,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onSaved = {
                            backStack.removeLastOrNull()
                            backStack.add(BranchDetailRoute(route.branchId))
                        }
                    )

                    is RegisterWeeklySettlementRoute -> RegisterWeeklySettlementScreen(
                        branchId = route.branchId,
                        previousBalance = route.previousBalance.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onSaved = {
                            backStack.removeLastOrNull()
                            backStack.removeLastOrNull()
                            backStack.add(BranchDetailRoute(route.branchId))
                        }
                    )

                    is CollectionListRoute -> CollectionListScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onNavigateToRegister = { backStack.add(RegisterCollectionRoute()) }
                    )

                    is AdvanceListRoute -> AdvanceListScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onNavigateToRegister = { backStack.add(RegisterAdvanceRoute()) }
                    )

                    is LedgerRoute -> LedgerScreen()

                    is ReportsRoute -> ReportsScreen(
                        onNavigateToDashboard = { backStack.add(DashboardRoute) },
                        onNavigateToLedger = { backStack.add(LedgerRoute) },
                        onNavigateToBancas = { backStack.add(BancasRoute) },
                        onNavigateToCollections = { backStack.add(CollectionListRoute) },
                        onNavigateToAdvances = { backStack.add(AdvanceListRoute) },
                        onNavigateToCashBox = { backStack.add(CashBoxRoute()) },
                        onNavigateToExport = { backStack.add(ExportRoute) },
                        onNavigateToCollectors = { backStack.add(CollectorsRoute) },
                        onLogout = onLogout
                    )

                    is CollectorsRoute -> com.example.btmcontabilidad.ui.screens.CollectorsScreen(
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )

                    is CashBoxRoute -> CashBoxScreen(
                        initialBranchId = route.initialBranchId,
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )

                    is ExportRoute -> ExportScreen(
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )

                }
            }
        }
    }
}

package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.BranchStatus
import com.example.btmcontabilidad.domain.model.LedgerEntry
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class DashboardUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val totalReceivable: BigDecimal = BigDecimal.ZERO,
    val totalBranchCredit: BigDecimal = BigDecimal.ZERO,
    val netPosition: BigDecimal = BigDecimal.ZERO,
    val collectionsTotal: BigDecimal = BigDecimal.ZERO,
    val advancesTotal: BigDecimal = BigDecimal.ZERO,
    val moneyDeliveredTotal: BigDecimal = BigDecimal.ZERO,
    val pendingBranchesCount: Int = 0,
    val creditBranchesCount: Int = 0,
    val recentTransactions: List<LedgerEntry> = emptyList(),
    val cashBalance: BigDecimal = BigDecimal.ZERO,
    val isAdmin: Boolean = false
)

class DashboardViewModel(
    private val repositoryContainer: RepositoryContainer = RepositoryContainer.Instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun refresh() {
        loadDashboardData()
    }

    private var loadJob: kotlinx.coroutines.Job? = null

    private fun loadDashboardData() {
        if (loadJob?.isActive == true && _uiState.value.totalReceivable != BigDecimal.ZERO) {
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val isAdmin = repositoryContainer.isAdmin
            val hasExisting = _uiState.value.totalReceivable != BigDecimal.ZERO || _uiState.value.recentTransactions.isNotEmpty()
            if (!hasExisting) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null, isAdmin = isAdmin) }
            } else {
                _uiState.update { it.copy(isAdmin = isAdmin) }
            }
            try {
                val snapshotDeferred = async { repositoryContainer.dashboard() }
                val branchesDeferred = async {
                    repositoryContainer.branchRepository.getBranches().firstOrNull().orEmpty()
                }
                val snapshot = snapshotDeferred.await()
                FinancialCalculator.setCurrencyCode(snapshot.currencyCode)
                val activeBranches = branchesDeferred.await().filter { it.status == BranchStatus.ACTIVE }

                _uiState.value = DashboardUiState(
                    isLoading = false,
                    isAdmin = isAdmin,
                    totalReceivable = snapshot.receivableTotal,
                    totalBranchCredit = snapshot.branchCreditTotal,
                    netPosition = snapshot.netPosition,
                    collectionsTotal = snapshot.collectionsTotal,
                    advancesTotal = snapshot.advancesTotal,
                    moneyDeliveredTotal = snapshot.moneyDeliveredTotal,
                    pendingBranchesCount = activeBranches.count { it.currentBalance > BigDecimal.ZERO },
                    creditBranchesCount = activeBranches.count { it.currentBalance < BigDecimal.ZERO },
                    recentTransactions = snapshot.recentActivity,
                    cashBalance = snapshot.cashBalance
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error al cargar datos del panel principal"
                    )
                }
            }
        }
    }
}

package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.BranchStatus
import com.example.btmcontabilidad.domain.model.LedgerEntry
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
    val pendingBranchesCount: Int = 0,
    val creditBranchesCount: Int = 0,
    val recentTransactions: List<LedgerEntry> = emptyList()
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

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val snapshot = repositoryContainer.dashboard()
                FinancialCalculator.setCurrencyCode(snapshot.currencyCode)
                val activeBranches = repositoryContainer.branchRepository.getBranches().firstOrNull()
                    .orEmpty()
                    .filter { it.status == BranchStatus.ACTIVE }

                _uiState.value = DashboardUiState(
                    isLoading = false,
                    totalReceivable = snapshot.receivableTotal,
                    totalBranchCredit = snapshot.branchCreditTotal,
                    netPosition = snapshot.netPosition,
                    collectionsTotal = snapshot.collectionsTotal,
                    advancesTotal = snapshot.advancesTotal,
                    pendingBranchesCount = activeBranches.count { it.currentBalance > BigDecimal.ZERO },
                    creditBranchesCount = activeBranches.count { it.currentBalance < BigDecimal.ZERO },
                    recentTransactions = snapshot.recentActivity
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

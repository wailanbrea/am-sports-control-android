package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.BranchStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

enum class BranchFilterTab(val label: String) {
    TODAS("Todas"),
    POR_COBRAR("Por cobrar"),
    POR_ENVIAR("Por enviar"),
    SALDADAS("Saldadas"),
    INACTIVAS("Inactivas")
}

data class BranchesUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val branches: List<Branch> = emptyList(),
    val filteredBranches: List<Branch> = emptyList(),
    val searchQuery: String = "",
    val selectedTab: BranchFilterTab = BranchFilterTab.TODAS,
    val totalPorCobrar: BigDecimal = BigDecimal.ZERO,
    val totalPorEnviar: BigDecimal = BigDecimal.ZERO,
    val netBalance: BigDecimal = BigDecimal.ZERO,
    val countPorCobrar: Int = 0,
    val countPorEnviar: Int = 0,
    val countSaldadas: Int = 0,
    val countInactivas: Int = 0
)

class BranchesViewModel(
    private val repositoryContainer: RepositoryContainer = RepositoryContainer.Instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(BranchesUiState())
    val uiState: StateFlow<BranchesUiState> = _uiState.asStateFlow()

    init {
        loadBranches()
    }

    fun refresh() {
        loadBranches()
    }

    fun loadBranches() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repositoryContainer.branchRepository.getBranches().collect { branchList ->
                    var porCobrar = BigDecimal.ZERO
                    var porEnviar = BigDecimal.ZERO
                    var cntPorCobrar = 0
                    var cntPorEnviar = 0
                    var cntSaldadas = 0
                    var cntInactivas = 0

                    for (b in branchList) {
                        if (b.status == BranchStatus.INACTIVE) {
                            cntInactivas++
                            continue
                        }
                        val rounded = FinancialCalculator.roundMoney(b.currentBalance)
                        when {
                            rounded > BigDecimal.ZERO -> {
                                porCobrar = porCobrar.add(rounded)
                                cntPorCobrar++
                            }
                            rounded < BigDecimal.ZERO -> {
                                porEnviar = porEnviar.add(rounded.abs())
                                cntPorEnviar++
                            }
                            else -> {
                                cntSaldadas++
                            }
                        }
                    }

                    val net = porCobrar.subtract(porEnviar)

                    val currentState = _uiState.value.copy(
                        isLoading = false,
                        branches = branchList,
                        totalPorCobrar = FinancialCalculator.roundMoney(porCobrar),
                        totalPorEnviar = FinancialCalculator.roundMoney(porEnviar),
                        netBalance = FinancialCalculator.roundMoney(net),
                        countPorCobrar = cntPorCobrar,
                        countPorEnviar = cntPorEnviar,
                        countSaldadas = cntSaldadas,
                        countInactivas = cntInactivas
                    )

                    _uiState.value = applyFilterAndSearch(currentState)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error al cargar el listado de bancas"
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        val updatedState = _uiState.value.copy(searchQuery = query)
        _uiState.value = applyFilterAndSearch(updatedState)
    }

    fun setFilterTab(tab: BranchFilterTab) {
        val updatedState = _uiState.value.copy(selectedTab = tab)
        _uiState.value = applyFilterAndSearch(updatedState)
    }

    fun addBranch(branch: Branch) {
        viewModelScope.launch {
            try {
                repositoryContainer.branchRepository.addBranch(branch)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Error al agregar banca") }
            }
        }
    }

    private fun applyFilterAndSearch(state: BranchesUiState): BranchesUiState {
        val query = state.searchQuery.trim().lowercase()

        val filtered = state.branches.filter { branch ->
            val matchesQuery = query.isBlank() ||
                    branch.code.lowercase().contains(query) ||
                    branch.name.lowercase().contains(query) ||
                    branch.operatorName.lowercase().contains(query) ||
                    branch.route.lowercase().contains(query)

            val rounded = FinancialCalculator.roundMoney(branch.currentBalance)
            val matchesTab = when (state.selectedTab) {
                BranchFilterTab.TODAS -> true
                BranchFilterTab.POR_COBRAR -> branch.status == BranchStatus.ACTIVE && rounded > BigDecimal.ZERO
                BranchFilterTab.POR_ENVIAR -> branch.status == BranchStatus.ACTIVE && rounded < BigDecimal.ZERO
                BranchFilterTab.SALDADAS -> branch.status == BranchStatus.ACTIVE && rounded.compareTo(BigDecimal.ZERO) == 0
                BranchFilterTab.INACTIVAS -> branch.status == BranchStatus.INACTIVE
            }

            matchesQuery && matchesTab
        }

        return state.copy(filteredBranches = filtered)
    }
}

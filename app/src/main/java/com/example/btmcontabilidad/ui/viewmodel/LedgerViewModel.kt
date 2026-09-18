package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.LedgerEntry
import com.example.btmcontabilidad.domain.model.LedgerEntryType
import com.example.btmcontabilidad.domain.model.LedgerSourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

enum class LedgerTypeFilter(val label: String) {
    ALL("Todas"),
    COLLECTION("Cobros"),
    ADVANCE("Adelantos")
}

data class LedgerUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null,
    val entries: List<LedgerEntry> = emptyList(),
    val filteredEntries: List<LedgerEntry> = emptyList(),
    val selectedTypeFilter: LedgerTypeFilter = LedgerTypeFilter.ALL,
    val selectedBranchId: String? = null,
    val searchQuery: String = "",
    val totalDebits: BigDecimal = BigDecimal.ZERO,
    val totalCredits: BigDecimal = BigDecimal.ZERO
)

class LedgerViewModel(
    private val repositoryContainer: RepositoryContainer = RepositoryContainer.Instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(LedgerUiState())
    val uiState: StateFlow<LedgerUiState> = _uiState.asStateFlow()

    init {
        loadLedgerEntries()
    }

    fun refresh() {
        loadLedgerEntries()
    }

    fun loadLedgerEntries() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repositoryContainer.ledgerRepository.getLedgerEntries().collect { entryList ->
                    var debits = BigDecimal.ZERO
                    var credits = BigDecimal.ZERO

                    for (entry in entryList) {
                        if (entry.entryType == LedgerEntryType.DEBIT) {
                            debits = debits.add(entry.signedAmount.abs())
                        } else {
                            credits = credits.add(entry.signedAmount.abs())
                        }
                    }

                    val updatedState = _uiState.value.copy(
                        isLoading = false,
                        entries = entryList,
                        totalDebits = FinancialCalculator.roundMoney(debits),
                        totalCredits = FinancialCalculator.roundMoney(credits)
                    )

                    _uiState.value = applyFilterAndSearch(updatedState)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error al cargar las entradas del libro mayor"
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        val updatedState = _uiState.value.copy(searchQuery = query)
        _uiState.value = applyFilterAndSearch(updatedState)
    }

    fun setTypeFilter(typeFilter: LedgerTypeFilter) {
        val updatedState = _uiState.value.copy(selectedTypeFilter = typeFilter)
        _uiState.value = applyFilterAndSearch(updatedState)
    }

    fun setBranchFilter(branchId: String?) {
        val updatedState = _uiState.value.copy(selectedBranchId = branchId)
        _uiState.value = applyFilterAndSearch(updatedState)
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccessMessage = null) }
    }

    fun reverseTransaction(entryId: String, reason: String, reversedBy: String = "SISTEMA") {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null, actionSuccessMessage = null) }
            try {
                if (reason.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Debe especificar un motivo de anulación") }
                    return@launch
                }

                repositoryContainer.reverseLedgerEntry(entryId, reason, reversedBy)

                _uiState.update {
                    it.copy(
                        actionSuccessMessage = "Transacción $entryId anulada y reversada exitosamente"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Error al anular la transacción $entryId"
                    )
                }
            }
        }
    }

    private fun applyFilterAndSearch(state: LedgerUiState): LedgerUiState {
        val query = state.searchQuery.trim().lowercase()

        val filtered = state.entries.filter { entry ->
            val matchesBranch = state.selectedBranchId == null || entry.branchId == state.selectedBranchId

            val matchesType = when (state.selectedTypeFilter) {
                LedgerTypeFilter.ALL -> true
                LedgerTypeFilter.COLLECTION -> entry.sourceType == LedgerSourceType.COLLECTION
                LedgerTypeFilter.ADVANCE -> entry.sourceType == LedgerSourceType.ADVANCE
            }

            val matchesQuery = query.isBlank() ||
                    entry.id.lowercase().contains(query) ||
                    entry.description.lowercase().contains(query) ||
                    entry.branchId.lowercase().contains(query) ||
                    entry.createdBy.lowercase().contains(query)

            matchesBranch && matchesType && matchesQuery
        }

        return state.copy(filteredEntries = filtered)
    }
}

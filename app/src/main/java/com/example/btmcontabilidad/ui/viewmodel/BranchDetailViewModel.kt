package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.domain.model.Advance
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.Collection
import com.example.btmcontabilidad.domain.model.LedgerEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BranchDetailUiState(
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null,
    val branch: Branch? = null,
    val statementEntries: List<LedgerEntry> = emptyList(),
    val recentCollections: List<Collection> = emptyList(),
    val recentAdvances: List<Advance> = emptyList()
)

class BranchDetailViewModel(
    private val repositoryContainer: RepositoryContainer = RepositoryContainer.Instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(BranchDetailUiState())
    val uiState: StateFlow<BranchDetailUiState> = _uiState.asStateFlow()

    private var currentBranchId: String? = null

    fun loadBranch(branchId: String) {
        currentBranchId = branchId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                combine(
                    repositoryContainer.branchRepository.getBranchById(branchId),
                    repositoryContainer.ledgerRepository.getBranchLedger(branchId),
                    repositoryContainer.collectionRepository.getCollectionsForBranch(branchId),
                    repositoryContainer.advanceRepository.getAdvancesForBranch(branchId)
                ) { branch, ledgerList, collectionsList, advancesList ->
                    if (branch == null) {
                        BranchDetailUiState(
                            isLoading = false,
                            errorMessage = "Banca con ID $branchId no encontrada"
                        )
                    } else {
                        BranchDetailUiState(
                            isLoading = false,
                            errorMessage = null,
                            branch = branch,
                            statementEntries = ledgerList,
                            recentCollections = collectionsList,
                            recentAdvances = advancesList
                        )
                    }
                }.collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error al cargar detalle de la banca"
                    )
                }
            }
        }
    }

    fun refresh() {
        currentBranchId?.let { loadBranch(it) }
    }

    fun deleteBranch() {
        val branchId = currentBranchId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
            try {
                repositoryContainer.branchRepository.deleteBranch(branchId)
                _uiState.update { it.copy(isDeleting = false, isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isDeleting = false, errorMessage = e.message ?: "No se pudo eliminar la banca")
                }
            }
        }
    }
}

package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.repository.MoneyDeliveriesSummary
import com.example.btmcontabilidad.data.repository.MoneyDeliveryEntry
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.domain.model.Advance
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.Collection
import com.example.btmcontabilidad.domain.model.LedgerEntry
import com.example.btmcontabilidad.domain.model.WeeklySettlement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

enum class BranchHistoryTab(val label: String) {
    ALL("Todos"),
    COLLECTIONS("Cobros"),
    DELIVERIES("Entregas de dinero"),
    SETTLEMENTS("Cuadres")
}

data class BranchDetailUiState(
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null,
    val isReversing: Boolean = false,
    val branch: Branch? = null,
    val statementEntries: List<LedgerEntry> = emptyList(),
    val recentCollections: List<Collection> = emptyList(),
    val recentAdvances: List<Advance> = emptyList(),
    val weeklySettlements: List<WeeklySettlement> = emptyList(),
    val moneyDeliveries: List<MoneyDeliveryEntry> = emptyList(),
    val selectedTab: BranchHistoryTab = BranchHistoryTab.ALL,
    val isAdmin: Boolean = false,
    val collectors: List<com.example.btmcontabilidad.data.network.CollectorDto> = emptyList()
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
            val isAdmin = repositoryContainer.isAdmin
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isAdmin = isAdmin) }
            if (isAdmin) {
                launch {
                    try {
                        repositoryContainer.collectorRepository.getCollectors().collect { list ->
                            _uiState.update { it.copy(collectors = list) }
                        }
                    } catch (_: Exception) {}
                }
            }
            try {
                val branchFlow = repositoryContainer.branchRepository.getBranchById(branchId)
                    .catch { emit(null) }
                val ledgerFlow = repositoryContainer.ledgerRepository.getBranchLedger(branchId)
                    .catch { emit(emptyList()) }
                val collectionsFlow = repositoryContainer.collectionRepository.getCollectionsForBranch(branchId)
                    .catch { emit(emptyList()) }

                val advancesFlow = if (isAdmin) {
                    repositoryContainer.advanceRepository.getAdvancesForBranch(branchId).catch { emit(emptyList()) }
                } else {
                    flowOf(emptyList())
                }

                val settlementsFlow = if (isAdmin) {
                    repositoryContainer.weeklySettlementRepository.getForBranch(branchId).catch { emit(emptyList()) }
                } else {
                    flowOf(emptyList())
                }

                val deliveriesFlow = if (isAdmin) {
                    repositoryContainer.moneyDeliveryRepository.getDeliveries(branchId = branchId)
                        .catch { emit(MoneyDeliveriesSummary(emptyList(), BigDecimal.ZERO)) }
                } else {
                    flowOf(MoneyDeliveriesSummary(emptyList(), BigDecimal.ZERO))
                }

                combine(
                    combine(branchFlow, ledgerFlow, collectionsFlow) { branch, ledger, collections ->
                        Triple(branch, ledger, collections)
                    },
                    combine(advancesFlow, settlementsFlow, deliveriesFlow) { advances, settlements, deliverySummary ->
                        Triple(advances, settlements, deliverySummary.deliveries)
                    }
                ) { (branch, ledgerList, collectionsList), (advancesList, weeklySettlements, deliveriesList) ->
                    if (branch == null) {
                        _uiState.value.copy(
                            isLoading = false,
                            isAdmin = isAdmin,
                            errorMessage = "Banca con ID $branchId no encontrada"
                        )
                    } else {
                        val matchesBranch = { entryBranchId: String? ->
                            entryBranchId == branch.id || (branch.code.isNotBlank() && entryBranchId.equals(branch.code, ignoreCase = true))
                        }
                        val branchLedger = ledgerList.filter { matchesBranch(it.branchId) }
                        val branchCollections = collectionsList.filter { matchesBranch(it.branchId) }
                        val branchAdvances = advancesList.filter { matchesBranch(it.branchId) }
                        val branchSettlements = weeklySettlements.filter { matchesBranch(it.branchId) }
                        val branchDeliveries = deliveriesList.filter { matchesBranch(it.branchId.toString()) }

                        _uiState.value.copy(
                            isLoading = false,
                            isAdmin = isAdmin,
                            errorMessage = null,
                            branch = branch,
                            statementEntries = branchLedger,
                            recentCollections = branchCollections,
                            recentAdvances = branchAdvances,
                            weeklySettlements = branchSettlements,
                            moneyDeliveries = branchDeliveries
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

    fun setHistoryTab(tab: BranchHistoryTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun refresh() {
        currentBranchId?.let {
            repositoryContainer.branchRepository.invalidateCache()
            loadBranch(it)
        }
    }

    fun reverseTransaction(entryId: String, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReversing = true, errorMessage = null, actionSuccessMessage = null) }
            try {
                repositoryContainer.reverseLedgerEntry(entryId, reason)
                _uiState.update {
                    it.copy(
                        isReversing = false,
                        actionSuccessMessage = "Transacción revertida / anulada correctamente"
                    )
                }
                refresh()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isReversing = false,
                        errorMessage = e.message ?: "No se pudo anular la transacción"
                    )
                }
            }
        }
    }

    fun updateCollection(collection: Collection) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, actionSuccessMessage = null) }
            try {
                repositoryContainer.collectionRepository.updateCollection(collection)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        actionSuccessMessage = "Recibo de cobro actualizado correctamente"
                    )
                }
                refresh()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "No se pudo actualizar el recibo"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccessMessage = null) }
    }

    fun assignCollector(collectorUserId: Long?) {
        val branchId = currentBranchId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, actionSuccessMessage = null) }
            try {
                repositoryContainer.branchRepository.assignCollector(branchId, collectorUserId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        actionSuccessMessage = "Cobrador asignado exitosamente"
                    )
                }
                refresh()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "No se pudo asignar el cobrador"
                    )
                }
            }
        }
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

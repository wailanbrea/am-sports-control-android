package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.domain.model.Advance
import com.example.btmcontabilidad.domain.model.AdvanceStatus
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.Collection
import com.example.btmcontabilidad.domain.model.CollectionStatus
import com.example.btmcontabilidad.domain.model.LedgerEntry
import com.example.btmcontabilidad.domain.model.LedgerSourceType
import com.example.btmcontabilidad.domain.repository.AdvanceRepository
import com.example.btmcontabilidad.domain.repository.BranchRepository
import com.example.btmcontabilidad.domain.repository.CollectionRepository
import com.example.btmcontabilidad.domain.repository.LedgerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CollectionListItem(
    val collection: Collection,
    val branch: Branch?,
    val reversibleLedgerEntry: LedgerEntry?
)

data class AdvanceListItem(
    val advance: Advance,
    val branch: Branch?,
    val reversibleLedgerEntry: LedgerEntry?
)

enum class CollectionListFilter(val label: String) {
    ALL("Todos"),
    REGISTERED("Registrados"),
    VERIFIED("Verificados"),
    CANCELLED("Cancelados")
}

enum class AdvanceListFilter(val label: String) {
    ALL("Todos"),
    REGISTERED("Registrados"),
    APPROVED("Aprobados"),
    CANCELLED("Cancelados")
}

data class CollectionListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null,
    val items: List<CollectionListItem> = emptyList(),
    val filteredItems: List<CollectionListItem> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: CollectionListFilter = CollectionListFilter.ALL,
    val reversingCollectionId: String? = null
)

data class AdvanceListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null,
    val items: List<AdvanceListItem> = emptyList(),
    val filteredItems: List<AdvanceListItem> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: AdvanceListFilter = AdvanceListFilter.ALL,
    val reversingAdvanceId: String? = null
)

internal fun findReversibleLedgerEntry(
    entries: List<LedgerEntry>,
    sourceType: LedgerSourceType,
    sourceId: String
): LedgerEntry? {
    val originalEntry = entries.firstOrNull { entry ->
        entry.sourceType == sourceType &&
            entry.sourceId == sourceId &&
            entry.reversalOfEntryId == null &&
            !entry.description.startsWith("Anulación", ignoreCase = true)
    } ?: return null

    return originalEntry.takeUnless { original ->
        entries.any { it.reversalOfEntryId == original.id }
    }
}

class CollectionListViewModel(
    private val collectionRepository: CollectionRepository = RepositoryContainer.Instance.collectionRepository,
    private val branchRepository: BranchRepository = RepositoryContainer.Instance.branchRepository,
    private val ledgerRepository: LedgerRepository = RepositoryContainer.Instance.ledgerRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CollectionListUiState())
    val uiState: StateFlow<CollectionListUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val (collections, branches, ledgerEntries) = coroutineScope {
                    val collections = async { collectionRepository.getCollections().first() }
                    val branches = async { branchRepository.getBranches().first() }
                    val ledgerEntries = async { ledgerRepository.getLedgerEntries().first() }
                    Triple(collections.await(), branches.await(), ledgerEntries.await())
                }
                val branchById = branches.associateBy { it.id }
                val items = collections.map { collection ->
                    CollectionListItem(
                        collection = collection,
                        branch = branchById[collection.branchId],
                        reversibleLedgerEntry = findReversibleLedgerEntry(
                            ledgerEntries,
                            LedgerSourceType.COLLECTION,
                            collection.id
                        )
                    )
                }
                _uiState.update { state ->
                    applyCollectionFilter(state.copy(isLoading = false, items = items))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Error al cargar los cobros")
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { applyCollectionFilter(it.copy(searchQuery = query)) }
    }

    fun setFilter(filter: CollectionListFilter) {
        _uiState.update { applyCollectionFilter(it.copy(selectedFilter = filter)) }
    }

    fun reverseCollection(collectionId: String, reason: String) {
        if (reason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Debe especificar un motivo de reversión") }
            return
        }

        val item = _uiState.value.items.firstOrNull { it.collection.id == collectionId }
        val ledgerEntry = item?.reversibleLedgerEntry
        if (ledgerEntry == null) {
            _uiState.update {
                it.copy(errorMessage = "Este cobro no tiene un asiento activo que pueda reversarse")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    actionSuccessMessage = null,
                    reversingCollectionId = collectionId
                )
            }
            try {
                ledgerRepository.reverseEntry(ledgerEntry.id, reason.trim(), "SISTEMA")
                _uiState.update {
                    it.copy(
                        actionSuccessMessage = "Cobro $collectionId reversado correctamente",
                        reversingCollectionId = null
                    )
                }
                refresh()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "No se pudo reversar el cobro",
                        reversingCollectionId = null
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccessMessage = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(actionSuccessMessage = null) }
    }

    private fun applyCollectionFilter(state: CollectionListUiState): CollectionListUiState {
        val query = state.searchQuery.trim().lowercase()
        val filtered = state.items.filter { item ->
            val collection = item.collection
            val matchesFilter = when (state.selectedFilter) {
                CollectionListFilter.ALL -> true
                CollectionListFilter.REGISTERED -> collection.status == CollectionStatus.REGISTERED
                CollectionListFilter.VERIFIED -> collection.status == CollectionStatus.VERIFIED
                CollectionListFilter.CANCELLED -> collection.status == CollectionStatus.CANCELLED
            }
            val branchText = item.branch?.let { "${it.code} ${it.name}" }.orEmpty()
            val searchableText = listOf(
                collection.id,
                collection.branchId,
                collection.amount.toPlainString(),
                collection.businessDate,
                collection.paymentMethod.label,
                collection.reference.orEmpty(),
                collection.notes.orEmpty(),
                collection.status.label,
                branchText
            ).joinToString(" ").lowercase()
            matchesFilter && (query.isBlank() || searchableText.contains(query))
        }
        return state.copy(filteredItems = filtered)
    }
}

class AdvanceListViewModel(
    private val advanceRepository: AdvanceRepository = RepositoryContainer.Instance.advanceRepository,
    private val branchRepository: BranchRepository = RepositoryContainer.Instance.branchRepository,
    private val ledgerRepository: LedgerRepository = RepositoryContainer.Instance.ledgerRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdvanceListUiState())
    val uiState: StateFlow<AdvanceListUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val (advances, branches, ledgerEntries) = coroutineScope {
                    val advances = async { advanceRepository.getAdvances().first() }
                    val branches = async { branchRepository.getBranches().first() }
                    val ledgerEntries = async { ledgerRepository.getLedgerEntries().first() }
                    Triple(advances.await(), branches.await(), ledgerEntries.await())
                }
                val branchById = branches.associateBy { it.id }
                val items = advances.map { advance ->
                    AdvanceListItem(
                        advance = advance,
                        branch = branchById[advance.branchId],
                        reversibleLedgerEntry = findReversibleLedgerEntry(
                            ledgerEntries,
                            LedgerSourceType.ADVANCE,
                            advance.id
                        )
                    )
                }
                _uiState.update { state ->
                    applyAdvanceFilter(state.copy(isLoading = false, items = items))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Error al cargar los adelantos")
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { applyAdvanceFilter(it.copy(searchQuery = query)) }
    }

    fun setFilter(filter: AdvanceListFilter) {
        _uiState.update { applyAdvanceFilter(it.copy(selectedFilter = filter)) }
    }

    fun reverseAdvance(advanceId: String, reason: String) {
        if (reason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Debe especificar un motivo de reversión") }
            return
        }

        val item = _uiState.value.items.firstOrNull { it.advance.id == advanceId }
        val ledgerEntry = item?.reversibleLedgerEntry
        if (ledgerEntry == null) {
            _uiState.update {
                it.copy(errorMessage = "Este adelanto no tiene un asiento activo que pueda reversarse")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    actionSuccessMessage = null,
                    reversingAdvanceId = advanceId
                )
            }
            try {
                ledgerRepository.reverseEntry(ledgerEntry.id, reason.trim(), "SISTEMA")
                _uiState.update {
                    it.copy(
                        actionSuccessMessage = "Adelanto $advanceId reversado correctamente",
                        reversingAdvanceId = null
                    )
                }
                refresh()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "No se pudo reversar el adelanto",
                        reversingAdvanceId = null
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccessMessage = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(actionSuccessMessage = null) }
    }

    private fun applyAdvanceFilter(state: AdvanceListUiState): AdvanceListUiState {
        val query = state.searchQuery.trim().lowercase()
        val filtered = state.items.filter { item ->
            val advance = item.advance
            val matchesFilter = when (state.selectedFilter) {
                AdvanceListFilter.ALL -> true
                AdvanceListFilter.REGISTERED -> advance.status == AdvanceStatus.REGISTERED
                AdvanceListFilter.APPROVED -> advance.status == AdvanceStatus.APPROVED
                AdvanceListFilter.CANCELLED -> advance.status == AdvanceStatus.CANCELLED
            }
            val branchText = item.branch?.let { "${it.code} ${it.name}" }.orEmpty()
            val searchableText = listOf(
                advance.id,
                advance.branchId,
                advance.amount.toPlainString(),
                advance.businessDate,
                advance.reason,
                advance.notes.orEmpty(),
                advance.status.label,
                branchText
            ).joinToString(" ").lowercase()
            matchesFilter && (query.isBlank() || searchableText.contains(query))
        }
        return state.copy(filteredItems = filtered)
    }
}

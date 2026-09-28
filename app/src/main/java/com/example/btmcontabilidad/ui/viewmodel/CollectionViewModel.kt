package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.BranchStatus
import com.example.btmcontabilidad.domain.model.Collection
import com.example.btmcontabilidad.domain.model.CollectionStatus
import com.example.btmcontabilidad.domain.model.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CollectionUiState(
    val isLoadingBranches: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedBranchId: String = "",
    val selectedBranch: Branch? = null,
    val availableBranches: List<Branch> = emptyList(),
    val amountInput: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    val referenceInput: String = "",
    val businessDateInput: String = "",
    val notesInput: String = "",
    val previousBalance: BigDecimal = BigDecimal.ZERO,
    val thisWeekBalance: BigDecimal = BigDecimal.ZERO,
    val oldBalance: BigDecimal = BigDecimal.ZERO,
    val projectedNewBalance: BigDecimal = BigDecimal.ZERO,
    val isOvercollectionWarning: Boolean = false,
    val collectionSaved: Boolean = false,
    val savedCollection: Collection? = null,
    val lastPreviousBalance: BigDecimal = BigDecimal.ZERO,
    val lastNewBalance: BigDecimal = BigDecimal.ZERO,
    val collectors: List<com.example.btmcontabilidad.data.network.CollectorDto> = emptyList(),
    val selectedCollector: com.example.btmcontabilidad.data.network.CollectorDto? = null,
    val isAdmin: Boolean = false
)

class CollectionViewModel(
    private val repositoryContainer: RepositoryContainer = RepositoryContainer.Instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    init {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        _uiState.update { it.copy(businessDateInput = today) }
        loadBranches()
    }

    private var loadJob: kotlinx.coroutines.Job? = null

    fun loadBranches(preselectedBranchId: String? = null) {
        val hasExisting = _uiState.value.availableBranches.isNotEmpty()
        if (hasExisting && preselectedBranchId == null && loadJob?.isActive == true) {
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val isAdmin = repositoryContainer.isAdmin
            if (!hasExisting) {
                _uiState.update { it.copy(isLoadingBranches = true, errorMessage = null, isAdmin = isAdmin) }
            } else {
                _uiState.update { it.copy(isAdmin = isAdmin) }
            }
            try {
                val branches = try {
                    repositoryContainer.branchRepository.fetchBranches()
                } catch (_: Exception) {
                    repositoryContainer.branchRepository.getBranches().firstOrNull() ?: emptyList()
                }
                val activeBranches = branches.filter { it.status == BranchStatus.ACTIVE }

                val currentSelectedId = _uiState.value.selectedBranchId
                val defaultBranch = if (!preselectedBranchId.isNullOrBlank()) {
                    activeBranches.find { it.id == preselectedBranchId || it.code.equals(preselectedBranchId, ignoreCase = true) }
                        ?: activeBranches.firstOrNull()
                } else if (currentSelectedId.isNotBlank()) {
                    activeBranches.find { it.id == currentSelectedId || it.code.equals(currentSelectedId, ignoreCase = true) }
                        ?: activeBranches.firstOrNull()
                } else {
                    activeBranches.firstOrNull()
                }

                // Cargar cobradores solo si es Administrador
                val collectorsList = if (isAdmin) {
                    runCatching {
                        repositoryContainer.collectorRepository.getCollectors().firstOrNull()
                    }.getOrNull() ?: emptyList()
                } else {
                    emptyList()
                }

                _uiState.update { state ->
                    val newState = state.copy(
                        isLoadingBranches = false,
                        isAdmin = isAdmin,
                        availableBranches = activeBranches,
                        selectedBranchId = defaultBranch?.id ?: "",
                        selectedBranch = defaultBranch,
                        previousBalance = defaultBranch?.currentBalance ?: BigDecimal.ZERO,
                        collectors = collectorsList,
                        selectedCollector = collectorsList.firstOrNull()
                    )
                    calculateProjections(newState)
                }

                defaultBranch?.let { loadBranchBalanceBreakdown(it.id, it.currentBalance) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingBranches = false,
                        isAdmin = isAdmin,
                        errorMessage = e.message ?: "Error al cargar bancas"
                    )
                }
            }
        }
    }

    fun selectBranch(branchId: String) {
        val branch = _uiState.value.availableBranches.find { it.id == branchId }
        val updatedState = _uiState.value.copy(
            selectedBranchId = branchId,
            selectedBranch = branch,
            previousBalance = branch?.currentBalance ?: BigDecimal.ZERO
        )
        _uiState.value = calculateProjections(updatedState)
        branch?.let { loadBranchBalanceBreakdown(it.id, it.currentBalance) }
    }

    private fun loadBranchBalanceBreakdown(branchId: String, totalBalance: BigDecimal) {
        viewModelScope.launch {
            try {
                val entries = repositoryContainer.ledgerRepository.getBranchLedger(branchId).firstOrNull() ?: emptyList()
                val mondayStr = getMondayOfCurrentWeek()

                // Filtrar movimientos de esta semana (>= lunes actual)
                val thisWeekEntries = entries.filter { entry ->
                    entry.businessDate.substringBefore('T') >= mondayStr
                }

                val thisWeekDelta = thisWeekEntries.fold(BigDecimal.ZERO) { acc, entry ->
                    acc.add(entry.signedAmount)
                }

                val old = totalBalance.subtract(thisWeekDelta)

                _uiState.update { state ->
                    state.copy(
                        thisWeekBalance = FinancialCalculator.roundMoney(thisWeekDelta),
                        oldBalance = FinancialCalculator.roundMoney(old)
                    )
                }
            } catch (_: Exception) {
                // Si falla cálculo histórico detallado, mantener total
            }
        }
    }

    fun selectCollector(collector: com.example.btmcontabilidad.data.network.CollectorDto?) {
        _uiState.update { it.copy(selectedCollector = collector) }
    }

    fun updateAmount(amountText: String) {
        val updatedState = _uiState.value.copy(amountInput = amountText)
        _uiState.value = calculateProjections(updatedState)
    }

    fun updatePaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun updateReference(reference: String) {
        _uiState.update { it.copy(referenceInput = reference) }
    }

    fun updateBusinessDate(date: String) {
        _uiState.update { it.copy(businessDateInput = date) }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notesInput = notes) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun clearSavedCollection() {
        _uiState.update { it.copy(collectionSaved = false, savedCollection = null) }
    }

    fun submitCollection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            try {
                val state = _uiState.value
                val branchId = state.selectedBranchId
                if (branchId.isBlank()) {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = "Debe seleccionar una banca") }
                    return@launch
                }

                val amount = state.amountInput.replace(",", ".").toBigDecimalOrNull()
                if (amount == null || amount <= BigDecimal.ZERO) {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = "El monto del cobro debe ser mayor a cero") }
                    return@launch
                }

                val collection = Collection(
                    id = "col_${UUID.randomUUID().toString().take(8)}",
                    branchId = branchId,
                    amount = FinancialCalculator.roundMoney(amount),
                    paymentMethod = state.paymentMethod,
                    reference = state.referenceInput.trim().ifBlank { null },
                    businessDate = state.businessDateInput.ifBlank {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    },
                    notes = state.notesInput.trim().ifBlank { null },
                    status = CollectionStatus.VERIFIED
                )

                val prevBal = state.previousBalance
                val newBal = state.projectedNewBalance
                val collectorName = if (state.isAdmin) {
                    state.selectedCollector?.name ?: "SISTEMA"
                } else {
                    repositoryContainer.session.userName() ?: "COBRADOR"
                }

                val saved = repositoryContainer.registerCollection(collection, collectorName)

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        collectionSaved = true,
                        savedCollection = saved,
                        lastPreviousBalance = prevBal,
                        lastNewBalance = newBal,
                        amountInput = "",
                        referenceInput = "",
                        notesInput = "",
                        successMessage = "Cobro de ${FinancialCalculator.formatCurrency(saved.amount)} registrado exitosamente"
                    )
                }

                // Refresh branch balance in state
                selectBranch(branchId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Error al registrar el cobro"
                    )
                }
            }
        }
    }

    private fun calculateProjections(state: CollectionUiState): CollectionUiState {
        val prev = state.previousBalance
        val amount = state.amountInput.replace(",", ".").toBigDecimalOrNull() ?: BigDecimal.ZERO
        val projected = prev.subtract(amount)

        val isOvercollection = prev > BigDecimal.ZERO && amount > prev

        return state.copy(
            projectedNewBalance = FinancialCalculator.roundMoney(projected),
            isOvercollectionWarning = isOvercollection
        )
    }

    private companion object {
        fun getMondayOfCurrentWeek(): String {
            val cal = java.util.Calendar.getInstance()
            cal.firstDayOfWeek = java.util.Calendar.MONDAY
            cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        }
    }
}

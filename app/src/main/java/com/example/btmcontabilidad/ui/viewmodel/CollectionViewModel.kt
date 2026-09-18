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
    val projectedNewBalance: BigDecimal = BigDecimal.ZERO,
    val isOvercollectionWarning: Boolean = false,
    val collectionSaved: Boolean = false
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

    fun loadBranches(preselectedBranchId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBranches = true, errorMessage = null) }
            try {
                val branches = repositoryContainer.branchRepository.getBranches().firstOrNull() ?: emptyList()
                val activeBranches = branches.filter { it.status == BranchStatus.ACTIVE }

                val defaultBranch = if (!preselectedBranchId.isNullBraking()) {
                    activeBranches.find { it.id == preselectedBranchId } ?: activeBranches.firstOrNull()
                } else {
                    activeBranches.firstOrNull()
                }

                _uiState.update { state ->
                    val newState = state.copy(
                        isLoadingBranches = false,
                        availableBranches = activeBranches,
                        selectedBranchId = defaultBranch?.id ?: "",
                        selectedBranch = defaultBranch,
                        previousBalance = defaultBranch?.currentBalance ?: BigDecimal.ZERO
                    )
                    calculateProjections(newState)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingBranches = false,
                        errorMessage = e.message ?: "Error al cargar bancas"
                    )
                }
            }
        }
    }

    private fun String?.isNullBraking(): Boolean = this == null || this.isBlank()

    fun selectBranch(branchId: String) {
        val branch = _uiState.value.availableBranches.find { it.id == branchId }
        val updatedState = _uiState.value.copy(
            selectedBranchId = branchId,
            selectedBranch = branch,
            previousBalance = branch?.currentBalance ?: BigDecimal.ZERO
        )
        _uiState.value = calculateProjections(updatedState)
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

    fun submitCollection(createdBy: String = "SISTEMA") {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            try {
                val state = _uiState.value
                val branchId = state.selectedBranchId
                if (branchId.isBlank()) {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = "Debe seleccionar una banca") }
                    return@launch
                }

                val amount = state.amountInput.toBigDecimalOrNull()
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

                val saved = repositoryContainer.registerCollection(collection, createdBy)

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        collectionSaved = true,
                        successMessage = "Cobro de ${FinancialCalculator.formatCurrency(saved.amount)} registrado exitosamente para la banca"
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
        val amount = state.amountInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val projected = prev.subtract(amount)

        val isOvercollection = prev > BigDecimal.ZERO && amount > prev

        return state.copy(
            projectedNewBalance = FinancialCalculator.roundMoney(projected),
            isOvercollectionWarning = isOvercollection
        )
    }
}

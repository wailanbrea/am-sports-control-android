package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.Advance
import com.example.btmcontabilidad.domain.model.AdvanceStatus
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.BranchStatus
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

data class AdvanceUiState(
    val isLoadingBranches: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedBranchId: String = "",
    val selectedBranch: Branch? = null,
    val availableBranches: List<Branch> = emptyList(),
    val amountInput: String = "",
    val reasonInput: String = "",
    val businessDateInput: String = "",
    val notesInput: String = "",
    val previousBalance: BigDecimal = BigDecimal.ZERO,
    val projectedNewBalance: BigDecimal = BigDecimal.ZERO,
    val advanceSaved: Boolean = false
)

class AdvanceViewModel(
    private val repositoryContainer: RepositoryContainer = RepositoryContainer.Instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvanceUiState())
    val uiState: StateFlow<AdvanceUiState> = _uiState.asStateFlow()

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

                val defaultBranch = if (!preselectedBranchId.isNullOrBlank()) {
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

    fun updateReason(reason: String) {
        _uiState.update { it.copy(reasonInput = reason) }
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

    fun submitAdvance(createdBy: String = "SISTEMA") {
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
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = "El monto del adelanto debe ser mayor a cero") }
                    return@launch
                }

                val reason = state.reasonInput.trim()
                if (reason.isBlank()) {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = "Debe especificar el motivo del adelanto") }
                    return@launch
                }

                val advance = Advance(
                    id = "adv_${UUID.randomUUID().toString().take(8)}",
                    branchId = branchId,
                    amount = FinancialCalculator.roundMoney(amount),
                    reason = reason,
                    businessDate = state.businessDateInput.ifBlank {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    },
                    notes = state.notesInput.trim().ifBlank { null },
                    status = AdvanceStatus.APPROVED
                )

                val saved = repositoryContainer.registerAdvance(advance, createdBy)

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        advanceSaved = true,
                        successMessage = "Adelanto por pérdidas de ${FinancialCalculator.formatCurrency(saved.amount)} registrado exitosamente"
                    )
                }

                // Refresh branch balance in state
                selectBranch(branchId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Error al registrar el adelanto"
                    )
                }
            }
        }
    }

    private fun calculateProjections(state: AdvanceUiState): AdvanceUiState {
        val prev = state.previousBalance
        val amount = state.amountInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val projected = prev.subtract(amount)

        return state.copy(
            projectedNewBalance = FinancialCalculator.roundMoney(projected)
        )
    }
}

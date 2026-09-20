package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.repository.MoneyDeliveryEntry
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.BranchStatus
import kotlinx.coroutines.flow.firstOrNull

data class MoneyDeliveryUiState(
    val branchId: String = "",
    val selectedBranch: Branch? = null,
    val availableBranches: List<Branch> = emptyList(),
    val isLoadingBranches: Boolean = false,
    val manualResultId: Long? = null,
    val suggestedAmount: BigDecimal = BigDecimal.ZERO,
    val amountInput: String = "",
    val reasonInput: String = "Cubrir pérdida operativa / Premios",
    val businessDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val notesInput: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val deliverySaved: Boolean = false,
    val savedDelivery: MoneyDeliveryEntry? = null
) {
    val amountValue: BigDecimal?
        get() = amountInput.trim().replace(",", ".").toBigDecimalOrNull()
}

class RegisterMoneyDeliveryViewModel(
    private val repositoryContainer: RepositoryContainer = RepositoryContainer.Instance
) : ViewModel() {
    private val _uiState = MutableStateFlow(MoneyDeliveryUiState())
    val uiState: StateFlow<MoneyDeliveryUiState> = _uiState.asStateFlow()

    fun initialize(branchId: String, suggestedAmount: BigDecimal, manualResultId: Long? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBranches = true, errorMessage = null) }
            try {
                val branches = repositoryContainer.branchRepository.getBranches().firstOrNull() ?: emptyList()
                val activeBranches = branches.filter { it.status == BranchStatus.ACTIVE }

                val defaultBranch = if (branchId.isNotBlank()) {
                    activeBranches.find { it.id == branchId } ?: activeBranches.firstOrNull()
                } else {
                    activeBranches.firstOrNull()
                }

                val currentBalance = defaultBranch?.currentBalance ?: BigDecimal.ZERO
                val effectiveSuggested = if (suggestedAmount > BigDecimal.ZERO) {
                    suggestedAmount
                } else if (currentBalance < BigDecimal.ZERO) {
                    currentBalance.abs()
                } else {
                    BigDecimal.ZERO
                }

                val defaultReason = if (currentBalance < BigDecimal.ZERO || effectiveSuggested > BigDecimal.ZERO) {
                    "Cubrir premios / déficit de la banca"
                } else {
                    "Fondo para pago de premios"
                }

                _uiState.update { state ->
                    state.copy(
                        branchId = defaultBranch?.id.orEmpty(),
                        selectedBranch = defaultBranch,
                        availableBranches = activeBranches,
                        isLoadingBranches = false,
                        manualResultId = manualResultId,
                        suggestedAmount = effectiveSuggested,
                        amountInput = if (effectiveSuggested > BigDecimal.ZERO) effectiveSuggested.toPlainString() else state.amountInput,
                        reasonInput = defaultReason
                    )
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
        val branch = _uiState.value.availableBranches.find { it.id == branchId } ?: return
        val currentBalance = branch.currentBalance
        val effectiveSuggested = if (currentBalance < BigDecimal.ZERO) currentBalance.abs() else BigDecimal.ZERO
        val reason = if (currentBalance < BigDecimal.ZERO) "Cubrir premios / déficit de la banca" else "Fondo para pago de premios"

        _uiState.update {
            it.copy(
                branchId = branch.id,
                selectedBranch = branch,
                suggestedAmount = effectiveSuggested,
                amountInput = if (effectiveSuggested > BigDecimal.ZERO) effectiveSuggested.toPlainString() else "",
                reasonInput = reason
            )
        }
    }

    fun updateAmount(input: String) {
        _uiState.update { it.copy(amountInput = input, errorMessage = null) }
    }

    fun updateReason(reason: String) {
        _uiState.update { it.copy(reasonInput = reason) }
    }

    fun updateDate(date: String) {
        _uiState.update { it.copy(businessDate = date) }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notesInput = notes) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun submit() {
        val state = _uiState.value
        val amount = state.amountValue ?: run {
            _uiState.update { it.copy(errorMessage = "Ingrese un monto entregado válido") }
            return
        }

        if (amount <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "El monto entregado debe ser mayor que cero") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            runCatching {
                repositoryContainer.moneyDeliveryRepository.addDelivery(
                    branchId = state.branchId,
                    amount = amount,
                    suggestedAmount = state.suggestedAmount,
                    manualResultId = state.manualResultId,
                    businessDate = state.businessDate,
                    reason = state.reasonInput,
                    notes = state.notesInput.takeIf { it.isNotBlank() }
                )
            }.onSuccess { entry ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        savedDelivery = entry,
                        deliverySaved = true
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.localizedMessage ?: "Error al registrar dinero llevado"
                    )
                }
            }
        }
    }
}

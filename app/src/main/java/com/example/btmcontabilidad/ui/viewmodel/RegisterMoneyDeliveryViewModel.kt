package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.repository.MoneyDeliveryEntry
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.BranchStatus
import com.example.btmcontabilidad.domain.model.CashBoxEntity
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

data class MoneyDeliveryUiState(
    val branchId: String = "",
    val selectedBranch: Branch? = null,
    val availableBranches: List<Branch> = emptyList(),
    val isLoadingBranches: Boolean = false,
    val cashBoxes: List<CashBoxEntity> = emptyList(),
    val selectedCashBoxId: Long? = null,
    val manualResultId: Long? = null,
    val suggestedAmount: BigDecimal = BigDecimal.ZERO,
    val amountInput: String = "",
    val applyCommission: Boolean = false,
    val customCommissionRateInput: String = "",
    val reasonInput: String = "Cubrir pérdida operativa / Premios",
    val businessDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val notesInput: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val deliverySaved: Boolean = false,
    val savedDelivery: MoneyDeliveryEntry? = null,
    val previousBalance: BigDecimal = BigDecimal.ZERO,
    val newBalance: BigDecimal = BigDecimal.ZERO
) {
    val branchCommissionRate: BigDecimal
        get() = selectedBranch?.commissionRate ?: BigDecimal.ZERO

    val effectiveCommissionRate: BigDecimal
        get() {
            val custom = customCommissionRateInput.trim().replace(",", ".").toBigDecimalOrNull()
            return custom ?: branchCommissionRate
        }

    val grossAmountValue: BigDecimal?
        get() = amountInput.trim().replace(",", ".").toBigDecimalOrNull()

    val commissionAmount: BigDecimal
        get() {
            if (!applyCommission) return BigDecimal.ZERO
            val gross = grossAmountValue ?: return BigDecimal.ZERO
            val rate = effectiveCommissionRate
            if (rate <= BigDecimal.ZERO) return BigDecimal.ZERO
            return gross.multiply(rate).divide(BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP)
        }

    val netDeliveredAmount: BigDecimal
        get() {
            val gross = grossAmountValue ?: return BigDecimal.ZERO
            return if (applyCommission) {
                gross.subtract(commissionAmount).max(BigDecimal.ZERO)
            } else {
                gross
            }
        }

    val amountValue: BigDecimal?
        get() = netDeliveredAmount.takeIf { grossAmountValue != null }

    val selectedCashBox: CashBoxEntity?
        get() = cashBoxes.find { it.id == selectedCashBoxId }
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
                val branches = try {
                    repositoryContainer.branchRepository.fetchBranches()
                } catch (_: Exception) {
                    repositoryContainer.branchRepository.getBranches().firstOrNull() ?: emptyList()
                }
                val activeBranches = branches.filter { it.status == BranchStatus.ACTIVE }

                val boxes = repositoryContainer.cashBoxRepository.getCashBoxes()
                val defaultBox = boxes.firstOrNull { it.isDefault } ?: boxes.firstOrNull()

                val defaultBranch = if (branchId.isNotBlank()) {
                    activeBranches.find { it.id == branchId || it.code.equals(branchId, ignoreCase = true) } ?: activeBranches.firstOrNull()
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

                val hasCommission = defaultBranch?.let { it.commissionRate > BigDecimal.ZERO } ?: false
                _uiState.update { state ->
                    state.copy(
                        branchId = defaultBranch?.id.orEmpty(),
                        selectedBranch = defaultBranch,
                        availableBranches = activeBranches,
                        isLoadingBranches = false,
                        cashBoxes = boxes,
                        selectedCashBoxId = defaultBox?.id,
                        manualResultId = manualResultId,
                        suggestedAmount = effectiveSuggested,
                        amountInput = if (effectiveSuggested > BigDecimal.ZERO) effectiveSuggested.toPlainString() else state.amountInput,
                        applyCommission = false,
                        customCommissionRateInput = if (hasCommission) defaultBranch?.commissionRate?.stripTrailingZeros()?.toPlainString().orEmpty() else "",
                        reasonInput = defaultReason
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingBranches = false,
                        errorMessage = e.message ?: "Error al cargar datos"
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
        val hasCommission = branch.commissionRate > BigDecimal.ZERO

        _uiState.update {
            it.copy(
                branchId = branch.id,
                selectedBranch = branch,
                suggestedAmount = effectiveSuggested,
                amountInput = if (effectiveSuggested > BigDecimal.ZERO) effectiveSuggested.toPlainString() else "",
                applyCommission = false,
                customCommissionRateInput = if (hasCommission) branch.commissionRate.stripTrailingZeros().toPlainString() else "",
                reasonInput = reason
            )
        }
    }

    fun toggleApplyCommission(applied: Boolean) {
        _uiState.update { it.copy(applyCommission = applied) }
    }

    fun updateCustomCommissionRate(rate: String) {
        _uiState.update { it.copy(customCommissionRateInput = rate) }
    }

    fun selectCashBox(boxId: Long) {
        _uiState.update { it.copy(selectedCashBoxId = boxId) }
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
        val grossAmount = state.grossAmountValue ?: run {
            _uiState.update { it.copy(errorMessage = "Ingrese un monto válido") }
            return
        }

        if (grossAmount <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "El monto debe ser mayor que cero") }
            return
        }

        val netAmount = state.netDeliveredAmount
        val commissionAmount = state.commissionAmount
        val commissionRate = if (state.applyCommission) state.effectiveCommissionRate else BigDecimal.ZERO

        val finalNotes = buildString {
            if (!state.notesInput.isNullOrBlank()) {
                append(state.notesInput)
            }
            if (state.applyCommission && commissionAmount > BigDecimal.ZERO) {
                if (isNotBlank()) append(" | ")
                append("Ganancia: $grossAmount - Comisión (${commissionRate.stripTrailingZeros().toPlainString()}%): -$commissionAmount - Neto entregado: $netAmount")
            }
        }.ifBlank { null }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            runCatching {
                repositoryContainer.moneyDeliveryRepository.addDelivery(
                    branchId = state.branchId,
                    amount = netAmount,
                    grossAmount = grossAmount,
                    commissionRate = commissionRate,
                    commissionAmount = commissionAmount,
                    suggestedAmount = state.suggestedAmount,
                    manualResultId = state.manualResultId,
                    businessDate = state.businessDate,
                    reason = state.reasonInput,
                    notes = finalNotes,
                    cashBoxId = state.selectedCashBoxId
                )
            }.onSuccess { entry ->
                val prevBal = state.selectedBranch?.currentBalance ?: BigDecimal.ZERO
                val newBal = prevBal.add(grossAmount)
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        savedDelivery = entry,
                        previousBalance = prevBal,
                        newBalance = newBal,
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

    fun clearSavedDelivery() {
        _uiState.update { it.copy(deliverySaved = false, savedDelivery = null) }
    }
}

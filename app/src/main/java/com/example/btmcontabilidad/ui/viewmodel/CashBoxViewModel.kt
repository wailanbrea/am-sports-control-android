package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.BranchStatus
import com.example.btmcontabilidad.domain.model.CashBox
import com.example.btmcontabilidad.domain.model.CashBoxEntity
import com.example.btmcontabilidad.domain.model.CashMovement
import com.example.btmcontabilidad.domain.model.CashMovementType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CashBoxUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isCreatingBox: Boolean = false,
    val showCreateBoxDialog: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val cashBoxes: List<CashBoxEntity> = emptyList(),
    val selectedCashBoxId: Long? = null,
    val cashBox: CashBox? = null,
    val availableBranches: List<Branch> = emptyList(),
    val selectedType: CashMovementType = CashMovementType.INCOME,
    val selectedBranchId: String = "",
    val amountInput: String = "",
    val businessDateInput: String = "",
    val reasonInput: String = "",
    val referenceInput: String = "",
    val notesInput: String = "",
    // Campos para creación de nueva caja chica
    val newBoxName: String = "",
    val newBoxInitialBalance: String = "",
    val newBoxDescription: String = "",
    val newBoxIsDefault: Boolean = false
)

class CashBoxViewModel(
    private val repositoryContainer: RepositoryContainer = RepositoryContainer.Instance
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        CashBoxUiState(
            businessDateInput = today()
        )
    )
    val uiState: StateFlow<CashBoxUiState> = _uiState.asStateFlow()

    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        refresh()
    }

    fun refresh(targetBoxId: Long? = null) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (_uiState.value.cashBox == null) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(errorMessage = null) }
            }
            try {
                val branches = try {
                    repositoryContainer.branchRepository.fetchBranches()
                } catch (_: Exception) {
                    repositoryContainer.branchRepository.getBranches().first()
                }.filter { it.status == BranchStatus.ACTIVE }
                val boxes = repositoryContainer.cashBoxRepository.getCashBoxes()
                val currentSelectedId = targetBoxId
                    ?: _uiState.value.selectedCashBoxId
                    ?: boxes.firstOrNull { it.isDefault }?.id
                    ?: boxes.firstOrNull()?.id

                val cashBox = repositoryContainer.cashBoxRepository.getCashBox(currentSelectedId).first()
                FinancialCalculator.setCurrencyCode(cashBox.currencyCode)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cashBoxes = boxes,
                        selectedCashBoxId = currentSelectedId,
                        cashBox = cashBox,
                        availableBranches = branches
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "No se pudo cargar la caja"
                    )
                }
            }
        }
    }

    fun selectCashBox(boxId: Long) {
        if (_uiState.value.selectedCashBoxId == boxId) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedCashBoxId = boxId, errorMessage = null) }
            try {
                val cashBox = repositoryContainer.cashBoxRepository.getCashBox(boxId).first()
                FinancialCalculator.setCurrencyCode(cashBox.currencyCode)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cashBox = cashBox
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Error al cambiar de caja chica"
                    )
                }
            }
        }
    }

    fun openCreateBoxDialog() {
        _uiState.update {
            it.copy(
                showCreateBoxDialog = true,
                newBoxName = "",
                newBoxInitialBalance = "",
                newBoxDescription = "",
                newBoxIsDefault = false,
                errorMessage = null
            )
        }
    }

    fun closeCreateBoxDialog() {
        _uiState.update { it.copy(showCreateBoxDialog = false) }
    }

    fun updateNewBoxName(value: String) {
        _uiState.update { it.copy(newBoxName = value, errorMessage = null) }
    }

    fun updateNewBoxInitialBalance(value: String) {
        _uiState.update { it.copy(newBoxInitialBalance = value, errorMessage = null) }
    }

    fun updateNewBoxDescription(value: String) {
        _uiState.update { it.copy(newBoxDescription = value, errorMessage = null) }
    }

    fun updateNewBoxIsDefault(value: Boolean) {
        _uiState.update { it.copy(newBoxIsDefault = value) }
    }

    fun createCashBox() {
        val state = _uiState.value
        val name = state.newBoxName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El nombre de la caja chica es obligatorio") }
            return
        }

        val initialBalance = if (state.newBoxInitialBalance.isNotBlank()) {
            state.newBoxInitialBalance.trim().replace(",", ".").toBigDecimalOrNull()
        } else {
            BigDecimal.ZERO
        }

        if (initialBalance == null || initialBalance < BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "El saldo inicial no es un monto válido") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingBox = true, errorMessage = null) }
            try {
                val createdBox = repositoryContainer.cashBoxRepository.createCashBox(
                    name = name,
                    initialBalance = initialBalance,
                    description = state.newBoxDescription.trim().ifBlank { null },
                    isDefault = state.newBoxIsDefault
                )
                _uiState.update {
                    it.copy(
                        isCreatingBox = false,
                        showCreateBoxDialog = false,
                        newBoxName = "",
                        newBoxInitialBalance = "",
                        newBoxDescription = "",
                        newBoxIsDefault = false,
                        successMessage = "Caja chica '${createdBox.name}' creada exitosamente"
                    )
                }
                refresh(targetBoxId = createdBox.id)
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingBox = false,
                        errorMessage = exception.message ?: "Error al crear la caja chica"
                    )
                }
            }
        }
    }

    fun selectType(type: CashMovementType) {
        _uiState.update { it.copy(selectedType = type, errorMessage = null) }
    }

    fun selectBranch(branchId: String) {
        _uiState.update { it.copy(selectedBranchId = branchId, errorMessage = null) }
    }

    fun prepareBranchTransfer(branchId: String) {
        _uiState.update {
            it.copy(
                selectedType = CashMovementType.BRANCH_TRANSFER,
                selectedBranchId = branchId,
                reasonInput = "Entrega para compensar balance negativo",
                errorMessage = null
            )
        }
    }

    fun updateAmount(value: String) {
        _uiState.update { it.copy(amountInput = value, errorMessage = null) }
    }

    fun updateBusinessDate(value: String) {
        _uiState.update { it.copy(businessDateInput = value, errorMessage = null) }
    }

    fun updateReason(value: String) {
        _uiState.update { it.copy(reasonInput = value, errorMessage = null) }
    }

    fun updateReference(value: String) {
        _uiState.update { it.copy(referenceInput = value, errorMessage = null) }
    }

    fun updateNotes(value: String) {
        _uiState.update { it.copy(notesInput = value, errorMessage = null) }
    }

    fun submit() {
        viewModelScope.launch {
            val state = _uiState.value
            val amount = state.amountInput.trim().replace(",", ".").toBigDecimalOrNull()
            val validationError = when {
                amount == null || amount <= BigDecimal.ZERO -> "El monto debe ser mayor a cero"
                !isValidBusinessDate(state.businessDateInput) -> "La fecha debe tener el formato yyyy-MM-dd"
                state.reasonInput.trim().isBlank() -> "Debe indicar el motivo"
                state.selectedType == CashMovementType.BRANCH_TRANSFER && state.selectedBranchId.isBlank() ->
                    "Debe seleccionar la banca de destino"
                else -> null
            }
            if (validationError != null) {
                _uiState.update { it.copy(errorMessage = validationError, successMessage = null) }
                return@launch
            }

            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            try {
                val movement = CashMovement(
                    id = "",
                    type = state.selectedType,
                    amount = FinancialCalculator.roundMoney(amount!!),
                    businessDate = state.businessDateInput.trim(),
                    reason = state.reasonInput.trim(),
                    branchId = state.selectedBranchId.takeIf {
                        state.selectedType == CashMovementType.BRANCH_TRANSFER
                    },
                    reference = state.referenceInput.trim().ifBlank { null },
                    notes = state.notesInput.trim().ifBlank { null }
                )

                val targetBoxId = state.selectedCashBoxId
                when (movement.type) {
                    CashMovementType.INCOME -> repositoryContainer.cashBoxRepository.addIncome(movement, targetBoxId)
                    CashMovementType.EXPENSE -> repositoryContainer.cashBoxRepository.addExpense(movement, targetBoxId)
                    CashMovementType.BRANCH_TRANSFER -> repositoryContainer.cashBoxRepository.transferToBranch(movement, targetBoxId)
                }

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        amountInput = "",
                        reasonInput = "",
                        referenceInput = "",
                        notesInput = "",
                        successMessage = "${movement.type.label} registrada correctamente"
                    )
                }
                refresh(targetBoxId)
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = exception.message ?: "No se pudo registrar el movimiento"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private companion object {
        fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        fun isValidBusinessDate(value: String): Boolean = runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
                .parse(value.trim())
        }.getOrNull() != null
    }
}

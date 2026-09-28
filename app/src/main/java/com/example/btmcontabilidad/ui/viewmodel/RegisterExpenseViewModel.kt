package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.BranchStatus
import com.example.btmcontabilidad.domain.model.CashBoxEntity
import com.example.btmcontabilidad.domain.model.CashMovement
import com.example.btmcontabilidad.domain.model.CashMovementType
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

data class ExpenseCategory(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val defaultReason: String
)

data class RegisterExpenseUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val expenseSaved: Boolean = false,
    val savedExpense: CashMovement? = null,
    val cashBoxes: List<CashBoxEntity> = emptyList(),
    val selectedCashBoxId: Long? = null,
    val availableBranches: List<Branch> = emptyList(),
    val selectedBranchId: String? = null,
    val amountInput: String = "",
    val businessDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val selectedCategory: ExpenseCategory = defaultCategories.first(),
    val reasonInput: String = "",
    val referenceInput: String = "",
    val notesInput: String = ""
) {
    val amountValue: BigDecimal?
        get() = amountInput.trim().replace(",", ".").toBigDecimalOrNull()

    val selectedCashBox: CashBoxEntity?
        get() = cashBoxes.find { it.id == selectedCashBoxId }

    val selectedBranch: Branch?
        get() = availableBranches.find { it.id == selectedBranchId }

    companion object {
        val defaultCategories = listOf(
            ExpenseCategory("comision", "Comisión a Banca", "🤝", "Pago de comisión a banca"),
            ExpenseCategory("luz", "Luz / Electricidad", "💡", "Pago de servicio de electricidad"),
            ExpenseCategory("papel", "Rollos / Papel", "🖨️", "Compra de papel térmico / rollos"),
            ExpenseCategory("gasolina", "Gasolina / Pasaje", "⛽", "Combustible / Gastos de transporte"),
            ExpenseCategory("alquiler", "Alquiler Local", "🏢", "Pago de alquiler de local"),
            ExpenseCategory("sueldo", "Sueldo / Dieta", "💼", "Pago de nómina / dieta / incentivo"),
            ExpenseCategory("mantenimiento", "Mantenimiento", "🔧", "Reparación o mantenimiento técnico"),
            ExpenseCategory("comida", "Refrigerio / Comida", "☕", "Gastos de cafetería / almuerzo"),
            ExpenseCategory("otros", "Otros Gastos", "📝", "Gasto operativo vario")
        )
    }
}

class RegisterExpenseViewModel(
    private val repositoryContainer: RepositoryContainer = RepositoryContainer.Instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterExpenseUiState())
    val uiState: StateFlow<RegisterExpenseUiState> = _uiState.asStateFlow()

    fun initialize(initialBranchId: String? = null, initialCategoryId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val boxes = repositoryContainer.cashBoxRepository.getCashBoxes()
                val defaultBox = boxes.firstOrNull { it.isDefault } ?: boxes.firstOrNull()

                val branches = try {
                    repositoryContainer.branchRepository.fetchBranches()
                } catch (_: Exception) {
                    repositoryContainer.branchRepository.getBranches().firstOrNull() ?: emptyList()
                }
                val activeBranches = branches.filter { it.status == BranchStatus.ACTIVE }

                val matchedBranch = if (!initialBranchId.isNullOrBlank()) {
                    activeBranches.find { it.id == initialBranchId || it.code.equals(initialBranchId, ignoreCase = true) }
                } else null

                val targetCategory = if (!initialCategoryId.isNullOrBlank()) {
                    RegisterExpenseUiState.defaultCategories.find { it.id == initialCategoryId }
                        ?: RegisterExpenseUiState.defaultCategories.first()
                } else {
                    RegisterExpenseUiState.defaultCategories.first()
                }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        cashBoxes = boxes,
                        selectedCashBoxId = defaultBox?.id,
                        availableBranches = activeBranches,
                        selectedBranchId = matchedBranch?.id,
                        selectedCategory = targetCategory,
                        reasonInput = targetCategory.defaultReason
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error al cargar datos para el gasto"
                    )
                }
            }
        }
    }

    fun selectCategory(category: ExpenseCategory) {
        _uiState.update { state ->
            val updateReason = state.reasonInput.isBlank() ||
                    RegisterExpenseUiState.defaultCategories.any { it.defaultReason == state.reasonInput }
            state.copy(
                selectedCategory = category,
                reasonInput = if (updateReason) category.defaultReason else state.reasonInput,
                errorMessage = null
            )
        }
    }

    fun selectCashBox(boxId: Long) {
        _uiState.update { it.copy(selectedCashBoxId = boxId, errorMessage = null) }
    }

    fun selectBranch(branchId: String?) {
        _uiState.update { it.copy(selectedBranchId = branchId, errorMessage = null) }
    }

    fun updateAmount(value: String) {
        _uiState.update { it.copy(amountInput = value, errorMessage = null) }
    }

    fun updateBusinessDate(value: String) {
        _uiState.update { it.copy(businessDate = value, errorMessage = null) }
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

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun submitExpense() {
        val state = _uiState.value
        val amount = state.amountValue

        if (amount == null || amount <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "Ingrese un monto válido mayor a 0") }
            return
        }

        if (state.reasonInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Especifique el concepto o motivo del gasto") }
            return
        }

        if (state.selectedCategory.id == "comision" && state.selectedBranchId == null) {
            _uiState.update { it.copy(errorMessage = "Para registrar un gasto de comisión, debe seleccionar la banca receptora") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val fullReason = "[${state.selectedCategory.name}] ${state.reasonInput.trim()}"
                val movement = CashMovement(
                    id = "",
                    type = CashMovementType.EXPENSE,
                    amount = FinancialCalculator.roundMoney(amount),
                    businessDate = state.businessDate.trim(),
                    reason = fullReason,
                    branchId = state.selectedBranchId,
                    reference = state.referenceInput.trim().ifBlank { null },
                    notes = state.notesInput.trim().ifBlank { null }
                )

                val saved = repositoryContainer.cashBoxRepository.addExpense(
                    movement = movement,
                    cashBoxId = state.selectedCashBoxId
                )

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        expenseSaved = true,
                        savedExpense = saved
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "No se pudo registrar el gasto"
                    )
                }
            }
        }
    }
}

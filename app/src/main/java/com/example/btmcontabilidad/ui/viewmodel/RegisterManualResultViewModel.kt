package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.repository.ManualResultEntry
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ManualResultUiState(
    val branchId: String = "",
    val previousBalance: BigDecimal = BigDecimal.ZERO,
    val amountInput: String = "",
    val businessDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val notesInput: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val resultSaved: Boolean = false,
    val savedResult: ManualResultEntry? = null,
    val showLossPrompt: Boolean = false
) {
    val amountValue: BigDecimal?
        get() = amountInput.trim().replace(",", ".").toBigDecimalOrNull()

    val isNegative: Boolean
        get() = (amountValue ?: BigDecimal.ZERO) < BigDecimal.ZERO

    val isPositive: Boolean
        get() = (amountValue ?: BigDecimal.ZERO) > BigDecimal.ZERO

    val projectedBalance: BigDecimal
        get() = previousBalance + (amountValue ?: BigDecimal.ZERO)
}

class RegisterManualResultViewModel(
    private val repositoryContainer: RepositoryContainer = RepositoryContainer.Instance
) : ViewModel() {
    private val _uiState = MutableStateFlow(ManualResultUiState())
    val uiState: StateFlow<ManualResultUiState> = _uiState.asStateFlow()

    fun initialize(branchId: String, previousBalance: BigDecimal) {
        if (_uiState.value.branchId == branchId) return
        _uiState.value = ManualResultUiState(
            branchId = branchId,
            previousBalance = previousBalance
        )
    }

    fun updateAmount(input: String) {
        _uiState.update { it.copy(amountInput = input, errorMessage = null) }
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
            _uiState.update { it.copy(errorMessage = "Ingrese un monto válido (positivo, negativo o cero)") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            runCatching {
                repositoryContainer.manualResultRepository.addResult(
                    branchId = state.branchId,
                    amount = amount,
                    businessDate = state.businessDate,
                    notes = state.notesInput.takeIf { it.isNotBlank() }
                )
            }.onSuccess { entry ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        savedResult = entry,
                        resultSaved = !entry.requiresMoneyDelivery,
                        showLossPrompt = entry.requiresMoneyDelivery
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.localizedMessage ?: "Error al registrar resultado"
                    )
                }
            }
        }
    }
}

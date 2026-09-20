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

data class MoneyDeliveryUiState(
    val branchId: String = "",
    val manualResultId: Long? = null,
    val suggestedAmount: BigDecimal = BigDecimal.ZERO,
    val amountInput: String = "",
    val reasonInput: String = "Cubrir pérdida operativa",
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
        if (_uiState.value.branchId == branchId && _uiState.value.amountInput.isNotEmpty()) return
        val suggestedStr = if (suggestedAmount > BigDecimal.ZERO) suggestedAmount.toPlainString() else ""
        _uiState.value = MoneyDeliveryUiState(
            branchId = branchId,
            manualResultId = manualResultId,
            suggestedAmount = suggestedAmount,
            amountInput = suggestedStr
        )
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

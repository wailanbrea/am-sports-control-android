package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.WeeklySettlement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class WeeklySettlementUiState(
    val branchId: String = "",
    val weekStart: String = "",
    val weekEnd: String = "",
    val salesInput: String = "",
    val prizesInput: String = "",
    val commissionRateInput: String = "20",
    val commissionAmountInput: String = "",
    val cashDeliveredInput: String = "",
    val notesInput: String = "",
    val previousBalance: BigDecimal = BigDecimal.ZERO,
    val commissionAmount: BigDecimal = BigDecimal.ZERO,
    val gameResult: BigDecimal = BigDecimal.ZERO,
    val operatingResult: BigDecimal = BigDecimal.ZERO,
    val lossAbsorbedAmount: BigDecimal = BigDecimal.ZERO,
    val weeklyBalance: BigDecimal = BigDecimal.ZERO,
    val projectedBalance: BigDecimal = BigDecimal.ZERO,
    val isLoss: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val settlementSaved: Boolean = false
)

class WeeklySettlementViewModel(
    private val repositoryContainer: RepositoryContainer = RepositoryContainer.Instance
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeeklySettlementUiState())
    val uiState: StateFlow<WeeklySettlementUiState> = _uiState.asStateFlow()

    fun initialize(branchId: String, previousBalance: BigDecimal) {
        if (_uiState.value.branchId == branchId) return
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val start = format(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, 6)
        val end = format(calendar.time)
        _uiState.value = WeeklySettlementUiState(
            branchId = branchId,
            weekStart = start,
            weekEnd = end,
            previousBalance = previousBalance,
            projectedBalance = previousBalance
        )
        viewModelScope.launch {
            val branch = repositoryContainer.branchRepository.getBranchById(branchId).firstOrNull()
            if (branch != null && branch.commissionRate > BigDecimal.ZERO) {
                val rateStr = branch.commissionRate.stripTrailingZeros().toPlainString()
                updateAmounts { it.copy(commissionRateInput = rateStr) }
            }
        }
    }

    fun updateWeekStart(value: String) = _uiState.update { it.copy(weekStart = value) }
    fun updateWeekEnd(value: String) = _uiState.update { it.copy(weekEnd = value) }
    fun updateSales(value: String) = updateAmounts { it.copy(salesInput = value) }
    fun updatePrizes(value: String) = updateAmounts { it.copy(prizesInput = value) }
    fun updateCommissionRate(value: String) = updateAmounts { it.copy(commissionRateInput = value) }
    fun updateCommissionAmount(value: String) = updateAmounts { it.copy(commissionAmountInput = value) }
    fun updateCashDelivered(value: String) = updateAmounts { it.copy(cashDeliveredInput = value) }
    fun updateNotes(value: String) = _uiState.update { it.copy(notesInput = value) }
    fun clearMessages() = _uiState.update { it.copy(errorMessage = null) }

    fun submit() {
        viewModelScope.launch {
            val state = _uiState.value
            val sales = state.salesInput.toAmountOrZero()
            val prizes = state.prizesInput.toAmountOrZero()
            val commissionRate = state.commissionRateInput.toAmountOrZero()
            val cashDelivered = state.cashDeliveredInput.toAmountOrZero()
            if (state.branchId.isBlank()) return@launch setError("No se identificó la banca")
            if (state.weekStart.isBlank() || state.weekEnd.isBlank()) return@launch setError("Debe indicar el período semanal")
            if (sales == BigDecimal.ZERO && prizes == BigDecimal.ZERO && cashDelivered == BigDecimal.ZERO) {
                return@launch setError("Debe ingresar al menos un monto mayor que cero")
            }
            if (commissionRate > BigDecimal("100")) return@launch setError("La comisión no puede superar el 100%")

            val calculatedCommission = sales.multiply(commissionRate)
                .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
            val commissionAmount = if (state.commissionAmountInput.isNotBlank()) {
                state.commissionAmountInput.toAmountOrZero()
            } else {
                calculatedCommission
            }

            // Resultado del juego (hoja de MegaLottery): Ventas - Premios - Comisión
            val gameResult = sales.subtract(prizes).subtract(commissionAmount)
            // Neto considerando dinero llevado previamente para premios
            val netWithCash = gameResult.add(cashDelivered)
            val isLoss = netWithCash < BigDecimal.ZERO

            val lossAbsorbedAmount = if (isLoss) netWithCash.abs() else BigDecimal.ZERO
            val weeklyBalance = if (isLoss) BigDecimal.ZERO else netWithCash
            val settlementType = if (isLoss) "loss_absorbed" else "gain"
            val finalBalanceAfter = state.previousBalance.add(weeklyBalance)

            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                repositoryContainer.registerWeeklySettlement(
                    WeeklySettlement(
                        id = "weekly_${UUID.randomUUID()}",
                        branchId = state.branchId,
                        weekStart = state.weekStart,
                        weekEnd = state.weekEnd,
                        salesAmount = sales,
                        prizesAmount = prizes,
                        commissionRate = commissionRate,
                        commissionAmount = commissionAmount,
                        cashDeliveredAmount = cashDelivered,
                        lossAbsorbedAmount = lossAbsorbedAmount,
                        weeklyBalance = weeklyBalance,
                        balanceBefore = state.previousBalance,
                        balanceAfter = finalBalanceAfter,
                        notes = state.notesInput.trim().ifBlank { null },
                        status = if (isLoss) "settled" else "confirmed",
                        settlementType = settlementType
                    )
                )
                _uiState.update { it.copy(isSubmitting = false, settlementSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = e.message ?: "No se pudo registrar el cuadre semanal")
                }
            }
        }
    }

    private fun updateAmounts(update: (WeeklySettlementUiState) -> WeeklySettlementUiState) {
        _uiState.update { current ->
            val updated = update(current)
            val sales = updated.salesInput.toAmountOrZero()
            val prizes = updated.prizesInput.toAmountOrZero()
            val commissionRate = updated.commissionRateInput.toAmountOrZero()
            val cashDelivered = updated.cashDeliveredInput.toAmountOrZero()

            val calculatedCommission = sales.multiply(commissionRate)
                .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
            val commission = if (updated.commissionAmountInput.isNotBlank()) {
                updated.commissionAmountInput.toAmountOrZero()
            } else {
                calculatedCommission
            }

            // Resultado del juego (hoja MegaLottery): Ventas - Premios - Comisión
            val gameResult = sales.subtract(prizes).subtract(commission)
            val netWithCash = gameResult.add(cashDelivered)
            val isLoss = netWithCash < BigDecimal.ZERO

            val lossAbsorbed = if (isLoss) netWithCash.abs() else BigDecimal.ZERO
            val weekly = if (isLoss) BigDecimal.ZERO else netWithCash
            val projected = updated.previousBalance.add(weekly)

            updated.copy(
                commissionAmount = FinancialCalculator.roundMoney(commission),
                gameResult = FinancialCalculator.roundMoney(gameResult),
                operatingResult = FinancialCalculator.roundMoney(gameResult),
                isLoss = isLoss,
                lossAbsorbedAmount = FinancialCalculator.roundMoney(lossAbsorbed),
                weeklyBalance = FinancialCalculator.roundMoney(weekly),
                projectedBalance = FinancialCalculator.roundMoney(projected)
            )
        }
    }

    private fun setError(message: String) = _uiState.update { it.copy(errorMessage = message) }

    private fun String.toAmountOrZero(): BigDecimal = toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO } ?: BigDecimal.ZERO

    private fun format(date: Date): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
}

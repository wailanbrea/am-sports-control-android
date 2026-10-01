package com.example.btmcontabilidad.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.ui.theme.PrimaryBlue
import com.example.btmcontabilidad.ui.viewmodel.WeeklySettlementViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterWeeklySettlementScreen(
    branchId: String,
    previousBalance: BigDecimal,
    viewModel: WeeklySettlementViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onSaved: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(branchId, previousBalance) { viewModel.initialize(branchId, previousBalance) }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(state.settlementSaved) {
        if (state.settlementSaved) onSaved()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Cuadre semanal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "Registre las ventas, premios y dinero entregado de la semana. Puede liquidar la semana para comenzar el lunes en $0.00 sin cargar pérdidas al vendedor.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoneyField("Ventas semanales", state.salesInput, viewModel::updateSales)
                    MoneyField("Premios pagados", state.prizesInput, viewModel::updatePrizes)
                    MoneyField("Comisión (%)", state.commissionRateInput, viewModel::updateCommissionRate)
                    MoneyField("Efectivo llevado a la banca (desde Caja Chica)", state.cashDeliveredInput, viewModel::updateCashDelivered)
                    Text(
                        text = "💡 Si llevaste dinero de caja chica para premios y no se recuperó con las ventas, la opción de liquidar abajo lo absorbe como pérdida para que el rifero arranque el lunes en $0.00.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.weekStart,
                        onValueChange = viewModel::updateWeekStart,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Inicio de semana (AAAA-MM-DD)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = state.weekEnd,
                        onValueChange = viewModel::updateWeekEnd,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Fin de semana (AAAA-MM-DD)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = state.notesInput,
                        onValueChange = viewModel::updateNotes,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Observaciones (opcional)") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Opción de Liquidar Semana en $0.00 (Absorber Pérdida)
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (state.absorbLoss) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleAbsorbLoss(!state.absorbLoss) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.absorbLoss,
                            onCheckedChange = viewModel::toggleAbsorbLoss,
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🛡️ Liquidar semana en $0.00 (Absorber pérdida)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Si la banca cierra en negativo o el dinero llevado no se recuperó, el consorcio absorbe la pérdida. La semana queda en $0.00 y NO se le suma a la deuda del vendedor.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Resumen Contable Protegido sin distorsión (Regla 8)
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Resumen contable", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Saldo anterior de la banca:")
                            Text(FinancialCalculator.formatCurrency(state.previousBalance), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        val sales = state.salesInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        val prizes = state.prizesInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        val cashDelivered = state.cashDeliveredInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        val hasNoProfit = (sales > BigDecimal.ZERO || prizes > BigDecimal.ZERO) && sales <= prizes

                        if (sales > BigDecimal.ZERO) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Ventas brutas:")
                                Text(FinancialCalculator.formatCurrency(sales), maxLines = 1, softWrap = false)
                            }
                        }
                        if (prizes > BigDecimal.ZERO) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Premios pagados:")
                                Text("-${FinancialCalculator.formatCurrency(prizes)}", color = MaterialTheme.colorScheme.error, maxLines = 1, softWrap = false)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Comisión (${state.commissionRateInput.ifBlank { "0" }}%):")
                            if (hasNoProfit) {
                                Text("$0.00 (No aplica)", color = MaterialTheme.colorScheme.error, maxLines = 1, softWrap = false)
                            } else {
                                Text(FinancialCalculator.formatCurrency(state.commissionAmount), maxLines = 1, softWrap = false)
                            }
                        }

                        if (cashDelivered > BigDecimal.ZERO) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Efectivo llevado de Caja Chica:")
                                Text(FinancialCalculator.formatCurrency(cashDelivered), fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Resultado operativo semanal:")
                            Text(
                                FinancialCalculator.formatCurrency(state.operatingResult),
                                fontWeight = FontWeight.Bold,
                                color = if (state.operatingResult < BigDecimal.ZERO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (state.isLoss) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (state.absorbLoss) {
                                        Text(
                                            "🛡️ Pérdida semanal asumida: -${FinancialCalculator.formatCurrency(state.lossAbsorbedAmount)}",
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.error,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            "El dinero llevado o perdido se asume por el consorcio. La semana cierra en $0.00. El rifero arranca el lunes en cero sin deuda adicional.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    } else {
                                        Text(
                                            "⚠️ Pérdida acumulada a la cuenta: ${FinancialCalculator.formatCurrency(state.weeklyBalance)}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Balance semanal resultante:", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (state.isLoss && state.absorbLoss) "$0.00 (Saldada)" else FinancialCalculator.formatCurrency(state.weeklyBalance),
                                fontWeight = FontWeight.Bold,
                                color = if (state.isLoss && state.absorbLoss) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Saldo deudor después del cuadre:", fontWeight = FontWeight.Bold)
                            Text(
                                FinancialCalculator.formatCurrency(state.projectedBalance),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (hasNoProfit) {
                            Text(
                                "💡 Las comisiones solo se descuentan de las ganancias; si la banca no genera ganancia, no se paga comisión ni se toma de caja chica.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = viewModel::submit,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = if (state.isLoss && state.absorbLoss) ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                             else ButtonDefaults.buttonColors()
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else if (state.isLoss && state.absorbLoss) {
                        Text("🛡️ Registrar Cuadre y Liquidar en $0.00", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Registrar Cuadre Semanal", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MoneyField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}


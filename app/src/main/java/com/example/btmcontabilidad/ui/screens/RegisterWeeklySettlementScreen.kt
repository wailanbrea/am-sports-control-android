package com.example.btmcontabilidad.ui.screens

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    previousBalance: BigDecimal = BigDecimal.ZERO,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit = onNavigateBack,
    viewModel: WeeklySettlementViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(branchId, previousBalance) {
        viewModel.initialize(branchId, previousBalance)
    }

    LaunchedEffect(state.settlementSaved) {
        if (state.settlementSaved) {
            onSaved()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Cuadre Semanal", fontWeight = FontWeight.Bold) },
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
                    "Ingrese los datos del reporte de la máquina (Periódico Riferos). Si la semana arroja pérdida, el consorcio la asume y el vendedor arranca en $0.00 sin tocar Caja Chica.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Datos del Reporte de MegaLottery
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Datos del Periódico Riferos",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )

                        MoneyField("Venta Juegos (US$)", state.salesInput, viewModel::updateSales)
                        MoneyField("Premios Pagados (US$)", state.prizesInput, viewModel::updatePrizes)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = state.commissionRateInput,
                                onValueChange = viewModel::updateCommissionRate,
                                modifier = Modifier.weight(1f),
                                label = { Text("Comisión (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = state.commissionAmountInput,
                                onValueChange = viewModel::updateCommissionAmount,
                                modifier = Modifier.weight(1.3f),
                                label = { Text("Monto Comisión ($)") },
                                placeholder = { Text(FinancialCalculator.formatCurrency(state.commissionAmount)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        MoneyField("Efectivo llevado para premios (opcional)", state.cashDeliveredInput, viewModel::updateCashDelivered)
                        Text(
                            text = "💡 Si durante la semana llevaste dinero físico a la banca para cubrir premios, regístralo aquí si no lo habías registrado antes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Fechas y Observaciones
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.weekStart,
                            onValueChange = viewModel::updateWeekStart,
                            modifier = Modifier.weight(1f),
                            label = { Text("Inicio (AAAA-MM-DD)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = state.weekEnd,
                            onValueChange = viewModel::updateWeekEnd,
                            modifier = Modifier.weight(1f),
                            label = { Text("Fin (AAAA-MM-DD)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    OutlinedTextField(
                        value = state.notesInput,
                        onValueChange = viewModel::updateNotes,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Observaciones (opcional)") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Resumen Contable Protegido sin distorsión (Regla 8)
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Resumen del Cuadre", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Saldo anterior del vendedor:")
                            Text(FinancialCalculator.formatCurrency(state.previousBalance), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        val sales = state.salesInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        val prizes = state.prizesInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        val cashDelivered = state.cashDeliveredInput.toBigDecimalOrNull() ?: BigDecimal.ZERO

                        if (sales > BigDecimal.ZERO) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Venta juegos:")
                                Text(FinancialCalculator.formatCurrency(sales), maxLines = 1, softWrap = false)
                            }
                        }
                        if (prizes > BigDecimal.ZERO) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Premios:")
                                Text("-${FinancialCalculator.formatCurrency(prizes)}", color = MaterialTheme.colorScheme.error, maxLines = 1, softWrap = false)
                            }
                        }
                        if (state.commissionAmount > BigDecimal.ZERO) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Comisión:")
                                Text("-${FinancialCalculator.formatCurrency(state.commissionAmount)}", color = MaterialTheme.colorScheme.error, maxLines = 1, softWrap = false)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Resultado del juego (columna TOTAL del papel de MegaLottery)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total papel de máquina:", fontWeight = FontWeight.Bold)
                            Text(
                                FinancialCalculator.formatCurrency(state.gameResult),
                                fontWeight = FontWeight.ExtraBold,
                                color = if (state.gameResult < BigDecimal.ZERO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (cashDelivered > BigDecimal.ZERO) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Dinero llevado para premios:")
                                Text("+${FinancialCalculator.formatCurrency(cashDelivered)}", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Conclusión de la semana
                        if (state.isLoss) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "🛡️ Pérdida del consorcio: -${FinancialCalculator.formatCurrency(state.lossAbsorbedAmount)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        "El consorcio absorbe la pérdida. La semana queda en $0.00 para el vendedor (no se le cobra) y NO se descuenta de Caja Chica.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Balance semanal del vendedor:", fontWeight = FontWeight.SemiBold)
                                Text("$0.00 (Saldada)", fontWeight = FontWeight.Bold, color = PrimaryBlue, maxLines = 1, softWrap = false)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "🟢 A cobrar al vendedor: ${FinancialCalculator.formatCurrency(state.weeklyBalance)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PrimaryBlue,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        "El vendedor tiene este dinero en su gaveta de las ventas netas para entregártelo.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Saldo total después del cuadre:", fontWeight = FontWeight.Bold)
                            Text(
                                FinancialCalculator.formatCurrency(state.projectedBalance),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
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
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else if (state.isLoss) {
                        Text("🛡️ Guardar Cuadre (Semana a $0.00)", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Guardar Cuadre Semanal", fontWeight = FontWeight.Bold)
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

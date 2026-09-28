package com.example.btmcontabilidad.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.ui.theme.PrimaryBlue
import com.example.btmcontabilidad.ui.theme.StatusAlertBg
import com.example.btmcontabilidad.ui.theme.StatusAlertContent
import com.example.btmcontabilidad.ui.theme.StatusReadyBg
import com.example.btmcontabilidad.ui.theme.StatusReadyContent
import com.example.btmcontabilidad.ui.viewmodel.ManualResultType
import com.example.btmcontabilidad.ui.viewmodel.RegisterManualResultViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterManualResultScreen(
    branchId: String,
    initialBalance: BigDecimal = BigDecimal.ZERO,
    viewModel: RegisterManualResultViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToMoneyDelivery: (branchId: String, suggestedAmount: String, resultId: Long?) -> Unit = { _, _, _ -> },
    onSaved: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(branchId, initialBalance) {
        viewModel.initialize(branchId, initialBalance)
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.resultSaved) {
        if (state.resultSaved && !state.showLossPrompt) {
            onSaved()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Registrar Resultado Manual", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Introduzca el resultado final consolidado. Un número positivo indica ganancia (+); un número negativo indica pérdida (-).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Balance previo: ${FinancialCalculator.formatCurrency(state.previousBalance)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (state.amountValue != null) {
                            Text(
                                text = "Nuevo balance proyectado: ${FinancialCalculator.formatCurrency(state.projectedBalance)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isNegative) StatusAlertContent else StatusReadyContent
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tipo de Resultado",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isProfit = state.resultType == ManualResultType.PROFIT
                        val isLoss = state.resultType == ManualResultType.LOSS

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setResultType(ManualResultType.PROFIT) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isProfit) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = if (isProfit) 2.dp else 1.dp,
                                color = if (isProfit) Color(0xFF2E7D32) else Color.LightGray.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = if (isProfit) Color(0xFF1B5E20) else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = "+ Ganancia",
                                    fontWeight = if (isProfit) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isProfit) Color(0xFF1B5E20) else Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setResultType(ManualResultType.LOSS) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLoss) Color(0xFFFEECEB) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = if (isLoss) 2.dp else 1.dp,
                                color = if (isLoss) Color(0xFFC62828) else Color.LightGray.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isLoss) Color(0xFFB71C1C) else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = "- Pérdida",
                                    fontWeight = if (isLoss) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isLoss) Color(0xFFB71C1C) else Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = viewModel::updateAmount,
                    label = {
                        Text(
                            if (state.resultType == ManualResultType.LOSS) "Monto de pérdida en banca ($)"
                            else "Monto de ganancia en banca ($)"
                        )
                    },
                    placeholder = {
                        Text(
                            if (state.resultType == ManualResultType.LOSS) "Ej: 1663.00"
                            else "Ej: 1400.00"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (state.resultType == ManualResultType.LOSS) StatusAlertBg else StatusReadyBg,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = if (state.resultType == ManualResultType.LOSS) " - $ " else " + $ ",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (state.resultType == ManualResultType.LOSS) StatusAlertContent else StatusReadyContent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                )
            }

            if (state.isNegative) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = StatusAlertBg
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusAlertContent)
                            Column {
                                Text(
                                    text = "PÉRDIDA OPERATIVA DETECTADA",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusAlertContent
                                )
                                Text(
                                    text = "Esta banca presenta una pérdida de ${FinancialCalculator.formatCurrency((state.amountValue ?: BigDecimal.ZERO).abs())}. El dinero debe llevarse inmediatamente.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusAlertContent
                                )
                            }
                        }
                    }
                }
            } else if (state.isPositive) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = StatusReadyBg
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "GANANCIA REGISTRADA",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusReadyContent
                                )
                                Text(
                                    text = "Ganancia de ${FinancialCalculator.formatCurrency(state.amountValue ?: BigDecimal.ZERO)}. Se acumulará para el cobro del lunes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusReadyContent
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.businessDate,
                    onValueChange = viewModel::updateDate,
                    label = { Text("Fecha contable (AAAA-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = state.notesInput,
                    onValueChange = viewModel::updateNotes,
                    label = { Text("Nota u observación (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = viewModel::submit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !state.isSubmitting && state.amountValue != null,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Guardar Resultado", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (state.showLossPrompt) {
        val lossAmount = (state.amountValue ?: BigDecimal.ZERO).abs()
        val prevDebt = state.previousBalance.coerceAtLeast(BigDecimal.ZERO)
        val coveredByDebt = lossAmount.min(prevDebt)
        val neededFromCashBox = (lossAmount - coveredByDebt).coerceAtLeast(BigDecimal.ZERO)

        val lossFormatted = FinancialCalculator.formatCurrency(lossAmount)
        val coveredFormatted = FinancialCalculator.formatCurrency(coveredByDebt)
        val neededFromCashBoxFormatted = FinancialCalculator.formatCurrency(neededFromCashBox)
        val prevDebtFormatted = FinancialCalculator.formatCurrency(state.previousBalance)
        val newBalanceFormatted = FinancialCalculator.formatCurrency(state.projectedBalance)

        AlertDialog(
            onDismissRequest = onSaved,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = if (neededFromCashBox == BigDecimal.ZERO) "Pérdida Cubierta con Deuda"
                        else if (coveredByDebt > BigDecimal.ZERO) "Compensación Mixta Automática"
                        else "Retiro de Caja Chica Requerido",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Pérdida registrada: $lossFormatted\nBalance previo de la banca: $prevDebtFormatted",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // CASO 1: Cubierto al 100% por la deuda existente (ej: 6169 deuda, 516 pérdida)
                    if (neededFromCashBox == BigDecimal.ZERO) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSaved() },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            border = BorderStroke(1.5.dp, Color(0xFF2E7D32))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFF1B5E20),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "100% Atribuido al Balance",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Text(
                                    text = "• Al Balance: -$lossFormatted descontados de la deuda.\n• Nuevo balance adeudado: $newBalanceFormatted.\n• Caja Chica: $0.00 (No requiere retiro de efectivo).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }

                    // CASO 2: Compensación Mixta (ej: 100 balance, 200 pérdida -> 100 balance, 100 caja chica)
                    else if (coveredByDebt > BigDecimal.ZERO) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                            border = BorderStroke(1.dp, Color(0xFF689F38))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Desglose Automático de la Pérdida:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF33691E)
                                )
                                Text(
                                    text = "1. Al Balance: $coveredFormatted descontados de la deuda (la deuda queda en $0.00).\n" +
                                            "2. De Caja Chica: $neededFromCashBoxFormatted faltantes a retirar para entregar a la banca.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF33691E)
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onNavigateToMoneyDelivery(
                                        state.branchId,
                                        neededFromCashBox.toPlainString(),
                                        state.savedResult?.id
                                    )
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                            border = BorderStroke(1.5.dp, Color(0xFFE65100))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Retirar $neededFromCashBoxFormatted de Caja Chica",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFBF360C),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Text(
                                    text = "Registrar la salida física de los $neededFromCashBoxFormatted restantes para la banca.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFBF360C)
                                )
                            }
                        }
                    }

                    // CASO 3: Sin deuda previa (balance <= 0, ej: 0 balance, 200 pérdida -> 200 caja chica)
                    else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onNavigateToMoneyDelivery(
                                        state.branchId,
                                        lossAmount.toPlainString(),
                                        state.savedResult?.id
                                    )
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                            border = BorderStroke(1.5.dp, Color(0xFFE65100))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Retirar $lossFormatted de Caja Chica",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFBF360C),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Text(
                                    text = "La banca no tiene deuda disponible. Los $lossFormatted deben salir de Caja Chica.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFBF360C)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (neededFromCashBox == BigDecimal.ZERO) {
                    Button(
                        onClick = onSaved,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("Aceptar y Finalizar")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onSaved,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (neededFromCashBox == BigDecimal.ZERO) "Cerrar" else "Omitir Retiro (Solo Balance)")
                }
            }
        )
    }
}

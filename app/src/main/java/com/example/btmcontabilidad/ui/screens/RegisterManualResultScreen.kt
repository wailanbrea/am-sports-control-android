package com.example.btmcontabilidad.ui.screens

import androidx.compose.foundation.background
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
                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = viewModel::updateAmount,
                    label = { Text("Resultado manual final ($)") },
                    placeholder = { Text("Ej: -1663 ó +1400") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) }
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
        val loss = (state.amountValue ?: BigDecimal.ZERO).abs().toPlainString()
        AlertDialog(
            onDismissRequest = onSaved,
            title = { Text("Pérdida Registrada", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "La banca presenta una pérdida de ${FinancialCalculator.formatCurrency((state.amountValue ?: BigDecimal.ZERO).abs())}.\n\n" +
                            "¿Deseas registrar el dinero llevado a esta banca ahora mismo?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onNavigateToMoneyDelivery(
                            state.branchId,
                            loss,
                            state.savedResult?.id
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Registrar Ahora")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onSaved) {
                    Text("Más tarde")
                }
            }
        )
    }
}

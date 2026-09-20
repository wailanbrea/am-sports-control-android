package com.example.btmcontabilidad.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.btmcontabilidad.ui.viewmodel.RegisterMoneyDeliveryViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterMoneyDeliveryScreen(
    branchId: String,
    suggestedAmount: BigDecimal = BigDecimal.ZERO,
    manualResultId: Long? = null,
    viewModel: RegisterMoneyDeliveryViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onSaved: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(branchId, suggestedAmount, manualResultId) {
        viewModel.initialize(branchId, suggestedAmount, manualResultId)
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.deliverySaved) {
        if (state.deliverySaved) {
            onSaved()
        }
    }

    var expandedDropdown by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Llevar Dinero a Banca", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoadingBranches) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Branch Selector Dropdown
                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        OutlinedTextField(
                            value = state.selectedBranch?.let { "${it.code} — ${it.name}${if (!it.operatorName.isNullOrBlank()) " (${it.operatorName})" else ""}" } ?: "Seleccionar Banca",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Banca Receptora del Dinero") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            state.availableBranches.forEach { branch ->
                                DropdownMenuItem(
                                    text = { Text("${branch.code} — ${branch.name}${if (!branch.operatorName.isNullOrBlank()) " (${branch.operatorName})" else ""}") },
                                    onClick = {
                                        viewModel.selectBranch(branch.id)
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Balance information banner
                item {
                    val branchBalance = state.selectedBranch?.currentBalance ?: BigDecimal.ZERO
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (branchBalance < BigDecimal.ZERO) StatusAlertBg else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (branchBalance < BigDecimal.ZERO) "DÉFICIT PENDIENTE POR PREMIOS" else "SALDO ACTUAL DE LA BANCA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (branchBalance < BigDecimal.ZERO) StatusAlertContent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FinancialCalculator.formatCurrency(branchBalance),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (branchBalance < BigDecimal.ZERO) StatusAlertContent else MaterialTheme.colorScheme.onSurface
                            )
                            if (branchBalance < BigDecimal.ZERO) {
                                Text(
                                    text = "Esta banca necesita ${FinancialCalculator.formatCurrency(branchBalance.abs())} para pagar premios a clientes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusAlertContent.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Registre el dinero en efectivo entregado a la banca para el pago de premios. Este dinero compensa la cuenta de la banca y salda el déficit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            item {
                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = viewModel::updateAmount,
                    label = { Text("Monto realmente entregado ($)") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) }
                )
            }

            item {
                OutlinedTextField(
                    value = state.reasonInput,
                    onValueChange = viewModel::updateReason,
                    label = { Text("Motivo de entrega") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = state.businessDate,
                    onValueChange = viewModel::updateDate,
                    label = { Text("Fecha de entrega (AAAA-MM-DD)") },
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
                    enabled = !state.isSubmitting && state.amountValue != null && (state.amountValue ?: BigDecimal.ZERO) > BigDecimal.ZERO,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Confirmar Salida y Entrega", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
}

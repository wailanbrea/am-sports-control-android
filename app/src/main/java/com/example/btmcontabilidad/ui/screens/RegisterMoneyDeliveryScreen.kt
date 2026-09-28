package com.example.btmcontabilidad.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
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
import com.example.btmcontabilidad.ui.components.ReceiptData
import com.example.btmcontabilidad.ui.components.ReceiptDialog
import com.example.btmcontabilidad.ui.theme.PrimaryBlue
import com.example.btmcontabilidad.ui.theme.StatusAlertBg
import com.example.btmcontabilidad.ui.theme.StatusAlertContent
import com.example.btmcontabilidad.ui.viewmodel.RegisterMoneyDeliveryViewModel
import com.example.btmcontabilidad.util.ReceiptManager
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

    if (state.deliverySaved && state.savedDelivery != null && state.selectedBranch != null) {
        val branch = state.selectedBranch!!
        val saved = state.savedDelivery!!
        val cashBoxName = state.selectedCashBox?.name ?: "Caja Chica"
        val receiptText = ReceiptManager.buildMoneyDeliveryReceiptText(
            deliveryId = saved.id.toString(),
            branch = branch,
            amount = saved.deliveredAmount,
            reason = saved.reason,
            cashBoxName = cashBoxName,
            notes = saved.notes,
            previousBalance = state.previousBalance,
            newBalance = state.newBalance,
            grossAmount = saved.grossAmount.takeIf { it > BigDecimal.ZERO },
            commissionRate = saved.commissionRate.takeIf { it > BigDecimal.ZERO },
            commissionAmount = saved.commissionAmount.takeIf { it > BigDecimal.ZERO }
        )
        val receiptData = ReceiptData(
            title = "COMPROBANTE DE ENTREGA",
            receiptId = saved.id.toString(),
            amount = saved.deliveredAmount,
            branch = branch,
            recipientPhone = branch.ownerPhone ?: branch.phone,
            previousBalance = state.previousBalance,
            newBalance = state.newBalance,
            concept = saved.reason,
            reference = cashBoxName,
            fullReceiptText = receiptText
        )

        ReceiptDialog(
            receipt = receiptData,
            onDismiss = {
                viewModel.clearSavedDelivery()
                onSaved()
            }
        )
    }

    var expandedDropdown by remember { mutableStateOf(false) }
    var cashBoxDropdownExpanded by remember { mutableStateOf(false) }

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

                // Selector de Caja Chica de origen
                if (state.cashBoxes.isNotEmpty()) {
                    item {
                        ExposedDropdownMenuBox(
                            expanded = cashBoxDropdownExpanded,
                            onExpandedChange = { cashBoxDropdownExpanded = !cashBoxDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = state.selectedCashBox?.let { "${it.name} (${FinancialCalculator.formatCurrency(it.balance)})" }
                                    ?: "Seleccionar Caja Chica",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Caja Chica de Origen (Fondo)") },
                                leadingIcon = {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = cashBoxDropdownExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(12.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = cashBoxDropdownExpanded,
                                onDismissRequest = { cashBoxDropdownExpanded = false }
                            ) {
                                state.cashBoxes.forEach { box ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(if (box.isDefault) "${box.name} ★" else box.name)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    FinancialCalculator.formatCurrency(box.balance),
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryBlue
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectCashBox(box.id)
                                            cashBoxDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Banner de Origen de Fondos (Caja Chica)
                item {
                    val boxName = state.selectedCashBox?.name ?: "la Caja Chica"
                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🏦", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Este monto saldrá automáticamente de $boxName y compensará la cuenta de la banca como desembolso para premios.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                item {
                    val branchRate = state.selectedBranch?.commissionRate ?: BigDecimal.ZERO
                    val hasConfiguredRate = branchRate > BigDecimal.ZERO
                    val questionTitle = if (hasConfiguredRate) {
                        "¿Dará el Porciento (${branchRate.stripTrailingZeros().toPlainString()}%)?"
                    } else {
                        "¿Dará el Porciento de Comisión?"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.applyCommission) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (state.applyCommission) PrimaryBlue else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = questionTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.applyCommission) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (hasConfiguredRate) {
                                            "Descontar el ${branchRate.stripTrailingZeros().toPlainString()}% de comisión de ganancias (no aplica a salidas de caja chica por pérdidas o premios)"
                                        } else {
                                            "Aplicar comisión (solo si la banca generó ganancias; no aplica a caja chica)"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                androidx.compose.material3.Switch(
                                    checked = state.applyCommission,
                                    onCheckedChange = viewModel::toggleApplyCommission
                                )
                            }

                            if (state.applyCommission) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = state.customCommissionRateInput,
                                    onValueChange = viewModel::updateCustomCommissionRate,
                                    label = { Text("Porcentaje a aplicar (%)") },
                                    placeholder = { Text("10") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                if (state.grossAmountValue != null && (state.grossAmountValue ?: BigDecimal.ZERO) > BigDecimal.ZERO) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    androidx.compose.material3.Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Ganancia Bruta a Liquidar:", style = MaterialTheme.typography.bodySmall)
                                                Text(
                                                    FinancialCalculator.formatCurrency(state.grossAmountValue ?: BigDecimal.ZERO),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "Descuento Comisión (${state.effectiveCommissionRate.stripTrailingZeros().toPlainString()}%):",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                Text(
                                                    "- " + FinancialCalculator.formatCurrency(state.commissionAmount),
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Efectivo Físico a Entregar:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                Text(
                                                    FinancialCalculator.formatCurrency(state.netDeliveredAmount),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryBlue
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Registre el dinero entregado a la banca. Si aplica comisión, se descontará del efectivo físico a llevar y se compensará el saldo total de la banca.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    OutlinedTextField(
                        value = state.amountInput,
                        onValueChange = viewModel::updateAmount,
                        label = { Text(if (state.applyCommission) "Ganancia Bruta a Liquidar ($)" else "Monto a Entregar ($)") },
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
                        enabled = !state.isSubmitting && state.grossAmountValue != null && (state.grossAmountValue ?: BigDecimal.ZERO) > BigDecimal.ZERO,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            val label = if (state.applyCommission && state.commissionAmount > BigDecimal.ZERO) {
                                "Entregar ${FinancialCalculator.formatCurrency(state.netDeliveredAmount)} en Efectivo"
                            } else {
                                "Confirmar Salida y Entrega"
                            }
                            Text(label, fontWeight = FontWeight.Bold)
                        }
                    }
                }
        }
    }
}
}

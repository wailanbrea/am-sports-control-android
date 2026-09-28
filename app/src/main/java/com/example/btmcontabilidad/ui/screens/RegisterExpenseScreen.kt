package com.example.btmcontabilidad.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.ui.components.ReceiptData
import com.example.btmcontabilidad.ui.components.ReceiptDialog
import com.example.btmcontabilidad.ui.theme.PrimaryBlue
import com.example.btmcontabilidad.ui.theme.StatusAlertBg
import com.example.btmcontabilidad.ui.theme.StatusAlertContent
import com.example.btmcontabilidad.ui.viewmodel.RegisterExpenseUiState
import com.example.btmcontabilidad.ui.viewmodel.RegisterExpenseViewModel
import com.example.btmcontabilidad.util.ReceiptManager
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterExpenseScreen(
    initialBranchId: String? = null,
    initialCategoryId: String? = null,
    viewModel: RegisterExpenseViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onSaved: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialBranchId, initialCategoryId) {
        viewModel.initialize(initialBranchId, initialCategoryId)
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    if (state.expenseSaved && state.savedExpense != null) {
        val saved = state.savedExpense!!
        val cashBoxName = state.selectedCashBox?.name ?: "Caja Chica"
        val branchName = state.selectedBranch?.let { "${it.code} - ${it.name}" } ?: "Oficina Principal / General"

        val receiptText = ReceiptManager.buildExpenseReceiptText(
            expenseId = saved.id,
            amount = saved.amount,
            reason = saved.reason,
            category = state.selectedCategory.name,
            cashBoxName = cashBoxName,
            branchName = branchName,
            reference = saved.reference,
            notes = saved.notes
        )

        val isComision = state.selectedCategory.id == "comision" || saved.reason.contains("Comisión", ignoreCase = true)
        val receiptData = ReceiptData(
            title = if (isComision) "COMPROBANTE DE COMISIÓN" else "COMPROBANTE DE GASTO",
            receiptId = saved.id.ifBlank { if (isComision) "COMISIÓN" else "GASTO" },
            amount = saved.amount,
            branch = state.selectedBranch ?: Branch(
                id = "OFICINA",
                code = "ADM",
                name = "Oficina Principal",
                route = "Central",
                operatorName = "Administración"
            ),
            recipientPhone = state.selectedBranch?.ownerPhone ?: state.selectedBranch?.phone,
            concept = saved.reason,
            reference = saved.reference ?: cashBoxName,
            fullReceiptText = receiptText
        )

        ReceiptDialog(
            receipt = receiptData,
            onDismiss = onSaved
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Registrar Gasto",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabecera Informativa
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusAlertBg.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, StatusAlertContent.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(StatusAlertContent.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = StatusAlertContent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Salida de Efectivo / Gasto",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusAlertContent
                                )
                                Text(
                                    text = "El dinero se descontará directamente de la caja chica seleccionada.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Categoría del Gasto (Chips)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Categoría del Gasto",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(RegisterExpenseUiState.defaultCategories) { category ->
                                val selected = state.selectedCategory.id == category.id
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.selectCategory(category) },
                                    label = {
                                        Text("${category.iconEmoji} ${category.name}", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                                        selectedLabelColor = PrimaryBlue
                                    )
                                )
                            }
                        }
                    }
                }

                // Campo Monto
                item {
                    OutlinedTextField(
                        value = state.amountInput,
                        onValueChange = { viewModel.updateAmount(it) },
                        label = { Text("Monto del Gasto (RD$)") },
                        placeholder = { Text("0.00") },
                        leadingIcon = {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = StatusAlertContent)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Motivo / Concepto
                item {
                    OutlinedTextField(
                        value = state.reasonInput,
                        onValueChange = { viewModel.updateReason(it) },
                        label = { Text("Concepto o Motivo del Gasto") },
                        placeholder = { Text("Ej: Pago de factura de luz CDEEE") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 3
                    )
                }

                // Selector de Caja Chica
                item {
                    var expandedBoxDropdown by remember { mutableStateOf(false) }
                    val currentBox = state.selectedCashBox

                    ExposedDropdownMenuBox(
                        expanded = expandedBoxDropdown,
                        onExpandedChange = { expandedBoxDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = if (currentBox != null) "${currentBox.name} (Saldo: ${FinancialCalculator.formatCurrency(currentBox.balance)})" else "Seleccionar Caja Chica",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Caja Chica de Origen") },
                            leadingIcon = {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PrimaryBlue)
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBoxDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedBoxDropdown,
                            onDismissRequest = { expandedBoxDropdown = false }
                        ) {
                            state.cashBoxes.forEach { box ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(box.name, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Disponible: ${FinancialCalculator.formatCurrency(box.balance)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectCashBox(box.id)
                                        expandedBoxDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Selector de Banca Asociada (Opcional)
                item {
                    var expandedBranchDropdown by remember { mutableStateOf(false) }
                    val currentBranch = state.selectedBranch

                    ExposedDropdownMenuBox(
                        expanded = expandedBranchDropdown,
                        onExpandedChange = { expandedBranchDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = currentBranch?.let { "${it.code} - ${it.name}" } ?: "Oficina Principal (General / Ninguna)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Banca Asignada (Opcional)") },
                            leadingIcon = {
                                Icon(Icons.Default.Store, contentDescription = null, tint = PrimaryBlue)
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBranchDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedBranchDropdown,
                            onDismissRequest = { expandedBranchDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🏢 Oficina Principal (General / Ninguna)") },
                                onClick = {
                                    viewModel.selectBranch(null)
                                    expandedBranchDropdown = false
                                }
                            )
                            state.availableBranches.forEach { branch ->
                                DropdownMenuItem(
                                    text = { Text("${branch.code} - ${branch.name}") },
                                    onClick = {
                                        viewModel.selectBranch(branch.id)
                                        expandedBranchDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Fecha y No. de Factura / Referencia
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = state.businessDate,
                            onValueChange = { viewModel.updateBusinessDate(it) },
                            label = { Text("Fecha (yyyy-MM-dd)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = state.referenceInput,
                            onValueChange = { viewModel.updateReference(it) },
                            label = { Text("No. Factura / Ref.") },
                            placeholder = { Text("Ej: B01-9982") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }

                // Observaciones
                item {
                    OutlinedTextField(
                        value = state.notesInput,
                        onValueChange = { viewModel.updateNotes(it) },
                        label = { Text("Notas / Observaciones (Opcional)") },
                        placeholder = { Text("Comentario adicional sobre el gasto...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 3
                    )
                }

                // Botón de Enviar
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.submitExpense() },
                        enabled = !state.isSubmitting && (state.amountValue ?: BigDecimal.ZERO) > BigDecimal.ZERO,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusAlertContent
                        )
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Registrando...")
                        } else {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text(
                                text = "Registrar Gasto",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

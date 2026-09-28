package com.example.btmcontabilidad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.PaymentMethod
import com.example.btmcontabilidad.ui.components.ReceiptData
import com.example.btmcontabilidad.ui.components.ReceiptDialog
import com.example.btmcontabilidad.ui.theme.BTMContabilidadTheme
import com.example.btmcontabilidad.ui.theme.DeepNavy
import com.example.btmcontabilidad.ui.theme.PrimaryBlue
import com.example.btmcontabilidad.ui.theme.StatusAlertBg
import com.example.btmcontabilidad.ui.theme.StatusAlertBorder
import com.example.btmcontabilidad.ui.theme.StatusAlertContent
import com.example.btmcontabilidad.ui.theme.StatusPendingBg
import com.example.btmcontabilidad.ui.theme.StatusPendingContent
import com.example.btmcontabilidad.ui.theme.StatusReadyBg
import com.example.btmcontabilidad.ui.theme.StatusReadyContent
import com.example.btmcontabilidad.ui.viewmodel.CollectionViewModel
import com.example.btmcontabilidad.util.ReceiptManager
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterCollectionScreen(
    initialBranchId: String? = null,
    viewModel: CollectionViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var expandedDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(initialBranchId) {
        if (!initialBranchId.isNullOrBlank() || uiState.availableBranches.isEmpty()) {
            viewModel.loadBranches(initialBranchId)
        }
    }


    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (uiState.collectionSaved && uiState.savedCollection != null && uiState.selectedBranch != null) {
        val branch = uiState.selectedBranch!!
        val saved = uiState.savedCollection!!
        val receiptText = ReceiptManager.buildCollectionReceiptText(
            collectionId = saved.id,
            branch = branch,
            amount = saved.amount,
            paymentMethod = saved.paymentMethod,
            reference = saved.reference,
            notes = saved.notes,
            previousBalance = uiState.lastPreviousBalance,
            newBalance = uiState.lastNewBalance
        )
        val receiptData = ReceiptData(
            title = "COMPROBANTE DE COBRO",
            receiptId = saved.id,
            amount = saved.amount,
            branch = branch,
            recipientPhone = branch.ownerPhone ?: branch.phone,
            previousBalance = uiState.lastPreviousBalance,
            newBalance = uiState.lastNewBalance,
            paymentMethod = saved.paymentMethod.label,
            concept = "Cobro de balance",
            reference = saved.reference,
            fullReceiptText = receiptText
        )

        ReceiptDialog(
            receipt = receiptData,
            onDismiss = {
                viewModel.clearSavedCollection()
                onNavigateBack()
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Registrar Cobro",
                        style = MaterialTheme.typography.titleLarge,
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
        if (uiState.isLoadingBranches) {
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
                // Branch Selector Dropdown
                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        OutlinedTextField(
                            value = uiState.selectedBranch?.let { "${it.code} — ${it.name}${if (!it.operatorName.isNullOrBlank()) " (${it.operatorName})" else ""}" } ?: "Seleccionar Banca",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Banca Receptora") },
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
                            uiState.availableBranches.forEach { branch ->
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

                // Current Debt / Balance Banner con Desglose Semanal vs Saldo Viejo
                item {
                    val balance = uiState.previousBalance
                    val (bg, fg, label) = when {
                        balance > BigDecimal.ZERO -> Triple(StatusPendingBg, StatusPendingContent, "Por cobrar")
                        balance < BigDecimal.ZERO -> Triple(StatusAlertBg, StatusAlertContent, "Por enviar")
                        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Al día")
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DeepNavy)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = when {
                                            balance > BigDecimal.ZERO -> "DEUDA TOTAL A RECOGER"
                                            balance < BigDecimal.ZERO -> "DÉFICIT EN BANCA (PREMIOS)"
                                            else -> "SALDO ACTUAL DE LA BANCA"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = FinancialCalculator.formatCurrency(balance),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }

                                Surface(shape = RoundedCornerShape(8.dp), color = bg) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = fg,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Desglose: Esta semana vs Saldo Viejo (Anterior)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.10f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📅 Esta semana",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = FinancialCalculator.formatCurrency(uiState.thisWeekBalance),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "⏳ Saldo anterior (viejo)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = FinancialCalculator.formatCurrency(uiState.oldBalance),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFB74D)
                                        )
                                    }

                                    HorizontalDivider(
                                        color = Color.White.copy(alpha = 0.15f),
                                        thickness = 1.dp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "💰 Total a cobrar",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = FinancialCalculator.formatCurrency(balance),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = StatusReadyBg
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Form Inputs
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Datos del Cobro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Selector de Cobrador: Solo para Administradores
                            if (uiState.isAdmin && uiState.collectors.isNotEmpty()) {
                                var collectorDropdownExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = collectorDropdownExpanded,
                                    onExpandedChange = { collectorDropdownExpanded = !collectorDropdownExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = uiState.selectedCollector?.name ?: "Seleccionar cobrador",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Cobrador Responsable") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = collectorDropdownExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = collectorDropdownExpanded,
                                        onDismissRequest = { collectorDropdownExpanded = false }
                                    ) {
                                        uiState.collectors.forEach { collector ->
                                            DropdownMenuItem(
                                                text = { Text(collector.name) },
                                                onClick = {
                                                    viewModel.selectCollector(collector)
                                                    collectorDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Amount Input
                            OutlinedTextField(
                                value = uiState.amountInput,
                                onValueChange = { viewModel.updateAmount(it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Monto a Recoger / Cobrado") },
                                 prefix = { Text("${FinancialCalculator.currencySymbol()} ", fontWeight = FontWeight.Bold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            // Botones de monto rápido (Semana / Viejo / Total)
                            if (uiState.previousBalance > BigDecimal.ZERO) {
                                if (uiState.thisWeekBalance > BigDecimal.ZERO && uiState.oldBalance > BigDecimal.ZERO) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        androidx.compose.material3.OutlinedButton(
                                            onClick = { viewModel.updateAmount(uiState.thisWeekBalance.toPlainString()) },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "Esta semana (${FinancialCalculator.formatCurrency(uiState.thisWeekBalance)})",
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1
                                            )
                                        }
                                        androidx.compose.material3.OutlinedButton(
                                            onClick = { viewModel.updateAmount(uiState.oldBalance.toPlainString()) },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "Saldo viejo (${FinancialCalculator.formatCurrency(uiState.oldBalance)})",
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { viewModel.updateAmount(uiState.previousBalance.toPlainString()) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cobrar total completo (${FinancialCalculator.formatCurrency(uiState.previousBalance)})")
                                }
                            }

                            // Payment Method Filter Chips
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Método de Pago",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(listOf(PaymentMethod.EFECTIVO, PaymentMethod.TRANSFERENCIA)) { method ->
                                        FilterChip(
                                            selected = uiState.paymentMethod == method,
                                            onClick = { viewModel.updatePaymentMethod(method) },
                                            label = { Text(method.label) }
                                        )
                                    }
                                }
                            }

                            // Reference
                            OutlinedTextField(
                                value = uiState.referenceInput,
                                onValueChange = { viewModel.updateReference(it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("No. de Referencia / Comprobante (opcional)") },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            // Business Date
                             OutlinedTextField(
                                 value = uiState.businessDateInput,
                                 onValueChange = {},
                                 modifier = Modifier.fillMaxWidth(),
                                 label = { Text("Fecha Operativa (automática)") },
                                 readOnly = true,
                                 shape = RoundedCornerShape(12.dp),
                                 singleLine = true
                             )

                            // Notes
                            OutlinedTextField(
                                value = uiState.notesInput,
                                onValueChange = { viewModel.updateNotes(it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Observaciones (opcional)") },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Balance Projection Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Proyección de Balance Pos-Cobro:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Saldo previo: ${FinancialCalculator.formatCurrency(uiState.previousBalance)} → Nuevo saldo: ${FinancialCalculator.formatCurrency(uiState.projectedNewBalance)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Overcollection Warning Banner
                if (uiState.isOvercollectionWarning) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(StatusAlertBg)
                                .border(1.dp, StatusAlertBorder, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Advertencia",
                                    tint = StatusAlertContent,
                                    modifier = Modifier.padding(end = 10.dp)
                                )
                                Column {
                                    Text(
                                        text = "Advertencia de Sobre-cobro",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusAlertContent
                                    )
                                    Text(
                                        text = "El cobro excede la deuda actual de la banca. La banca quedará con un saldo a favor de ${FinancialCalculator.formatCurrency(uiState.projectedNewBalance)}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StatusAlertContent
                                    )
                                }
                            }
                        }
                    }
                }

                // Submit Button
                item {
                    Button(
                        onClick = { viewModel.submitCollection() },
                        enabled = !uiState.isSubmitting && uiState.amountInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.height(24.dp))
                        } else {
                            Text(
                                text = "Registrar Cobro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun RegisterCollectionScreenPreview() {
    BTMContabilidadTheme {
        RegisterCollectionScreen()
    }
}

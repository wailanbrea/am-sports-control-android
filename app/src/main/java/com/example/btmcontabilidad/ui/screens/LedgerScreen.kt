package com.example.btmcontabilidad.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btmcontabilidad.ui.theme.BTMContabilidadTheme
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.LedgerEntry
import com.example.btmcontabilidad.domain.model.LedgerEntryType
import com.example.btmcontabilidad.domain.model.LedgerSourceType
import com.example.btmcontabilidad.domain.model.isCollection
import com.example.btmcontabilidad.domain.model.isMoneyDelivery
import com.example.btmcontabilidad.domain.model.isNegativeMovement
import com.example.btmcontabilidad.ui.theme.DeepNavy
import com.example.btmcontabilidad.ui.theme.PrimaryBlue
import com.example.btmcontabilidad.ui.theme.StatusAlertBg
import com.example.btmcontabilidad.ui.theme.StatusAlertContent
import com.example.btmcontabilidad.ui.theme.StatusNeutralBg
import com.example.btmcontabilidad.ui.theme.StatusNeutralContent
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.ui.components.ReceiptData
import com.example.btmcontabilidad.ui.components.ReceiptDialog
import com.example.btmcontabilidad.ui.components.WhatsAppDarkGreen
import com.example.btmcontabilidad.ui.theme.StatusReadyBg
import com.example.btmcontabilidad.ui.theme.StatusReadyContent
import com.example.btmcontabilidad.ui.viewmodel.LedgerTypeFilter
import com.example.btmcontabilidad.ui.viewmodel.LedgerViewModel
import com.example.btmcontabilidad.util.ReceiptManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedEntryToReverse by remember { mutableStateOf<LedgerEntry?>(null) }
    var selectedReceiptData by remember { mutableStateOf<ReceiptData?>(null) }

    LaunchedEffect(uiState.errorMessage, uiState.actionSuccessMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.actionSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (selectedReceiptData != null) {
        ReceiptDialog(
            receipt = selectedReceiptData!!,
            onDismiss = { selectedReceiptData = null }
        )
    }

    if (selectedEntryToReverse != null) {
        ReversalDialog(
            entry = selectedEntryToReverse!!,
            onDismiss = { selectedEntryToReverse = null },
            onConfirm = { reason ->
                val entryId = selectedEntryToReverse!!.id
                selectedEntryToReverse = null
                viewModel.reverseTransaction(entryId, reason)
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Libro Mayor",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
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
                // Totals Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DeepNavy)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL DÉBITOS (Cargos)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = FinancialCalculator.formatCurrency(uiState.totalDebits),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Column {
                                Text(
                                    text = "TOTAL CRÉDITOS (Abonos)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = FinancialCalculator.formatCurrency(uiState.totalCredits),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusReadyBg
                                )
                            }
                        }
                    }
                }

                // Search Box
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar por ID, banca, descripción...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }

                // Type Filters
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(LedgerTypeFilter.entries.toTypedArray()) { filter ->
                            FilterChip(
                                selected = uiState.selectedTypeFilter == filter,
                                onClick = { viewModel.setTypeFilter(filter) },
                                label = { Text(filter.label) }
                            )
                        }
                    }
                }

                // Branch Filters
                if (uiState.branchesMap.isNotEmpty()) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = uiState.selectedBranchId == null,
                                    onClick = { viewModel.setBranchFilter(null) },
                                    label = { Text("Todas las bancas") }
                                )
                            }
                            items(uiState.branchesMap.values.toList()) { branch ->
                                FilterChip(
                                    selected = uiState.selectedBranchId == branch.id,
                                    onClick = {
                                        if (uiState.selectedBranchId == branch.id) {
                                            viewModel.setBranchFilter(null)
                                        } else {
                                            viewModel.setBranchFilter(branch.id)
                                        }
                                    },
                                    label = { Text("${branch.code} - ${branch.name}") }
                                )
                            }
                        }
                    }
                }

                // Ledger Entries List
                items(
                    items = uiState.filteredEntries,
                    key = { it.id }
                ) { entry ->
                    val branch = uiState.branchesMap[entry.branchId]
                    LedgerCardItem(
                        entry = entry,
                        branch = branch,
                        onReverseClicked = { selectedEntryToReverse = entry },
                        onShareClicked = {
                            val receiptText = ReceiptManager.buildLedgerReceiptText(entry, branch)
                            selectedReceiptData = ReceiptData(
                                title = when {
                                    entry.isCollection() -> "COMPROBANTE DE COBRO"
                                    entry.isMoneyDelivery() -> "COMPROBANTE DE ENTREGA"
                                    entry.sourceType.name.contains("ADVANCE", ignoreCase = true) -> "COMPROBANTE DE ADELANTO"
                                    entry.sourceType.name.contains("SETTLEMENT", ignoreCase = true) -> "LIQUIDACIÓN SEMANAL"
                                    entry.sourceType.name.contains("MANUAL_RESULT", ignoreCase = true) -> "RESULTADO DE OPERACIONES"
                                    else -> "COMPROBANTE DE MOVIMIENTO"
                                },
                                receiptId = entry.id,
                                amount = entry.signedAmount.abs(),
                                branch = branch ?: Branch(
                                    id = entry.branchId,
                                    code = entry.branchId,
                                    name = "Banca ${entry.branchId}",
                                    route = "Central",
                                    operatorName = "Operador"
                                ),
                                recipientPhone = branch?.ownerPhone ?: branch?.phone,
                                previousBalance = entry.balanceBefore,
                                newBalance = entry.balanceAfter,
                                concept = entry.description,
                                reference = entry.sourceId,
                                fullReceiptText = receiptText
                            )
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun LedgerCardItem(
    entry: LedgerEntry,
    branch: Branch? = null,
    onReverseClicked: () -> Unit,
    onShareClicked: () -> Unit
) {
    val isNegative = entry.isNegativeMovement()
    val isMoneyDelivery = entry.isMoneyDelivery()
    val isCollection = entry.isCollection()
    val amountColor = if (isNegative) StatusAlertContent else StatusReadyContent
    val badgeBg = if (isNegative) StatusAlertBg else StatusReadyBg
    val prefix = if (isNegative) "- " else "+ "
    val isReversed = entry.reversalOfEntryId != null || entry.description.startsWith("Anulación")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onShareClicked() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cabecera: Icono + Tipo + Monto Destacado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (icon, tint) = when {
                        isCollection -> Pair(Icons.Default.Payments, StatusReadyContent)
                        isMoneyDelivery -> Pair(Icons.Default.LocalAtm, StatusAlertContent)
                        isNegative -> Pair(Icons.Default.LocalAtm, StatusAlertContent)
                        else -> Pair(Icons.Default.Add, StatusReadyContent)
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = badgeBg,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                        }
                    }

                    Column {
                        val typeLabel = when {
                            isReversed -> "ANULACIÓN / REVERSO"
                            isMoneyDelivery -> "Entrega de Premios"
                            isCollection -> "Cobro a Banca"
                            isNegative -> "Pérdida Operativa"
                            else -> "Ganancia Operativa"
                        }
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isReversed) StatusAlertContent else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = entry.businessDate.substringBefore('T'),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "$prefix${FinancialCalculator.formatCurrency(entry.signedAmount.abs())}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor,
                    maxLines = 1
                )
            }

            // Descripción
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Tira de Trazabilidad: Había ➔ Quedó en Banca
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Había:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FinancialCalculator.formatCurrency(entry.balanceBefore),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Quedó:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FinancialCalculator.formatCurrency(entry.balanceAfter),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryBlue
                        )
                    }
                }
            }

            // Badges / Micro-pills inferiores y botón de anulación
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = branch?.let { "${it.code} ${it.name}" } ?: "Banca ${entry.branchId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (entry.createdBy.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = entry.createdBy,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onShareClicked,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Enviar por WhatsApp",
                            tint = WhatsAppDarkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (!isReversed) {
                        OutlinedButton(
                            onClick = onReverseClicked,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusAlertContent)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Anular", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReversalDialog(
    entry: LedgerEntry,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Anular Transacción ${entry.id}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Está a punto de anular el movimiento '${entry.description}' por valor de ${FinancialCalculator.formatCurrency(entry.signedAmount.abs())}.")
                Text("Se generará una transacción de reversión y se actualizará el balance de la banca.", style = MaterialTheme.typography.bodySmall)

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Motivo de la anulación (Requerido)") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = StatusAlertContent)
            ) {
                Text("Anular")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun LedgerScreenPreview() {
    BTMContabilidadTheme {
        LedgerScreen()
    }
}

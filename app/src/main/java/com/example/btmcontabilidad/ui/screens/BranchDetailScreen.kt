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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btmcontabilidad.data.repository.MoneyDeliveryEntry
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.Collection
import com.example.btmcontabilidad.domain.model.LedgerEntry
import com.example.btmcontabilidad.domain.model.WeeklySettlement
import com.example.btmcontabilidad.domain.model.isCollection
import com.example.btmcontabilidad.domain.model.isMoneyDelivery
import com.example.btmcontabilidad.domain.model.isNegativeMovement
import com.example.btmcontabilidad.ui.components.ReceiptData
import com.example.btmcontabilidad.ui.components.ReceiptDialog
import com.example.btmcontabilidad.ui.components.WhatsAppDarkGreen
import com.example.btmcontabilidad.ui.components.WhatsAppGreen
import com.example.btmcontabilidad.ui.theme.BTMContabilidadTheme
import com.example.btmcontabilidad.ui.theme.DeepNavy
import com.example.btmcontabilidad.ui.theme.PrimaryBlue
import com.example.btmcontabilidad.ui.theme.StatusAlertBg
import com.example.btmcontabilidad.ui.theme.StatusAlertContent
import com.example.btmcontabilidad.ui.theme.StatusNeutralBg
import com.example.btmcontabilidad.ui.theme.StatusNeutralContent
import com.example.btmcontabilidad.ui.theme.StatusPendingBg
import com.example.btmcontabilidad.ui.theme.StatusPendingContent
import com.example.btmcontabilidad.ui.theme.StatusReadyBg
import com.example.btmcontabilidad.ui.theme.StatusReadyContent
import com.example.btmcontabilidad.ui.viewmodel.BranchDetailViewModel
import com.example.btmcontabilidad.ui.viewmodel.BranchHistoryTab
import com.example.btmcontabilidad.util.ReceiptManager
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchDetailScreen(
    branchId: String,
    refreshKey: Long = 0L,
    viewModel: BranchDetailViewModel = viewModel(key = "branch_detail_${branchId}_$refreshKey"),
    onNavigateBack: () -> Unit = {},
    onEdit: (String) -> Unit = {},
    onDeleted: () -> Unit = {},
    onRegisterCollection: (String) -> Unit = {},
    onRegisterAdvance: (String) -> Unit = {},
    onRegisterWeeklySettlement: (String, String) -> Unit = { _, _ -> },
    onTransferToBranch: (String) -> Unit = {},
    onRegisterManualResult: (String, String) -> Unit = { _, _ -> },
    onRegisterMoneyDelivery: (String, String) -> Unit = { _, _ -> },
    onRegisterCommission: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showAssignCollectorDialog by remember { mutableStateOf(false) }
    var activeReceiptData by remember { mutableStateOf<ReceiptData?>(null) }
    var selectedEntryToReverse by remember { mutableStateOf<LedgerEntry?>(null) }
    var activeCollectionToEdit by remember { mutableStateOf<Collection?>(null) }

    LaunchedEffect(branchId, refreshKey) {
        viewModel.loadBranch(branchId)
    }
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onDeleted()
    }
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

    // Modal para editar recibo de pago
    activeCollectionToEdit?.let { col ->
        com.example.btmcontabilidad.ui.components.EditCollectionDialog(
            collection = col,
            branchName = uiState.branch?.let { "${it.code} - ${it.name}" } ?: "",
            onDismiss = { activeCollectionToEdit = null },
            onConfirm = { updated ->
                activeCollectionToEdit = null
                viewModel.updateCollection(updated)
            }
        )
    }

    // Modal de Recibo Digital interactivo con WhatsApp
    activeReceiptData?.let { receipt ->
        ReceiptDialog(
            receipt = receipt,
            onDismiss = { activeReceiptData = null }
        )
    }

    // Modal de Reversión / Anulación de Transacción
    selectedEntryToReverse?.let { entry ->
        ReversalDialog(
            entry = entry,
            onDismiss = { selectedEntryToReverse = null },
            onConfirm = { reason ->
                val id = entry.id
                selectedEntryToReverse = null
                viewModel.reverseTransaction(id, reason)
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.branch?.name ?: "Detalle de Banca",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    uiState.branch?.let { branch ->
                        val context = LocalContext.current
                        IconButton(
                            onClick = {
                                val statementText = ReceiptManager.buildBranchStatementText(branch)
                                ReceiptManager.shareViaWhatsApp(
                                    context = context,
                                    phone = branch.ownerPhone ?: branch.phone,
                                    messageText = statementText
                                )
                            },
                            enabled = !uiState.isDeleting
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Enviar estado de cuenta por WhatsApp",
                                tint = WhatsAppDarkGreen
                            )
                        }
                        if (uiState.isAdmin) {
                            IconButton(onClick = { showAssignCollectorDialog = true }, enabled = !uiState.isDeleting) {
                                Icon(Icons.Default.Person, contentDescription = "Asignar cobrador")
                            }
                            IconButton(onClick = { onEdit(branch.id) }, enabled = !uiState.isDeleting) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar banca")
                            }
                            IconButton(onClick = { showDeleteConfirmation = true }, enabled = !uiState.isDeleting) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar banca")
                            }
                        }
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
        } else if (uiState.branch == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(uiState.errorMessage ?: "Banca no encontrada")
            }
        } else {
            val branch = uiState.branch!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                uiState.errorMessage?.let { message ->
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Branch Header Card con balance y acciones rápidas
                item {
                    BranchDetailHeaderCard(
                        branch = branch,
                        isAdmin = uiState.isAdmin,
                        onEditBranch = { onEdit(branch.id) },
                        onAssignCollector = { showAssignCollectorDialog = true },
                        onRegisterCollection = { onRegisterCollection(branch.id) },
                        onRegisterAdvance = { onRegisterAdvance(branch.id) },
                        onRegisterWeeklySettlement = { onRegisterWeeklySettlement(branch.id, branch.currentBalance.toPlainString()) },
                        onTransferToBranch = { onTransferToBranch(branch.id) },
                        onRegisterManualResult = { onRegisterManualResult(branch.id, branch.currentBalance.toPlainString()) },
                        onRegisterMoneyDelivery = { onRegisterMoneyDelivery(branch.id, branch.currentBalance.abs().toPlainString()) },
                        onRegisterCommission = { onRegisterCommission(branch.id) }
                    )
                }

                // Título de la Sección de Historial
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Historial de la Banca",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (uiState.isAdmin && uiState.selectedTab == BranchHistoryTab.SETTLEMENTS) {
                            OutlinedButton(
                                onClick = { onRegisterWeeklySettlement(branch.id, branch.currentBalance.toPlainString()) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nuevo Cuadre", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // Filtros por pestaña: [ Todos | Cobros | Entregas de dinero | Cuadres ]
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.selectedTab == BranchHistoryTab.ALL,
                                onClick = { viewModel.setHistoryTab(BranchHistoryTab.ALL) },
                                label = { Text("Todos (${uiState.statementEntries.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.selectedTab == BranchHistoryTab.COLLECTIONS,
                                onClick = { viewModel.setHistoryTab(BranchHistoryTab.COLLECTIONS) },
                                label = { Text("Cobros (${uiState.recentCollections.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StatusReadyContent,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        if (uiState.isAdmin) {
                            item {
                                FilterChip(
                                    selected = uiState.selectedTab == BranchHistoryTab.DELIVERIES,
                                    onClick = { viewModel.setHistoryTab(BranchHistoryTab.DELIVERIES) },
                                    label = { Text("Entregas (${uiState.moneyDeliveries.size})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StatusAlertContent,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = uiState.selectedTab == BranchHistoryTab.SETTLEMENTS,
                                    onClick = { viewModel.setHistoryTab(BranchHistoryTab.SETTLEMENTS) },
                                    label = { Text("Cuadres (${uiState.weeklySettlements.size})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DeepNavy,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // Contenido según la pestaña seleccionada
                when (uiState.selectedTab) {
                    BranchHistoryTab.ALL -> {
                        val visibleEntries = uiState.statementEntries
                        if (visibleEntries.isEmpty()) {
                            if (uiState.recentCollections.isNotEmpty()) {
                                items(items = uiState.recentCollections, key = { it.id }) { collection ->
                                    CollectionHistoryCard(
                                        collection = collection,
                                        branch = branch,
                                        onOpenReceipt = {
                                            val receiptText = ReceiptManager.buildCollectionReceiptText(
                                                collectionId = collection.id,
                                                branch = branch,
                                                amount = collection.amount,
                                                paymentMethod = collection.paymentMethod,
                                                reference = collection.reference,
                                                notes = collection.notes,
                                                previousBalance = BigDecimal.ZERO,
                                                newBalance = BigDecimal.ZERO
                                            )
                                            activeReceiptData = ReceiptData(
                                                title = "COMPROBANTE DE COBRO",
                                                receiptId = collection.id,
                                                amount = collection.amount,
                                                branch = branch,
                                                recipientPhone = branch.ownerPhone ?: branch.phone,
                                                previousBalance = BigDecimal.ZERO,
                                                newBalance = BigDecimal.ZERO,
                                                concept = "Cobro registrado",
                                                fullReceiptText = receiptText
                                            )
                                        }
                                    )
                                }
                            } else {
                                item {
                                    EmptyHistoryCard(if (uiState.isAdmin) "No hay movimientos registrados para esta banca" else "No hay movimientos ni cobros registrados para esta banca")
                                }
                            }
                        } else {
                            items(items = visibleEntries, key = { it.id }) { entry ->
                                val matchingCollection = if (entry.isCollection()) uiState.recentCollections.firstOrNull { it.id == entry.sourceId || it.id == entry.id } else null
                                val onEditCollection = if (uiState.isAdmin && matchingCollection != null) {
                                    { activeCollectionToEdit = matchingCollection }
                                } else null

                                StatementEntryCard(
                                    entry = entry,
                                    isAdmin = uiState.isAdmin,
                                    onOpenReceipt = {
                                        val receiptText = ReceiptManager.buildLedgerReceiptText(entry, branch)
                                        activeReceiptData = ReceiptData(
                                            title = "COMPROBANTE DE MOVIMIENTO",
                                            receiptId = entry.id,
                                            amount = entry.signedAmount.abs(),
                                            branch = branch,
                                            recipientPhone = branch.ownerPhone ?: branch.phone,
                                            previousBalance = entry.balanceBefore,
                                            newBalance = entry.balanceAfter,
                                            concept = entry.description,
                                            fullReceiptText = receiptText
                                        )
                                    },
                                    onEditClicked = onEditCollection,
                                    onReverseClicked = if (uiState.isAdmin && entry.reversalOfEntryId == null && !entry.description.startsWith("Reverso:")) {
                                        { selectedEntryToReverse = entry }
                                    } else null
                                )
                            }
                        }
                    }

                    BranchHistoryTab.COLLECTIONS -> {
                        if (uiState.recentCollections.isEmpty()) {
                            item {
                                EmptyHistoryCard("No hay cobros registrados para esta banca")
                            }
                        } else {
                            items(items = uiState.recentCollections, key = { it.id }) { collection ->
                                val matchingEntry = uiState.statementEntries.firstOrNull { it.sourceId == collection.id || it.id == collection.id }
                                CollectionHistoryCard(
                                    collection = collection,
                                    branch = branch,
                                    onOpenReceipt = {
                                        val receiptText = ReceiptManager.buildCollectionReceiptText(
                                            collectionId = collection.id,
                                            branch = branch,
                                            amount = collection.amount,
                                            paymentMethod = collection.paymentMethod,
                                            reference = collection.reference,
                                            notes = collection.notes,
                                            previousBalance = BigDecimal.ZERO,
                                            newBalance = BigDecimal.ZERO
                                        )
                                        activeReceiptData = ReceiptData(
                                            title = "COMPROBANTE DE COBRO",
                                            receiptId = collection.id,
                                            amount = collection.amount,
                                            branch = branch,
                                            recipientPhone = branch.ownerPhone ?: branch.phone,
                                            paymentMethod = collection.paymentMethod.label,
                                            concept = "Cobro de balance",
                                            reference = collection.reference,
                                            fullReceiptText = receiptText
                                        )
                                    },
                                    onEditClicked = if (uiState.isAdmin) {
                                        { activeCollectionToEdit = collection }
                                    } else null,
                                    onReverseClicked = if (uiState.isAdmin && matchingEntry != null && matchingEntry.reversalOfEntryId == null && !matchingEntry.description.startsWith("Reverso:")) {
                                        { selectedEntryToReverse = matchingEntry }
                                    } else null
                                )
                            }
                        }
                    }

                    BranchHistoryTab.DELIVERIES -> {
                        if (uiState.moneyDeliveries.isEmpty()) {
                            item {
                                EmptyHistoryCard("No hay entregas de dinero registradas para esta banca")
                            }
                        } else {
                            items(items = uiState.moneyDeliveries, key = { it.id }) { delivery ->
                                val matchingEntry = uiState.statementEntries.firstOrNull { it.sourceId == delivery.id.toString() || it.id == delivery.id.toString() }
                                MoneyDeliveryHistoryCard(
                                    delivery = delivery,
                                    branch = branch,
                                    onOpenReceipt = {
                                        val receiptText = ReceiptManager.buildMoneyDeliveryReceiptText(
                                            deliveryId = delivery.id.toString(),
                                            branch = branch,
                                            amount = delivery.deliveredAmount,
                                            reason = delivery.reason,
                                            cashBoxName = "Caja Principal",
                                            notes = delivery.notes,
                                            previousBalance = BigDecimal.ZERO,
                                            newBalance = delivery.branchBalanceAfter
                                        )
                                        activeReceiptData = ReceiptData(
                                            title = "COMPROBANTE DE ENTREGA DE DINERO",
                                            receiptId = delivery.id.toString(),
                                            amount = delivery.deliveredAmount,
                                            branch = branch,
                                            recipientPhone = branch.ownerPhone ?: branch.phone,
                                            newBalance = delivery.branchBalanceAfter,
                                            concept = delivery.reason,
                                            fullReceiptText = receiptText
                                        )
                                    },
                                    onReverseClicked = if (uiState.isAdmin && matchingEntry != null && matchingEntry.reversalOfEntryId == null && !matchingEntry.description.startsWith("Reverso:")) {
                                        { selectedEntryToReverse = matchingEntry }
                                    } else null
                                )
                            }
                        }
                    }

                    BranchHistoryTab.SETTLEMENTS -> {
                        if (uiState.weeklySettlements.isEmpty()) {
                            item {
                                EmptyHistoryCard("No hay cuadres semanales registrados.")
                            }
                        } else {
                            items(items = uiState.weeklySettlements, key = { it.id }) { settlement ->
                                WeeklySettlementCard(settlement)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isDeleting) showDeleteConfirmation = false },
            title = { Text("Eliminar banca") },
            text = { Text("Esta acción eliminará la banca y no se puede deshacer.") },
            confirmButton = {
                Button(onClick = viewModel::deleteBranch, enabled = !uiState.isDeleting) {
                    if (uiState.isDeleting) CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    ) else Text("Eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = false }, enabled = !uiState.isDeleting) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showAssignCollectorDialog) {
        uiState.branch?.let { branch ->
            com.example.btmcontabilidad.ui.components.AssignCollectorDialog(
                branchName = branch.name,
                branchCode = branch.code,
                currentCollectorId = branch.collectorUserId,
                collectors = uiState.collectors,
                isLoading = uiState.isLoading,
                onDismiss = { showAssignCollectorDialog = false },
                onConfirm = { collectorId ->
                    showAssignCollectorDialog = false
                    viewModel.assignCollector(collectorId)
                }
            )
        }
    }
}

@Composable
fun BranchDetailHeaderCard(
    branch: Branch,
    isAdmin: Boolean = true,
    onEditBranch: () -> Unit = {},
    onAssignCollector: () -> Unit = {},
    onRegisterCollection: () -> Unit,
    onRegisterAdvance: () -> Unit,
    onRegisterWeeklySettlement: () -> Unit,
    onTransferToBranch: () -> Unit,
    onRegisterManualResult: () -> Unit = {},
    onRegisterMoneyDelivery: () -> Unit = {},
    onRegisterCommission: () -> Unit = {}
) {
    val roundedBalance = FinancialCalculator.roundMoney(branch.currentBalance)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavy),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = branch.code.take(2).uppercase(),
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Column {
                        Text(
                            text = branch.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Código: ${branch.code}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (branch.status.name.equals("ACTIVE", ignoreCase = true)) StatusReadyBg else StatusNeutralBg
                ) {
                    Text(
                        text = if (branch.status.name.equals("ACTIVE", ignoreCase = true)) "Activa" else "Inactiva",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (branch.status.name.equals("ACTIVE", ignoreCase = true)) StatusReadyContent else StatusNeutralContent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Información y Parámetros de la Banca (con botón directo de edición)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isAdmin) Modifier.clickable { onEditBranch() } else Modifier)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📋 Datos de la Banca",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        if (isAdmin) {
                            Surface(
                                modifier = Modifier.clickable { onEditBranch() },
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryBlue
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar campos",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Editar Datos",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                    // Fila 1: Teléfono Banca & Teléfono/WhatsApp Dueño
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("📞 Teléfono Banca", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
                            Text(
                                text = branch.phone?.takeIf { it.isNotBlank() } ?: "No registrado",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("💬 WhatsApp Dueño", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
                            Text(
                                text = branch.ownerPhone?.takeIf { it.isNotBlank() } ?: "No registrado",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (!branch.ownerPhone.isNullOrBlank()) WhatsAppGreen else Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Fila 2: Dueño & Encargado / Operador
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("👤 Dueño", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
                            Text(
                                text = branch.ownerName?.takeIf { it.isNotBlank() } ?: "No registrado",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🧑‍💼 Encargado / Op.", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
                            Text(
                                text = branch.operatorName.takeIf { it.isNotBlank() } ?: "No registrado",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Fila 3: Ruta & Comisión
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("📍 Ruta", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
                            Text(
                                text = branch.route.takeIf { it.isNotBlank() } ?: "General",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🏷️ Comisión", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
                            Text(
                                text = if (branch.commissionRate > BigDecimal.ZERO) "${branch.commissionRate.stripTrailingZeros().toPlainString()}%" else "0%",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Fila 4: Dirección / Descripción (si existe)
                    if (!branch.description.isNullOrBlank()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("🏠 Dirección / Notas", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
                            Text(
                                text = branch.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    // Fila 5: Cobrador asignado
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("💼 Cobrador:", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
                            Text(
                                text = branch.collectorName ?: "Sin asignar",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (isAdmin) {
                            Surface(
                                modifier = Modifier.clickable { onAssignCollector() },
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (branch.collectorUserId == null) "Asignar" else "Cambiar",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Balance Section
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "BALANCE ACTUAL DE CUENTA CORRIENTE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = FinancialCalculator.formatCurrency(roundedBalance),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                val (bg, fg, label) = when {
                    roundedBalance > BigDecimal.ZERO -> Triple(StatusPendingBg, StatusPendingContent, "🟢 Por recoger (Lunes)")
                    roundedBalance < BigDecimal.ZERO -> Triple(StatusAlertBg, StatusAlertContent, "🔴 Requiere dinero (Déficit)")
                    else -> Triple(StatusNeutralBg, StatusNeutralContent, "⚪ Cuenta al día")
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bg
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = fg,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    val context = LocalContext.current
                    Surface(
                        modifier = Modifier.clickable {
                            val statementText = ReceiptManager.buildBranchStatementText(branch)
                            ReceiptManager.shareViaWhatsApp(
                                context = context,
                                phone = branch.ownerPhone ?: branch.phone,
                                messageText = statementText
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = WhatsAppGreen
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Enviar por WhatsApp",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "WhatsApp",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (isAdmin) {
                // Primary Action: Registrar Resultado Manual (Solo Admin)
                Button(
                    onClick = onRegisterManualResult,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Registrar Resultado Manual", fontWeight = FontWeight.Bold)
                }

                // Quick actions row (Admin)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRegisterCollection,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue.copy(alpha = 0.85f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp).size(18.dp)
                        )
                        Text("Cobro", fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onRegisterMoneyDelivery,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalAtm,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp).size(18.dp)
                        )
                        Text("Entrega", fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    Button(
                        onClick = onRegisterCommission,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) {
                        Text("🤝 Comisión", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                if (roundedBalance < BigDecimal.ZERO) {
                    Button(
                        onClick = onRegisterMoneyDelivery,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusAlertContent)
                    ) {
                        Icon(Icons.Default.LocalAtm, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Entregar dinero a la banca", fontWeight = FontWeight.Bold)
                    }
                }

                // Botón destacado para editar la información de la banca
                OutlinedButton(
                    onClick = onEditBranch,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("✏️ Editar Información de la Banca", fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                // Cobrador: Solo botón prominente para cobrar
                Button(
                    onClick = onRegisterCollection,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusReadyContent)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp).size(20.dp)
                    )
                    Text("Registrar Cobro a Esta Banca", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CollectionHistoryCard(
    collection: Collection,
    branch: Branch,
    onOpenReceipt: () -> Unit,
    onEditClicked: (() -> Unit)? = null,
    onReverseClicked: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenReceipt() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(StatusReadyContent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = StatusReadyContent
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Cobro Realizado",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryBlue.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = collection.paymentMethod.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    val dateClean = collection.businessDate.substringBefore('T')
                    val refText = if (!collection.reference.isNullOrBlank()) " • Ref: ${collection.reference}" else ""
                    val notesText = if (!collection.notes.isNullOrBlank()) " • ${collection.notes}" else ""
                    Text(
                        text = "📅 $dateClean$refText$notesText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "+ ${FinancialCalculator.formatCurrency(collection.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StatusReadyContent
                )

                IconButton(
                    onClick = onOpenReceipt,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Enviar por WhatsApp",
                        tint = WhatsAppDarkGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (onEditClicked != null) {
                    IconButton(
                        onClick = onEditClicked,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Recibo de Pago",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onReverseClicked != null) {
                    IconButton(
                        onClick = onReverseClicked,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Anular / Arreglar Transacción",
                            tint = StatusAlertContent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MoneyDeliveryHistoryCard(
    delivery: MoneyDeliveryEntry,
    branch: Branch,
    onOpenReceipt: () -> Unit,
    onReverseClicked: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenReceipt() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(StatusAlertContent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalAtm,
                        contentDescription = null,
                        tint = StatusAlertContent
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (delivery.reason.isNotBlank()) delivery.reason else "Entrega de Dinero",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val dateClean = delivery.businessDate.substringBefore('T')
                    val notesText = if (!delivery.notes.isNullOrBlank()) " • ${delivery.notes}" else ""
                    Text(
                        text = "📅 $dateClean$notesText • Quedó en: ${FinancialCalculator.formatCurrency(delivery.branchBalanceAfter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = FinancialCalculator.formatCurrency(delivery.deliveredAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StatusAlertContent
                )

                IconButton(
                    onClick = onOpenReceipt,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Enviar por WhatsApp",
                        tint = WhatsAppDarkGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (onReverseClicked != null) {
                    IconButton(
                        onClick = onReverseClicked,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Anular / Arreglar Transacción",
                            tint = StatusAlertContent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklySettlementCard(settlement: WeeklySettlement) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("${settlement.weekStart} al ${settlement.weekEnd}", fontWeight = FontWeight.Bold)
            Text("Ventas ${FinancialCalculator.formatCurrency(settlement.salesAmount)} · Premios ${FinancialCalculator.formatCurrency(settlement.prizesAmount)}")
            Text("Comisión ${settlement.commissionRate}%: ${FinancialCalculator.formatCurrency(settlement.commissionAmount)}")
            Text("Entregado ${FinancialCalculator.formatCurrency(settlement.cashDeliveredAmount)}")
            Text("Balance semanal: ${FinancialCalculator.formatCurrency(settlement.weeklyBalance)}", fontWeight = FontWeight.Bold)
            Text("Saldo: ${FinancialCalculator.formatCurrency(settlement.balanceBefore)} → ${FinancialCalculator.formatCurrency(settlement.balanceAfter)}", style = MaterialTheme.typography.bodySmall)
            Text(
                text = when (settlement.status) {
                    "partially_paid" -> "Estado: pago parcial"
                    "paid" -> "Estado: pagado"
                    "negative_balance" -> "Estado: balance negativo"
                    "compensated" -> "Estado: compensado"
                    "settled" -> "Estado: saldado"
                    else -> "Estado: pendiente"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun StatementEntryCard(
    entry: LedgerEntry,
    isAdmin: Boolean = true,
    onOpenReceipt: () -> Unit = {},
    onEditClicked: (() -> Unit)? = null,
    onReverseClicked: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenReceipt() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isNegative = entry.isNegativeMovement()
            val isMoneyDelivery = entry.isMoneyDelivery()
            val isCollection = entry.isCollection()
            val isReversed = entry.reversalOfEntryId != null || entry.description.startsWith("Reverso:")

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (icon, tint) = when {
                    isCollection -> Pair(Icons.Default.Payments, StatusReadyContent)
                    isMoneyDelivery -> Pair(Icons.Default.LocalAtm, StatusAlertContent)
                    isNegative -> Pair(Icons.Default.LocalAtm, StatusAlertContent)
                    else -> Pair(Icons.Default.Add, StatusReadyContent)
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = tint)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isMoneyDelivery) {
                        Text(
                            text = if (isAdmin) "🏦 Salida de Caja Chica para Premios" else "🏦 Entrega de Efectivo para Premios",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusAlertContent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    val creatorText = if (entry.createdBy.isNotBlank()) " • 👤 ${entry.createdBy}" else ""
                    Text(
                        text = "${entry.businessDate.substringBefore('T')}$creatorText • Había: ${FinancialCalculator.formatCurrency(entry.balanceBefore)} → Quedó: ${FinancialCalculator.formatCurrency(entry.balanceAfter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val amountColor = if (isNegative) StatusAlertContent else StatusReadyContent
                val prefix = if (isNegative) "- " else "+ "

                Text(
                    text = "$prefix${FinancialCalculator.formatCurrency(entry.signedAmount.abs())}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    maxLines = 1
                )

                IconButton(
                    onClick = onOpenReceipt,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Ver y Enviar Comprobante",
                        tint = WhatsAppDarkGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (onEditClicked != null && !isReversed) {
                    IconButton(
                        onClick = onEditClicked,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Recibo de Pago",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onReverseClicked != null && !isReversed) {
                    IconButton(
                        onClick = onReverseClicked,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Anular / Arreglar Transacción",
                            tint = StatusAlertContent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun BranchDetailScreenPreview() {
    BTMContabilidadTheme {
        BranchDetailScreen(branchId = "B001")
    }
}

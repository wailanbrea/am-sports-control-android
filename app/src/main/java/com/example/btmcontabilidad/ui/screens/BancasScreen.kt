package com.example.btmcontabilidad.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.BranchStatus
import com.example.btmcontabilidad.ui.components.AssignCollectorDialog
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
import com.example.btmcontabilidad.ui.viewmodel.BranchFilterTab
import com.example.btmcontabilidad.ui.viewmodel.BranchesViewModel
import com.example.btmcontabilidad.util.ReceiptManager
import java.math.BigDecimal

private val WhatsAppDarkGreen = Color(0xFF075E54)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BancasScreen(
    viewModel: BranchesViewModel = viewModel(),
    onBranchSelected: (String) -> Unit = {},
    onCreateBranch: () -> Unit = {},
    onEditBranch: (String) -> Unit = {},
    onRegisterCollection: (String) -> Unit = {},
    onRegisterWeeklySettlement: (String, String) -> Unit = { _, _ -> },
    onTransferToBranch: (String) -> Unit = {},
    onRegisterCommission: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var branchToAssignCollector by remember { mutableStateOf<Branch?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        floatingActionButton = {
            if (uiState.isAdmin) {
                FloatingActionButton(onClick = onCreateBranch) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva banca")
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bancas & Operadores",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
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
                // Summary Bar
                item {
                    BancasSummaryCard(
                        totalPorCobrar = uiState.totalPorCobrar,
                        totalPorEnviar = uiState.totalPorEnviar,
                        netBalance = uiState.netBalance,
                        isAdmin = uiState.isAdmin
                    )
                }

                // Search field
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar banca, operador o ruta...") },
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

                // Filter Tabs
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(BranchFilterTab.entries.toTypedArray()) { tab ->
                            val count = when (tab) {
                                BranchFilterTab.TODAS -> uiState.branches.size
                                BranchFilterTab.POR_COBRAR -> uiState.countPorCobrar
                                BranchFilterTab.POR_ENVIAR -> uiState.countPorEnviar
                                BranchFilterTab.SALDADAS -> uiState.countSaldadas
                                BranchFilterTab.INACTIVAS -> uiState.countInactivas
                            }
                            FilterChip(
                                selected = uiState.selectedTab == tab,
                                onClick = { viewModel.setFilterTab(tab) },
                                label = { Text("${tab.label} ($count)") }
                            )
                        }
                    }
                }

                // Branch Cards List
                items(
                    items = uiState.filteredBranches,
                    key = { it.id }
                ) { branch ->
                    BranchListItemCard(
                        branch = branch,
                        isAdmin = uiState.isAdmin,
                        onClick = { onBranchSelected(branch.id) },
                        onEditBranch = { onEditBranch(branch.id) },
                        onAssignCollector = { branchToAssignCollector = branch },
                        onRegisterCollection = { onRegisterCollection(branch.id) },
                        onRegisterWeeklySettlement = {
                            onRegisterWeeklySettlement(branch.id, branch.currentBalance.toPlainString())
                        },
                        onTransferToBranch = { onTransferToBranch(branch.id) },
                        onRegisterCommission = { onRegisterCommission(branch.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    branchToAssignCollector?.let { branch ->
        AssignCollectorDialog(
            branchName = branch.name,
            branchCode = branch.code,
            currentCollectorId = branch.collectorUserId,
            collectors = uiState.collectors,
            isLoading = uiState.isLoading,
            onDismiss = { branchToAssignCollector = null },
            onConfirm = { collectorId ->
                viewModel.assignCollector(branch.id, collectorId)
                branchToAssignCollector = null
            }
        )
    }
}

@Composable
fun BancasSummaryCard(
    totalPorCobrar: BigDecimal,
    totalPorEnviar: BigDecimal,
    netBalance: BigDecimal,
    isAdmin: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavy)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "ESTADO GENERAL DE BANCAS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )

            if (isAdmin) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Por cobrar (Ganan)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = FinancialCalculator.formatCurrency(totalPorCobrar),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Por enviar (Pierden)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = FinancialCalculator.formatCurrency(totalPorEnviar),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusAlertContent
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Balance neto total:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = FinancialCalculator.formatCurrency(netBalance),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (netBalance >= BigDecimal.ZERO) Color.White else StatusAlertContent
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Suma de deudas y préstamos",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                        Text(
                            text = FinancialCalculator.formatCurrency(totalPorCobrar),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Bancas con Deuda",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BranchListItemCard(
    branch: Branch,
    isAdmin: Boolean = true,
    onClick: () -> Unit,
    onEditBranch: () -> Unit = {},
    onAssignCollector: () -> Unit = {},
    onRegisterCollection: () -> Unit,
    onRegisterWeeklySettlement: () -> Unit,
    onTransferToBranch: () -> Unit,
    onRegisterCommission: () -> Unit = {}
) {
    val roundedBalance = FinancialCalculator.roundMoney(branch.currentBalance)
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DeepNavy
                    ) {
                        Text(
                            text = branch.code,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = branch.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (branch.commissionRate > BigDecimal.ZERO) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PrimaryBlue.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${branch.commissionRate.stripTrailingZeros().toPlainString()}% com.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryBlue,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = buildString {
                                append("${branch.operatorName} • ${branch.route}")
                                if (!branch.collectorName.isNullOrBlank()) {
                                    append(" • 👤 ${branch.collectorName}")
                                } else if (isAdmin) {
                                    append(" • 👤 Sin cobrador")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Status chip
                    val (bg, fg, statusLabel) = when {
                        branch.status == BranchStatus.INACTIVE -> Triple(StatusNeutralBg, StatusNeutralContent, "Inactiva")
                        roundedBalance > BigDecimal.ZERO -> Triple(StatusPendingBg, StatusPendingContent, "Por cobrar")
                        roundedBalance < BigDecimal.ZERO -> Triple(StatusAlertBg, StatusAlertContent, "Por enviar")
                        else -> Triple(StatusNeutralBg, StatusNeutralContent, "Saldada")
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bg
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = fg,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (isAdmin) {
                        IconButton(
                            onClick = onEditBranch,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar banca",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Contraer" else "Desplegar opciones e historial",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Balance Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Balance actual:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val balanceColor = when {
                    roundedBalance > BigDecimal.ZERO -> PrimaryBlue
                    roundedBalance < BigDecimal.ZERO -> StatusAlertContent
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Text(
                    text = FinancialCalculator.formatCurrency(roundedBalance),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRegisterCollection,
                    modifier = if (isAdmin) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Cobro", fontWeight = FontWeight.SemiBold)
                }

                if (isAdmin) {
                    OutlinedButton(
                        onClick = onRegisterWeeklySettlement,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalAtm,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Cuadre", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (isAdmin && roundedBalance < BigDecimal.ZERO) {
                OutlinedButton(
                    onClick = onTransferToBranch,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.LocalAtm, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Entregar dinero a banca", fontWeight = FontWeight.SemiBold)
                }
            }

            // Sección Desplegable (Acordeón de Historial y Opciones Rápidas)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Contact info if available
                    if (!branch.phone.isNullOrBlank() || !branch.ownerName.isNullOrBlank() || !branch.collectorName.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📞 ${branch.phone ?: "Sin tel."} • 👤 ${branch.ownerName ?: branch.operatorName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!branch.collectorName.isNullOrBlank() || isAdmin) {
                            Text(
                                text = "💼 Cobrador: ${branch.collectorName ?: "Sin asignar"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Botón principal: Ver Historial y Detalle Completo
                    Button(
                        onClick = onClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepNavy)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver Historial y Detalle Completo", fontWeight = FontWeight.Bold)
                    }

                    if (isAdmin) {
                        OutlinedButton(
                            onClick = onEditBranch,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("✏️ Editar Datos de la Banca", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Botón secundario: Enviar Estado de Cuenta por WhatsApp
                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            val statementText = ReceiptManager.buildBranchStatementText(branch)
                            ReceiptManager.shareViaWhatsApp(
                                context = context,
                                phone = branch.ownerPhone ?: branch.phone,
                                messageText = statementText
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppDarkGreen)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = WhatsAppDarkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enviar Estado de Cuenta por WhatsApp", fontWeight = FontWeight.SemiBold)
                    }

                    if (isAdmin) {
                        // Botón para asignar cobrador directamente
                        Button(
                            onClick = onAssignCollector,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("👤 Asignar / Cambiar Cobrador", fontWeight = FontWeight.Bold)
                        }

                        // Botón para editar la banca
                        OutlinedButton(
                            onClick = onEditBranch,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("✏️ Editar Información de la Banca", fontWeight = FontWeight.Bold)
                        }

                        // Botón terciario: Registrar Gasto de Comisión
                        OutlinedButton(
                            onClick = onRegisterCommission,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706))
                        ) {
                            Text("🤝 Registrar Gasto por Comisión", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

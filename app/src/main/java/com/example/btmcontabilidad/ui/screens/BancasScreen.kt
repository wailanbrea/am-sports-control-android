package com.example.btmcontabilidad.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.BranchStatus
import com.example.btmcontabilidad.ui.theme.BTMContabilidadTheme
import com.example.btmcontabilidad.ui.theme.DeepNavy
import com.example.btmcontabilidad.ui.theme.PrimaryBlue
import com.example.btmcontabilidad.ui.theme.StatusAlertBg
import com.example.btmcontabilidad.ui.theme.StatusAlertContent
import com.example.btmcontabilidad.ui.theme.StatusNeutralBg
import com.example.btmcontabilidad.ui.theme.StatusNeutralContent
import com.example.btmcontabilidad.ui.theme.StatusPendingBg
import com.example.btmcontabilidad.ui.theme.StatusPendingBorder
import com.example.btmcontabilidad.ui.theme.StatusPendingContent
import com.example.btmcontabilidad.ui.theme.StatusReadyBg
import com.example.btmcontabilidad.ui.theme.StatusReadyBorder
import com.example.btmcontabilidad.ui.theme.StatusReadyContent
import com.example.btmcontabilidad.ui.viewmodel.BranchFilterTab
import com.example.btmcontabilidad.ui.viewmodel.BranchesViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BancasScreen(
    viewModel: BranchesViewModel = viewModel(),
    onBranchSelected: (String) -> Unit = {},
    onCreateBranch: () -> Unit = {},
    onRegisterCollection: (String) -> Unit = {},
    onRegisterWeeklySettlement: (String, String) -> Unit = { _, _ -> },
    onTransferToBranch: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateBranch) {
                Icon(Icons.Default.Add, contentDescription = "Nueva banca")
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
                        netBalance = uiState.netBalance
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
                        onClick = { onBranchSelected(branch.id) },
                        onRegisterCollection = { onRegisterCollection(branch.id) },
                        onRegisterWeeklySettlement = {
                            onRegisterWeeklySettlement(branch.id, branch.currentBalance.toPlainString())
                        },
                        onTransferToBranch = { onTransferToBranch(branch.id) }
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
fun BancasSummaryCard(
    totalPorCobrar: BigDecimal,
    totalPorEnviar: BigDecimal,
    netBalance: BigDecimal
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Por Cobrar",
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

                Column {
                    Text(
                        text = "Por Enviar",
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

                Column {
                    Text(
                        text = "Posición Neta",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = FinancialCalculator.formatCurrency(netBalance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusReadyBorder
                    )
                }
            }
        }
    }
}

@Composable
fun BranchListItemCard(
    branch: Branch,
    onClick: () -> Unit,
    onRegisterCollection: () -> Unit,
    onRegisterWeeklySettlement: () -> Unit,
    onTransferToBranch: () -> Unit
) {
    val roundedBalance = FinancialCalculator.roundMoney(branch.currentBalance)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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

                    Column {
                        Text(
                            text = branch.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${branch.operatorName} • ${branch.route}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
                    modifier = Modifier.weight(1f),
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

            if (roundedBalance < BigDecimal.ZERO) {
                OutlinedButton(
                    onClick = onTransferToBranch,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.LocalAtm, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Entregar dinero a banca", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun BancasScreenPreview() {
    BTMContabilidadTheme {
        BancasScreen()
    }
}

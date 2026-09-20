package com.example.btmcontabilidad.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.LedgerEntry
import com.example.btmcontabilidad.domain.model.LedgerEntryType
import com.example.btmcontabilidad.domain.model.LedgerSourceType
import com.example.btmcontabilidad.ui.theme.BTMContabilidadTheme
import com.example.btmcontabilidad.ui.theme.DeepNavy
import com.example.btmcontabilidad.ui.theme.PrimaryBlue
import com.example.btmcontabilidad.ui.theme.StatusAlertBg
import com.example.btmcontabilidad.ui.theme.StatusAlertContent
import com.example.btmcontabilidad.ui.theme.StatusPendingBg
import com.example.btmcontabilidad.ui.theme.StatusPendingContent
import com.example.btmcontabilidad.ui.theme.StatusReadyBg
import com.example.btmcontabilidad.ui.theme.StatusReadyContent
import com.example.btmcontabilidad.ui.viewmodel.DashboardUiState
import com.example.btmcontabilidad.ui.viewmodel.DashboardViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onNavigateToRegisterCollection: () -> Unit = {},
    onNavigateToRegisterAdvance: () -> Unit = {},
    onNavigateToMoneyDelivery: () -> Unit = {},
    onNavigateToManualResult: () -> Unit = {},
    onNavigateToLedger: () -> Unit = {},
    onNavigateToBancas: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showNotifications by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "A & M Sports LLC",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Control de Bancas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showNotifications = true }) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notificaciones")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Perfil",
                            tint = DeepNavy,
                            modifier = Modifier.size(32.dp)
                        )
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
                // Top Metrics Cards Grid
                item {
                    TopMetricsSection(
                        uiState = uiState,
                        onNavigateToBancas = onNavigateToBancas
                    )
                }

                // Quick Action Buttons
                item {
                    QuickActionsSection(
                        onRegisterCollection = onNavigateToRegisterCollection,
                        onRegisterMoneyDelivery = onNavigateToMoneyDelivery,
                        onRegisterManualResult = onNavigateToManualResult
                    )
                }

                item {
                    ActivitySummaryCard(uiState = uiState)
                }

                // Recent Transactions Header & List
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Movimientos Recientes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onNavigateToLedger) {
                            Text("Ver todos", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(
                    items = uiState.recentTransactions,
                    key = { it.id }
                ) { entry ->
                    DashboardLedgerItemCard(entry = entry)
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showNotifications) {
        AlertDialog(
            onDismissRequest = { showNotifications = false },
            title = { Text("Notificaciones") },
            text = { Text("No hay notificaciones pendientes para esta sesión.") },
            confirmButton = {
                TextButton(onClick = { showNotifications = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun TopMetricsSection(
    uiState: DashboardUiState,
    onNavigateToBancas: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Main Receivable Card (Deep Navy)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToBancas() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepNavy),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL POR RECOGER",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${uiState.pendingBranchesCount} bancas",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = FinancialCalculator.formatCurrency(uiState.totalReceivable),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Total Por Enviar a Bancas Card (Premios / Déficit)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToBancas() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StatusAlertBg)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "POR ENVIAR (PREMIOS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusAlertContent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FinancialCalculator.formatCurrency(uiState.totalBranchCredit),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusAlertContent
                    )
                    Text(
                        text = "${uiState.creditBranchesCount} bancas",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusAlertContent.copy(alpha = 0.8f)
                    )
                }
            }

            // Net Position Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToBancas() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StatusReadyBg)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "POSICIÓN NETA",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusReadyContent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FinancialCalculator.formatCurrency(uiState.netPosition),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusReadyContent
                    )
                    Text(
                        text = "Exigible Neto",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusReadyContent.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    onRegisterCollection: () -> Unit,
    onRegisterMoneyDelivery: () -> Unit,
    onRegisterManualResult: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Acciones Rápidas",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton(
                label = "Cobrar",
                icon = Icons.Default.Payments,
                backgroundColor = StatusReadyContent,
                contentColor = Color.White,
                modifier = Modifier.weight(1f),
                onClick = onRegisterCollection
            )

            QuickActionButton(
                label = "Llevar Dinero",
                icon = Icons.Default.LocalAtm,
                backgroundColor = StatusAlertContent,
                contentColor = Color.White,
                modifier = Modifier.weight(1f),
                onClick = onRegisterMoneyDelivery
            )

            QuickActionButton(
                label = "Resultado",
                icon = Icons.Default.Add,
                backgroundColor = DeepNavy,
                contentColor = Color.White,
                modifier = Modifier.weight(1f),
                onClick = onRegisterManualResult
            )
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun ActivitySummaryCard(uiState: DashboardUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Resumen de actividad",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Cobrado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FinancialCalculator.formatCurrency(uiState.collectionsTotal),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusReadyContent
                    )
                }

                Column {
                    Text(
                        text = "Adelantos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FinancialCalculator.formatCurrency(uiState.advancesTotal),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusAlertContent
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardLedgerItemCard(entry: LedgerEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val (icon, tint) = when (entry.sourceType) {
                    LedgerSourceType.COLLECTION -> Pair(Icons.Default.Payments, StatusReadyContent)
                    LedgerSourceType.ADVANCE -> Pair(Icons.Default.LocalAtm, StatusAlertContent)
                    else -> Pair(Icons.Default.LocalAtm, PrimaryBlue)
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
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Banca: ${entry.branchId} • ${entry.businessDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val amountColor = if (entry.entryType == LedgerEntryType.CREDIT) StatusReadyContent else StatusAlertContent
            val prefix = if (entry.entryType == LedgerEntryType.CREDIT) "- " else "+ "

            Text(
                text = "$prefix${FinancialCalculator.formatCurrency(entry.signedAmount.abs())}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = amountColor,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.widthIn(min = 112.dp)
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun DashboardScreenPreview() {
    BTMContabilidadTheme {
        DashboardScreen()
    }
}

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.CashMovement
import com.example.btmcontabilidad.domain.model.CashMovementType
import com.example.btmcontabilidad.ui.theme.DeepNavy
import com.example.btmcontabilidad.ui.theme.PrimaryBlue
import com.example.btmcontabilidad.ui.theme.StatusAlertBg
import com.example.btmcontabilidad.ui.theme.StatusAlertContent
import com.example.btmcontabilidad.ui.theme.StatusReadyBg
import com.example.btmcontabilidad.ui.theme.StatusReadyContent
import com.example.btmcontabilidad.ui.viewmodel.CashBoxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashBoxScreen(
    initialBranchId: String? = null,
    viewModel: CashBoxViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var branchMenuExpanded by remember { mutableStateOf(false) }
    val selectedBranch = uiState.availableBranches.find { it.id == uiState.selectedBranchId }

    LaunchedEffect(initialBranchId) {
        initialBranchId?.let(viewModel::prepareBranchTransfer)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Caja menor",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar caja")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.cashBox == null) {
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    uiState.errorMessage?.let { MessageCard(it, StatusAlertBg, StatusAlertContent) }
                    uiState.successMessage?.let { MessageCard(it, StatusReadyBg, StatusReadyContent) }
                }

                item {
                    CashBalanceCard(
                        balance = uiState.cashBox?.currentBalance ?: java.math.BigDecimal.ZERO,
                        currencyCode = uiState.cashBox?.currencyCode ?: "USD",
                        isRefreshing = uiState.isLoading
                    )
                }

                item {
                    Text(
                        text = "Registrar movimiento",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(
                            items = CashMovementType.entries.toList(),
                            key = { it.name }
                        ) { type ->
                            FilterChip(
                                selected = uiState.selectedType == type,
                                onClick = { viewModel.selectType(type) },
                                label = { Text(type.label, maxLines = 1) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = type.icon(),
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.amountInput,
                                onValueChange = viewModel::updateAmount,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Monto") },
                                prefix = {
                                    Text(
                                        "${FinancialCalculator.currencySymbol()} ",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                isError = uiState.errorMessage != null && uiState.amountInput.isBlank()
                            )
                             OutlinedTextField(
                                 value = uiState.businessDateInput,
                                 onValueChange = {},
                                 modifier = Modifier.fillMaxWidth(),
                                 label = { Text("Fecha operativa (automática)") },
                                 readOnly = true,
                                 singleLine = true
                             )
                            OutlinedTextField(
                                value = uiState.reasonInput,
                                onValueChange = viewModel::updateReason,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Motivo") },
                                singleLine = true,
                                isError = uiState.errorMessage != null && uiState.reasonInput.isBlank()
                            )
                            OutlinedTextField(
                                value = uiState.referenceInput,
                                onValueChange = viewModel::updateReference,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Referencia (opcional)") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = uiState.notesInput,
                                onValueChange = viewModel::updateNotes,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Notas (opcional)") },
                                minLines = 2,
                                maxLines = 4
                            )

                            if (uiState.selectedType == CashMovementType.BRANCH_TRANSFER) {
                                ExposedDropdownMenuBox(
                                    expanded = branchMenuExpanded,
                                    onExpandedChange = { branchMenuExpanded = !branchMenuExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedBranch?.displayName() ?: "Seleccionar banca",
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                        label = { Text("Banca de destino") },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(branchMenuExpanded)
                                        },
                                        isError = uiState.errorMessage != null && selectedBranch == null,
                                        singleLine = true
                                    )
                                    ExposedDropdownMenu(
                                        expanded = branchMenuExpanded,
                                        onDismissRequest = { branchMenuExpanded = false }
                                    ) {
                                        uiState.availableBranches.forEach { branch ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        branch.displayName(),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.selectBranch(branch.id)
                                                    branchMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = viewModel::submit,
                                enabled = !uiState.isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (uiState.isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.height(22.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Registrar ${uiState.selectedType.label}")
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Historial de caja",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                val entries = uiState.cashBox?.entries.orEmpty().sortedWith(
                    compareByDescending<CashMovement> { it.businessDate }
                        .thenByDescending { it.createdAt.orEmpty() }
                )
                if (entries.isEmpty()) {
                    item {
                        Text(
                            text = "No hay movimientos registrados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(entries, key = { it.id }) { movement ->
                        CashMovementCard(
                            movement = movement,
                            branch = uiState.availableBranches.find { it.id == movement.branchId }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun CashBalanceCard(
    balance: java.math.BigDecimal,
    currencyCode: String,
    isRefreshing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavy)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text(
                        "SALDO ACTUAL DE CAJA",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isRefreshing) CircularProgressIndicator(
                    modifier = Modifier.height(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
            Text(
                FinancialCalculator.formatCurrency(balance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Moneda: $currencyCode",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun CashMovementCard(movement: CashMovement, branch: Branch?) {
    val outgoing = movement.type != CashMovementType.INCOME
    val amount = if (outgoing) movement.amount.negate() else movement.amount
    val amountColor = if (outgoing) StatusAlertContent else StatusReadyContent
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = movement.type.icon(),
                        contentDescription = movement.type.label,
                        tint = amountColor
                    )
                    Text(
                        movement.type.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = FinancialCalculator.formatCurrency(amount),
                    color = amountColor,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Text(
                movement.reason,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                buildString {
                    append(movement.businessDate)
                    branch?.let { append(" · ").append(it.code).append(" ").append(it.name) }
                    movement.reference?.let { append(" · Ref. ").append(it) }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            movement.notes?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MessageCard(message: String, background: Color, content: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = background
    ) {
        Text(
            text = message,
            color = content,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp)
        )
    }
}

private fun CashMovementType.icon() = when (this) {
    CashMovementType.INCOME -> Icons.Default.Add
    CashMovementType.EXPENSE -> Icons.Default.Remove
    CashMovementType.BRANCH_TRANSFER -> Icons.Default.SwapHoriz
}

private fun Branch.displayName(): String = "$code — $name"

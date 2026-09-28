package com.example.btmcontabilidad.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.CashBoxEntity
import com.example.btmcontabilidad.domain.model.CashMovement
import com.example.btmcontabilidad.domain.model.CashMovementType
import com.example.btmcontabilidad.ui.theme.DeepNavy
import com.example.btmcontabilidad.ui.theme.PrimaryBlue
import com.example.btmcontabilidad.ui.theme.SecondaryNavy
import com.example.btmcontabilidad.ui.theme.StatusAlertBg
import com.example.btmcontabilidad.ui.theme.StatusAlertContent
import com.example.btmcontabilidad.ui.theme.StatusReadyBg
import com.example.btmcontabilidad.ui.theme.StatusReadyContent
import com.example.btmcontabilidad.ui.viewmodel.CashBoxViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashBoxScreen(
    initialBranchId: String? = null,
    viewModel: CashBoxViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var branchMenuExpanded by remember { mutableStateOf(false) }
    var historyFilter by remember { mutableStateOf(CashHistoryFilter.ALL) }
    val selectedBranch = uiState.availableBranches.find { it.id == uiState.selectedBranchId }

    LaunchedEffect(initialBranchId) {
        initialBranchId?.let(viewModel::prepareBranchTransfer)
    }

    val currentBoxName = uiState.cashBox?.name ?: "Caja Chica"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentBoxName,
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
                        onClick = viewModel::openCreateBoxDialog,
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Nueva Caja Chica")
                    }
                    IconButton(
                        onClick = { viewModel.refresh() },
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

                // Selector de Cajas Chicas
                if (uiState.cashBoxes.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cajas Chicas",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = viewModel::openCreateBoxDialog,
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                                    Text("+ Nueva Caja", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(uiState.cashBoxes, key = { it.id }) { box ->
                                    val isSelected = box.id == uiState.selectedCashBoxId
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.selectCashBox(box.id) },
                                        label = {
                                            Text(
                                                text = if (box.isDefault) "${box.name} ★" else box.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.AccountBalanceWallet,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    val currentBoxEntity = uiState.cashBoxes.find { it.id == uiState.selectedCashBoxId }
                    CashBalanceCard(
                        boxName = uiState.cashBox?.name ?: currentBoxEntity?.name,
                        balance = uiState.cashBox?.currentBalance ?: java.math.BigDecimal.ZERO,
                        currencyCode = uiState.cashBox?.currencyCode ?: "DOP",
                        isDefault = currentBoxEntity?.isDefault ?: false,
                        description = currentBoxEntity?.description,
                        isRefreshing = uiState.isLoading
                    )
                }

                item {
                    Text(
                        text = "Registrar movimiento en ${uiState.cashBox?.name ?: "esta caja"}",
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
                                label = { Text("Agrega un concepto (opcional)") },
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
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Registrar ${uiState.selectedType.label}")
                                }
                            }
                        }
                    }
                }

                item {
                    HistoryHeaderAndFilters(
                        activeFilter = historyFilter,
                        onFilterSelected = { historyFilter = it }
                    )
                }

                val rawEntries = uiState.cashBox?.entries.orEmpty().sortedWith(
                    compareByDescending<CashMovement> { it.businessDate }
                        .thenByDescending { it.createdAt.orEmpty() }
                )
                val filteredEntries = rawEntries.filter { movement ->
                    when (historyFilter) {
                        CashHistoryFilter.ALL -> true
                        CashHistoryFilter.INCOMES -> movement.type == CashMovementType.INCOME
                        CashHistoryFilter.EXPENSES -> movement.type == CashMovementType.EXPENSE
                        CashHistoryFilter.TRANSFERS -> movement.type == CashMovementType.BRANCH_TRANSFER
                    }
                }

                if (filteredEntries.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "No hay movimientos registrados para este filtro.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    val groupedEntries = filteredEntries.groupBy { it.businessDate.substringBefore('T') }
                    groupedEntries.forEach { (dateKey, movementsInDate) ->
                        item(key = dateKey) {
                            HistoryDateGroupCard(
                                dateKey = dateKey,
                                movements = movementsInDate,
                                branches = uiState.availableBranches
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    // Diálogo para Crear Nueva Caja Chica
    if (uiState.showCreateBoxDialog) {
        AlertDialog(
            onDismissRequest = viewModel::closeCreateBoxDialog,
            title = {
                Text(
                    text = "Nueva Caja Chica",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Crea una caja chica para separar fondos de operación (ej: Caja Oficina, Caja de Ruta, etc.).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = uiState.newBoxName,
                        onValueChange = viewModel::updateNewBoxName,
                        label = { Text("Nombre de la caja *") },
                        placeholder = { Text("Ej: Caja de Ruta 1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uiState.newBoxInitialBalance,
                        onValueChange = viewModel::updateNewBoxInitialBalance,
                        label = { Text("Saldo inicial") },
                        placeholder = { Text("0.00") },
                        prefix = { Text("${FinancialCalculator.currencySymbol()} ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uiState.newBoxDescription,
                        onValueChange = viewModel::updateNewBoxDescription,
                        label = { Text("Descripción (opcional)") },
                        placeholder = { Text("Ej: Fondo para gastos de cobrador en ruta") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = uiState.newBoxIsDefault,
                            onCheckedChange = viewModel::updateNewBoxIsDefault
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = "Establecer como caja principal",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::createCashBox,
                    enabled = !uiState.isCreatingBox
                ) {
                    if (uiState.isCreatingBox) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Crear Caja")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::closeCreateBoxDialog,
                    enabled = !uiState.isCreatingBox
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun CashBalanceCard(
    boxName: String?,
    balance: java.math.BigDecimal,
    currencyCode: String,
    isDefault: Boolean = false,
    description: String? = null,
    isRefreshing: Boolean = false
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
                        text = (boxName ?: "CAJA CHICA").uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isDefault) {
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PrimaryBlue
                        ) {
                            Text(
                                "PRINCIPAL",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
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
            description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                "Moneda: $currencyCode",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

enum class CashHistoryFilter(val label: String) {
    ALL("Todos"),
    INCOMES("Entradas"),
    EXPENSES("Salidas"),
    TRANSFERS("Transferencias")
}

private fun formatGroupDate(dateStr: String): String {
    val cleanDate = dateStr.substringBefore('T')
    return try {
        val parsed = LocalDate.parse(cleanDate)
        val today = LocalDate.now()
        when {
            parsed.isEqual(today) -> "Hoy"
            parsed.isEqual(today.minusDays(1)) -> "Ayer"
            else -> parsed.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "ES")))
        }
    } catch (_: Exception) {
        cleanDate
    }
}

private fun formatMovementTime(movement: CashMovement): String {
    val raw = movement.createdAt
    if (raw != null && raw.contains('T')) {
        try {
            val timePart = raw.substringAfter('T').substringBefore('.')
            val cleanTime = timePart.substringBefore('Z')
            val localTime = java.time.LocalTime.parse(cleanTime)
            return localTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
        } catch (_: Exception) {}
    }
    return ""
}

@Composable
private fun HistoryHeaderAndFilters(
    activeFilter: CashHistoryFilter,
    onFilterSelected: (CashHistoryFilter) -> Unit,
    onTuneClicked: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Historial de movimientos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Consulta y filtra todos los movimientos de la caja chica",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = CircleShape,
                color = PrimaryBlue.copy(alpha = 0.12f),
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onTuneClicked() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filtro",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(CashHistoryFilter.values()) { filter ->
                val isSelected = filter == activeFilter
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surface,
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                    modifier = Modifier.clickable { onFilterSelected(filter) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (filter) {
                            CashHistoryFilter.ALL -> Unit
                            CashHistoryFilter.INCOMES -> Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Color(0xFF00875A),
                                modifier = Modifier.size(15.dp)
                            )
                            CashHistoryFilter.EXPENSES -> Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Color(0xFFD32F2F),
                                modifier = Modifier.size(15.dp)
                            )
                            CashHistoryFilter.TRANSFERS -> Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else SecondaryNavy,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryDateGroupCard(
    dateKey: String,
    movements: List<CashMovement>,
    branches: List<Branch>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            // Cabecera del grupo: Fecha + Contador de movimientos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatGroupDate(dateKey),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val countText = if (movements.size == 1) "1 movimiento" else "${movements.size} movimientos"
                Text(
                    text = countText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Filas de movimientos con divisores
            movements.forEachIndexed { index, movement ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.8.dp
                    )
                }
                val branch = branches.find { it.id == movement.branchId }
                HistoryMovementRow(movement = movement, branch = branch)
            }
        }
    }
}

@Composable
private fun HistoryMovementRow(movement: CashMovement, branch: Branch?) {
    val outgoing = movement.type != CashMovementType.INCOME
    val amount = movement.amount
    val isTransfer = movement.type == CashMovementType.BRANCH_TRANSFER
    val isInitialFund = movement.reason.contains("apertura", ignoreCase = true) ||
        movement.reason.contains("inicial", ignoreCase = true) ||
        movement.reason.contains("fondo base", ignoreCase = true)
    val isDeposit = movement.reason.contains("depósito", ignoreCase = true) || movement.reason.contains("banco", ignoreCase = true)

    val (iconBg, iconTint, iconVector) = when {
        isInitialFund -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Icons.Default.AccountBalanceWallet)
        isTransfer -> Triple(Color(0xFFFEECEB), Color(0xFFD32F2F), Icons.Default.SwapHoriz)
        isDeposit -> Triple(Color(0xFFE7F7EE), Color(0xFF00875A), Icons.Default.AccountBalance)
        movement.type == CashMovementType.INCOME -> Triple(Color(0xFFE7F7EE), Color(0xFF00875A), Icons.Default.ArrowDownward)
        else -> Triple(Color(0xFFEEEDFD), Color(0xFF5C59E8), Icons.Default.Description)
    }

    val title = when {
        isInitialFund -> "Fondo Inicial / Apertura"
        isTransfer -> "Transferencia a banca"
        isDeposit -> "Entrada por depósito"
        movement.type == CashMovementType.INCOME -> "Entrada de efectivo"
        movement.reason.contains("ajuste", ignoreCase = true) -> "Ajuste manual"
        movement.reason.isNotBlank() -> movement.reason
        else -> "Salida de efectivo"
    }

    val subtitle = buildString {
        if (isInitialFund) {
            append("Apertura de caja chica")
        } else if (isTransfer && branch != null) {
            append("${branch.code} ${branch.name}")
        } else if (movement.reason.isNotBlank() && title != movement.reason) {
            append(movement.reason)
        } else {
            append("Caja principal")
        }
        val time = formatMovementTime(movement)
        if (time.isNotBlank()) {
            append(" · ").append(time)
        }
    }

    val conceptTag = movement.notes?.takeIf { it.isNotBlank() }
        ?: if (isInitialFund) "Fondo base inicial"
        else if (isTransfer && movement.reason.isNotBlank() && movement.reason != title) movement.reason
        else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono Pastel (44dp x 44dp, RoundedCornerShape(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconBg,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                conceptTag?.let { tag ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEEF4FF),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Columna Derecha: Monto + Saldo + Menú de 3 puntos
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val formattedAmount = FinancialCalculator.formatCurrency(amount.abs())
                val amountStr = if (outgoing) "-$formattedAmount" else "+$formattedAmount"
                val amountColor = if (outgoing) Color(0xFFD32F2F) else Color(0xFF00875A)
                Text(
                    text = amountStr,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor
                )
                val balanceStr = movement.balanceAfter?.let { "Saldo: ${FinancialCalculator.formatCurrency(it)}" } ?: "Saldo: —"
                Text(
                    text = balanceStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { /* Menú de opciones */ },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
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


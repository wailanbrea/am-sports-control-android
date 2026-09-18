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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.AdvanceStatus
import com.example.btmcontabilidad.domain.model.CollectionStatus
import com.example.btmcontabilidad.ui.theme.BTMContabilidadTheme
import com.example.btmcontabilidad.ui.theme.PrimaryBlue
import com.example.btmcontabilidad.ui.theme.StatusAlertBg
import com.example.btmcontabilidad.ui.theme.StatusAlertContent
import com.example.btmcontabilidad.ui.theme.StatusNeutralBg
import com.example.btmcontabilidad.ui.theme.StatusNeutralContent
import com.example.btmcontabilidad.ui.theme.StatusReadyBg
import com.example.btmcontabilidad.ui.theme.StatusReadyContent
import com.example.btmcontabilidad.ui.viewmodel.AdvanceListFilter
import com.example.btmcontabilidad.ui.viewmodel.AdvanceListItem
import com.example.btmcontabilidad.ui.viewmodel.AdvanceListUiState
import com.example.btmcontabilidad.ui.viewmodel.AdvanceListViewModel
import com.example.btmcontabilidad.ui.viewmodel.CollectionListFilter
import com.example.btmcontabilidad.ui.viewmodel.CollectionListItem
import com.example.btmcontabilidad.ui.viewmodel.CollectionListUiState
import com.example.btmcontabilidad.ui.viewmodel.CollectionListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionListScreen(
    viewModel: CollectionListViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedItem by remember { mutableStateOf<CollectionListItem?>(null) }

    RecordMessages(
        errorMessage = uiState.errorMessage,
        successMessage = uiState.actionSuccessMessage,
        snackbarHostState = snackbarHostState,
        clearSuccessMessage = viewModel::clearSuccessMessage
    )

    uiState.items.firstOrNull { it.collection.id == selectedItem?.collection?.id }
        ?.reversibleLedgerEntry
        ?.let { entry ->
            RecordReversalDialog(
                entry = entry,
                onDismiss = { selectedItem = null },
                onConfirm = { reason ->
                    val collectionId = selectedItem?.collection?.id ?: return@RecordReversalDialog
                    selectedItem = null
                    viewModel.reverseCollection(collectionId, reason)
                }
            )
        }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Cobros registrados", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToRegister) {
                        Icon(Icons.Default.Add, contentDescription = "Registrar cobro")
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar cobros")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        CollectionListContent(
            uiState = uiState,
            onSearchChanged = viewModel::setSearchQuery,
            onFilterChanged = viewModel::setFilter,
            onRefresh = viewModel::refresh,
            onReverse = { item -> if (item.reversibleLedgerEntry != null) selectedItem = item },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun CollectionListContent(
    uiState: CollectionListUiState,
    onSearchChanged: (String) -> Unit,
    onFilterChanged: (CollectionListFilter) -> Unit,
    onRefresh: () -> Unit,
    onReverse: (CollectionListItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading && uiState.items.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (uiState.errorMessage != null && uiState.items.isEmpty()) {
        ErrorContent(message = uiState.errorMessage, onRetry = onRefresh, modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SearchField(
                value = uiState.searchQuery,
                placeholder = "Buscar por banca, fecha, referencia...",
                onValueChange = onSearchChanged
            )
        }
        item {
            FilterRow(
                filters = CollectionListFilter.entries,
                selected = uiState.selectedFilter,
                onSelected = onFilterChanged
            )
        }
        if (uiState.isLoading) {
            item { LinearRefreshIndicator() }
        }
        if (uiState.filteredItems.isEmpty()) {
            item { EmptyContent("No hay cobros que coincidan con los filtros") }
        } else {
            items(uiState.filteredItems, key = { it.collection.id }) { item ->
                CollectionCard(item = item, onReverse = { onReverse(item) })
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvanceListScreen(
    viewModel: AdvanceListViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedItem by remember { mutableStateOf<AdvanceListItem?>(null) }

    RecordMessages(
        errorMessage = uiState.errorMessage,
        successMessage = uiState.actionSuccessMessage,
        snackbarHostState = snackbarHostState,
        clearSuccessMessage = viewModel::clearSuccessMessage
    )

    uiState.items.firstOrNull { it.advance.id == selectedItem?.advance?.id }
        ?.reversibleLedgerEntry
        ?.let { entry ->
            RecordReversalDialog(
                entry = entry,
                onDismiss = { selectedItem = null },
                onConfirm = { reason ->
                    val advanceId = selectedItem?.advance?.id ?: return@RecordReversalDialog
                    selectedItem = null
                    viewModel.reverseAdvance(advanceId, reason)
                }
            )
        }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Adelantos registrados", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToRegister) {
                        Icon(Icons.Default.Add, contentDescription = "Registrar adelanto")
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar adelantos")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        AdvanceListContent(
            uiState = uiState,
            onSearchChanged = viewModel::setSearchQuery,
            onFilterChanged = viewModel::setFilter,
            onRefresh = viewModel::refresh,
            onReverse = { item -> if (item.reversibleLedgerEntry != null) selectedItem = item },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun AdvanceListContent(
    uiState: AdvanceListUiState,
    onSearchChanged: (String) -> Unit,
    onFilterChanged: (AdvanceListFilter) -> Unit,
    onRefresh: () -> Unit,
    onReverse: (AdvanceListItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading && uiState.items.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (uiState.errorMessage != null && uiState.items.isEmpty()) {
        ErrorContent(message = uiState.errorMessage, onRetry = onRefresh, modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SearchField(
                value = uiState.searchQuery,
                placeholder = "Buscar por banca, fecha, motivo...",
                onValueChange = onSearchChanged
            )
        }
        item {
            FilterRow(
                filters = AdvanceListFilter.entries,
                selected = uiState.selectedFilter,
                onSelected = onFilterChanged
            )
        }
        if (uiState.isLoading) {
            item { LinearRefreshIndicator() }
        }
        if (uiState.filteredItems.isEmpty()) {
            item { EmptyContent("No hay adelantos que coincidan con los filtros") }
        } else {
            items(uiState.filteredItems, key = { it.advance.id }) { item ->
                AdvanceCard(item = item, onReverse = { onReverse(item) })
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun CollectionCard(item: CollectionListItem, onReverse: () -> Unit) {
    val collection = item.collection
    RecordCard(
        icon = Icons.Default.Payments,
        title = "Cobro ${collection.id}",
        branch = branchLabel(item.branch, collection.branchId),
        amount = FinancialCalculator.formatCurrency(collection.amount),
        date = collection.businessDate,
        status = collection.status.label,
        statusColors = statusColors(collection.status),
        detail = listOfNotNull(
            "Método: ${collection.paymentMethod.label}",
            collection.reference?.takeIf { it.isNotBlank() }?.let { "Referencia: $it" },
            collection.notes?.takeIf { it.isNotBlank() }?.let { "Detalle: $it" }
        ).joinToString(" • "),
        canReverse = item.reversibleLedgerEntry != null,
        onReverse = onReverse
    )
}

@Composable
private fun AdvanceCard(item: AdvanceListItem, onReverse: () -> Unit) {
    val advance = item.advance
    RecordCard(
        icon = Icons.Default.LocalAtm,
        title = "Adelanto ${advance.id}",
        branch = branchLabel(item.branch, advance.branchId),
        amount = FinancialCalculator.formatCurrency(advance.amount),
        date = advance.businessDate,
        status = advance.status.label,
        statusColors = statusColors(advance.status),
        detail = listOfNotNull(
            "Motivo: ${advance.reason}",
            advance.notes?.takeIf { it.isNotBlank() }?.let { "Detalle: $it" }
        ).joinToString(" • "),
        canReverse = item.reversibleLedgerEntry != null,
        onReverse = onReverse
    )
}

@Composable
private fun RecordCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    branch: String,
    amount: String,
    date: String,
    status: String,
    statusColors: StatusColors,
    detail: String,
    canReverse: Boolean,
    onReverse: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryBlue)
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(
                        branch,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusBadge(status, statusColors)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Monto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Fecha", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (canReverse) {
                OutlinedButton(
                    onClick = onReverse,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusAlertContent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                    Spacer(modifier = Modifier.padding(horizontal = 3.dp))
                    Text("Reversar asiento")
                }
            } else {
                Text(
                    "Sin asiento activo para reversar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class StatusColors(val background: androidx.compose.ui.graphics.Color, val content: androidx.compose.ui.graphics.Color)

private fun statusColors(status: CollectionStatus): StatusColors = when (status) {
    CollectionStatus.CANCELLED -> StatusColors(StatusAlertBg, StatusAlertContent)
    CollectionStatus.VERIFIED -> StatusColors(StatusReadyBg, StatusReadyContent)
    CollectionStatus.REGISTERED -> StatusColors(StatusNeutralBg, StatusNeutralContent)
}

private fun statusColors(status: AdvanceStatus): StatusColors = when (status) {
    AdvanceStatus.CANCELLED -> StatusColors(StatusAlertBg, StatusAlertContent)
    AdvanceStatus.APPROVED -> StatusColors(StatusReadyBg, StatusReadyContent)
    AdvanceStatus.REGISTERED -> StatusColors(StatusNeutralBg, StatusNeutralContent)
}

@Composable
private fun StatusBadge(status: String, colors: StatusColors) {
    Surface(shape = RoundedCornerShape(10.dp), color = colors.background) {
        Text(
            status,
            style = MaterialTheme.typography.labelSmall,
            color = colors.content,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun SearchField(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                }
            }
        },
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )
}

@Composable
private fun <T> FilterRow(
    filters: List<T>,
    selected: T,
    onSelected: (T) -> Unit
) where T : Enum<T> {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filters) { filter ->
            val label = when (filter) {
                is CollectionListFilter -> filter.label
                is AdvanceListFilter -> filter.label
                else -> filter.name
            }
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun RecordMessages(
    errorMessage: String?,
    successMessage: String?,
    snackbarHostState: SnackbarHostState,
    clearSuccessMessage: () -> Unit
) {
    LaunchedEffect(errorMessage, successMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            clearSuccessMessage()
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message, color = StatusAlertContent)
            Button(onClick = onRetry) { Text("Reintentar") }
        }
    }
}

@Composable
private fun EmptyContent(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LinearRefreshIndicator() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
    }
}

private fun branchLabel(branch: com.example.btmcontabilidad.domain.model.Branch?, branchId: String): String =
    branch?.let { "Banca ${it.code} · ${it.name}" } ?: "Banca $branchId"

@Composable
private fun RecordReversalDialog(
    entry: com.example.btmcontabilidad.domain.model.LedgerEntry,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar reversión", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Se reversará el asiento ${entry.id} por ${FinancialCalculator.formatCurrency(entry.signedAmount.abs())}.")
                Text("La operación creará un nuevo movimiento en el servidor; el registro original no se eliminará.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Motivo requerido") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = StatusAlertContent)
            ) { Text("Reversar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun RecordListScreensPreview() {
    BTMContabilidadTheme { EmptyContent("Vista previa") }
}

package com.example.btmcontabilidad.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btmcontabilidad.domain.model.BranchStatus
import com.example.btmcontabilidad.ui.viewmodel.BranchFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchFormScreen(
    branchId: String?,
    viewModel: BranchFormViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onSaved: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(branchId) {
        if (branchId != null) viewModel.loadBranch(branchId)
    }
    LaunchedEffect(uiState.savedBranchId) {
        uiState.savedBranchId?.let(onSaved)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (branchId == null) "Nueva Banca" else "Editar Banca", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !uiState.isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Datos de la banca",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                item { BranchTextField(uiState.code, viewModel::updateCode, "Código", "Ej. B-001", !uiState.isSaving) }
                item { BranchTextField(uiState.name, viewModel::updateName, "Nombre", "Nombre de la banca", !uiState.isSaving) }
                item { BranchTextField(uiState.phone, viewModel::updatePhone, "Teléfono de la banca (opcional)", "Ej. 809-555-0101", !uiState.isSaving) }
                item {
                    Text(
                        "Datos del dueño / encargado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                item { BranchTextField(uiState.ownerName, viewModel::updateOwnerName, "Nombre del dueño (opcional)", "Ej. Juan Pérez", !uiState.isSaving) }
                item { BranchTextField(uiState.ownerPhone, viewModel::updateOwnerPhone, "Teléfono / WhatsApp del dueño (opcional)", "Ej. 809-555-0102", !uiState.isSaving) }
                item { BranchTextField(uiState.operatorName, viewModel::updateOperatorName, "Encargado / Operador (opcional)", "Nombre del operador", !uiState.isSaving) }
                item { BranchTextField(uiState.route, viewModel::updateRoute, "Ruta (opcional)", "Ej. Ruta Norte", !uiState.isSaving) }
                item { BranchTextField(uiState.description, viewModel::updateDescription, "Descripción / Dirección (opcional)", "Ubicación o notas", !uiState.isSaving, minLines = 2) }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Estado", style = MaterialTheme.typography.labelLarge)
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BranchStatus.entries.forEach { status ->
                                FilterChip(
                                    selected = uiState.status == status,
                                    onClick = { viewModel.updateStatus(status) },
                                    enabled = !uiState.isSaving,
                                    label = { Text(status.label) }
                                )
                            }
                        }
                    }
                }
                uiState.errorMessage?.let { message ->
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(message, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                item {
                    Button(
                        onClick = { viewModel.save(branchId) },
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else Text(if (branchId == null) "Crear banca" else "Guardar cambios")
                    }
                }
            }
        }
    }
}

@Composable
private fun BranchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        minLines = minLines,
        singleLine = minLines == 1
    )
}

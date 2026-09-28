package com.example.btmcontabilidad.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.btmcontabilidad.data.network.CollectorDto
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.BranchStatus
import com.example.btmcontabilidad.ui.theme.DeepNavy
import com.example.btmcontabilidad.ui.theme.PrimaryBlue

@Composable
fun AssignBranchesDialog(
    collector: CollectorDto,
    allBranches: List<Branch>,
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>) -> Unit
) {
    // Pre-populate with branches currently assigned to this collector
    var selectedBranchIds by remember {
        mutableStateOf(
            allBranches
                .filter { it.collectorUserId == collector.id }
                .mapNotNull { it.id.toLongOrNull() }
                .toSet()
        )
    }
    var searchQuery by remember { mutableStateOf("") }

    val activeBranches = remember(allBranches) {
        allBranches.filter { it.status == BranchStatus.ACTIVE }
    }

    val filteredBranches = remember(activeBranches, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) activeBranches
        else activeBranches.filter {
            it.name.lowercase().contains(q) ||
            it.code.lowercase().contains(q) ||
            it.operatorName.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Column {
                Text(
                    text = "Asignar Bancas a Cobrador",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Cobrador: ${collector.name} (${collector.email})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Buscador rápido
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar banca por código o nombre...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Acciones rápidas (Marcar / Desmarcar todas)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedBranchIds.size} de ${activeBranches.size} seleccionadas",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )

                    Row {
                        TextButton(
                            onClick = {
                                selectedBranchIds = activeBranches.mapNotNull { it.id.toLongOrNull() }.toSet()
                            },
                            enabled = !isLoading
                        ) {
                            Text("Todas", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(
                            onClick = {
                                selectedBranchIds = emptySet()
                            },
                            enabled = !isLoading
                        ) {
                            Text("Ninguna", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Lista de bancas con checkbox
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredBranches, key = { it.id }) { branch ->
                        val branchLongId = branch.id.toLongOrNull()
                        val isChecked = branchLongId != null && selectedBranchIds.contains(branchLongId)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !isLoading && branchLongId != null) {
                                    branchLongId?.let { id ->
                                        selectedBranchIds = if (isChecked) {
                                            selectedBranchIds - id
                                        } else {
                                            selectedBranchIds + id
                                        }
                                    }
                                },
                            color = if (isChecked) PrimaryBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        branchLongId?.let { id ->
                                            selectedBranchIds = if (checked) {
                                                selectedBranchIds + id
                                            } else {
                                                selectedBranchIds - id
                                            }
                                        }
                                    },
                                    enabled = !isLoading && branchLongId != null,
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = DeepNavy
                                ) {
                                    Text(
                                        text = branch.code,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = branch.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (branch.collectorUserId != null && branch.collectorUserId != collector.id) {
                                        Text(
                                            text = "⚠️ Asignada a: ${branch.collectorName ?: "Otro cobrador"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        Text(
                                            text = branch.route.ifBlank { "Ruta general" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedBranchIds.toList()) },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Guardar (${selectedBranchIds.size})")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancelar")
            }
        }
    )
}

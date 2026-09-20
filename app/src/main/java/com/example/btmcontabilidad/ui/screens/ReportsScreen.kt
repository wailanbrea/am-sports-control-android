package com.example.btmcontabilidad.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.btmcontabilidad.ui.theme.BTMContabilidadTheme
import com.example.btmcontabilidad.ui.theme.DeepNavy
import com.example.btmcontabilidad.ui.theme.PrimaryBlue
import com.example.btmcontabilidad.data.settings.ApiSettings
import com.example.btmcontabilidad.data.repository.BackendApiProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToLedger: () -> Unit = {},
    onNavigateToBancas: () -> Unit = {},
    onNavigateToCollections: () -> Unit = {},
    onNavigateToAdvances: () -> Unit = {},
    onNavigateToCashBox: () -> Unit = {},
    onNavigateToExport: () -> Unit = {},
    onNavigateToCollectors: () -> Unit = {},
    onLogout: () -> Unit = {}
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Reportes & Configuración",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Reportes Contables",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }

            item {
                ReportMenuItemCard(
                    title = "Cuadrante Contable",
                    subtitle = "Resumen consolidado de cobros y saldos por banca",
                    icon = Icons.Default.Assessment,
                    onClick = onNavigateToDashboard
                )
            }

            item {
                ReportMenuItemCard(
                    title = "Estado de Cuenta Consolidado",
                    subtitle = "Historial completo de movimientos del consorcio",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    onClick = onNavigateToLedger
                )
            }

            item {
                ReportMenuItemCard(
                    title = "Caja menor",
                    subtitle = "Consulta saldo y registra entradas, gastos o transferencias",
                    icon = Icons.Default.AccountBalanceWallet,
                    onClick = onNavigateToCashBox
                )
            }

            item {
                ReportMenuItemCard(
                    title = "Exportación a Excel / PDF",
                    subtitle = "Descarga el libro mayor en formato CSV o PDF",
                    icon = Icons.Default.Download,
                    onClick = onNavigateToExport
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Ajustes del Sistema",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }

            item {
                ReportMenuItemCard(
                    title = "Gestión de Cobradores",
                    subtitle = "Crear y administrar cobradores y accesos al consorcio",
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToCollectors
                )
            }

            item {
                ReportMenuItemCard(
                    title = "Configuración del Consorcio",
                    subtitle = "Gestión de parámetros, usuarios y rutas",
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToBancas
                )
            }

            item {
                ReportMenuItemCard(
                    title = "Cobros registrados",
                    subtitle = "Consulta, filtra y reversa cobros con asiento activo",
                    icon = Icons.Default.Payments,
                    onClick = onNavigateToCollections
                )
            }

            item {
                ReportMenuItemCard(
                    title = "Adelantos registrados",
                    subtitle = "Consulta, filtra y reversa adelantos con asiento activo",
                    icon = Icons.Default.LocalAtm,
                    onClick = onNavigateToAdvances
                )
            }

            item {
                ApiConfigurationCard(onLogout = onLogout)
            }
        }
    }
}

@Composable
private fun ApiConfigurationCard(onLogout: () -> Unit = {}) {
    val context = LocalContext.current.applicationContext
    val settings = remember(context) { ApiSettings(context) }
    val provider = remember(context) { BackendApiProvider(context) }
    val scope = rememberCoroutineScope()
    var baseUrl by remember { mutableStateOf("") }
    var serverMessage by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAuthenticating by remember { mutableStateOf(false) }
    var isAuthenticated by remember { mutableStateOf(provider.isAuthenticated()) }
    var sessionMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settings) {
        baseUrl = settings.baseUrl.first()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Conexión al servidor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "URL de la API. Para emulador usa 10.0.2.2; en producción usa HTTPS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL base") },
                singleLine = true
            )
            Button(onClick = {
                scope.launch {
                    serverMessage = runCatching { settings.updateBaseUrl(baseUrl) }
                        .fold(
                            onSuccess = { "Servidor actualizado" },
                            onFailure = { it.message ?: "URL inválida" }
                        )
                }
            }) {
                Text("Guardar servidor")
            }
            serverMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

            Text("Sesión", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                if (isAuthenticated) "Sesión activa en este dispositivo" else "Sin sesión iniciada",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isAuthenticated) {
                Button(onClick = {
                    provider.logout()
                    isAuthenticated = false
                    password = ""
                    sessionMessage = "Sesión cerrada"
                    onLogout()
                }) {
                    Text("Cerrar sesión")
                }
            } else {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Correo electrónico") },
                    singleLine = true,
                    enabled = !isAuthenticating
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contraseña") },
                    singleLine = true,
                    enabled = !isAuthenticating,
                    visualTransformation = PasswordVisualTransformation()
                )
                Button(
                    onClick = {
                        scope.launch {
                            isAuthenticating = true
                            sessionMessage = null
                            runCatching {
                                provider.login(email, password, Build.MODEL.ifBlank { Build.DEVICE })
                            }.onSuccess {
                                isAuthenticated = true
                                password = ""
                                sessionMessage = "Sesión iniciada correctamente"
                            }.onFailure {
                                sessionMessage = it.message ?: "No se pudo iniciar sesión"
                            }
                            isAuthenticating = false
                        }
                    },
                    enabled = !isAuthenticating
                ) {
                    if (isAuthenticating) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Iniciar sesión")
                    }
                }
            }
            sessionMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun ReportMenuItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DeepNavy
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Próximamente",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun ReportsScreenPreview() {
    BTMContabilidadTheme {
        ReportsScreen()
    }
}

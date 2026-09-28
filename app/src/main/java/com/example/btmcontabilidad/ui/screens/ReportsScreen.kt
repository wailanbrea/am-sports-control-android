package com.example.btmcontabilidad.ui.screens

import android.os.Build
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onNavigateToExpense: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    val provider = remember(context) { BackendApiProvider(context) }
    val isAdmin = provider.isAdmin

    var currentUserName by remember { mutableStateOf(provider.session.userName().orEmpty()) }
    var currentUserEmail by remember { mutableStateOf(provider.session.userEmail().orEmpty()) }
    var currentUserRole by remember { mutableStateOf(provider.session.userRole()) }

    LaunchedEffect(Unit) {
        val user = provider.fetchCurrentUser()
        if (user != null) {
            currentUserName = user.name
            currentUserEmail = user.email
            currentUserRole = user.role ?: if (isAdmin) "admin" else "collector"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isAdmin) "Reportes & Configuración" else "Menú de Opciones",
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
                ActiveUserSessionCard(
                    name = currentUserName.ifBlank { if (isAdmin) "Administrador BTM" else "Cobrador" },
                    email = currentUserEmail.ifBlank { if (isAdmin) "demo@btmcontabilidad.local" else "Sin correo registrado" },
                    role = if (isAdmin) "Administrador" else "Cobrador",
                    isAdmin = isAdmin,
                    onLogout = {
                        provider.logout()
                        onLogout()
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAdmin) "Reportes Contables" else "Consultas de Cobro",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }

            item {
                ReportMenuItemCard(
                    title = if (isAdmin) "Cuadrante Contable" else "Resumen de Bancas y Préstamos",
                    subtitle = if (isAdmin) "Resumen consolidado de cobros y saldos por banca" else "Consulta deudas, préstamos y saldos pendientes por banca",
                    icon = Icons.Default.Assessment,
                    onClick = if (isAdmin) onNavigateToDashboard else onNavigateToBancas
                )
            }

            if (isAdmin) {
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
                        title = "Caja Chica (Oficina)",
                        subtitle = "Consulta saldo disponible, entregas para premios y entradas de efectivo",
                        icon = Icons.Default.AccountBalanceWallet,
                        onClick = onNavigateToCashBox
                    )
                }

                item {
                    ReportMenuItemCard(
                        title = "Registrar Gasto Operativo",
                        subtitle = "Luz, rollos, transporte, alquiler y otros gastos con comprobante",
                        icon = Icons.Default.ReceiptLong,
                        onClick = onNavigateToExpense
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
            }

            if (isAdmin) {
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
            }

            item {
                ReportMenuItemCard(
                    title = "Cobros registrados",
                    subtitle = if (isAdmin) "Consulta, filtra y reversa cobros con asiento activo" else "Consulta y filtra cobros registrados",
                    icon = Icons.Default.Payments,
                    onClick = onNavigateToCollections
                )
            }

            if (isAdmin) {
                item {
                    ReportMenuItemCard(
                        title = "Adelantos registrados",
                        subtitle = "Consulta, filtra y reversa adelantos con asiento activo",
                        icon = Icons.Default.LocalAtm,
                        onClick = onNavigateToAdvances
                    )
                }
            }

            if (isAdmin) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Configuración del Servidor",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
                item {
                    ServerConfigurationCard()
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ActiveUserSessionCard(
    name: String,
    email: String,
    role: String,
    isAdmin: Boolean,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdmin) DeepNavy else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(if (isAdmin) PrimaryBlue else PrimaryBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Usuario",
                        tint = if (isAdmin) Color.White else PrimaryBlue,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SESIÓN ACTIVA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isAdmin) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.0.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isAdmin) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isAdmin) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAdmin) PrimaryBlue else PrimaryBlue.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = role,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isAdmin) Color.White else PrimaryBlue
                    )
                }
            }

            HorizontalDivider(
                color = if (isAdmin) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isAdmin) Color.White else MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "Cerrar sesión",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ServerConfigurationCard() {
    val context = LocalContext.current.applicationContext
    val settings = remember(context) { ApiSettings(context) }
    val scope = rememberCoroutineScope()
    var baseUrl by remember { mutableStateOf("") }
    var serverMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settings) {
        baseUrl = settings.baseUrl.first()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Conexión al servidor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "URL de la API backend.",
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

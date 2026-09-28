package com.example.btmcontabilidad.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.LedgerEntry
import com.example.btmcontabilidad.domain.model.isCollection
import com.example.btmcontabilidad.domain.model.isMoneyDelivery
import com.example.btmcontabilidad.domain.model.isNegativeMovement
import com.example.btmcontabilidad.ui.theme.BTMContabilidadTheme
import com.example.btmcontabilidad.ui.theme.DeepNavy
import com.example.btmcontabilidad.ui.theme.PrimaryBlue
import com.example.btmcontabilidad.ui.theme.StatusAlertBg
import com.example.btmcontabilidad.ui.theme.StatusAlertContent
import com.example.btmcontabilidad.ui.theme.StatusReadyBg
import com.example.btmcontabilidad.ui.theme.StatusReadyContent
import com.example.btmcontabilidad.ui.viewmodel.DashboardUiState
import com.example.btmcontabilidad.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Micro-interacción elástica al presionar cualquier elemento interactivo.
 */
fun Modifier.bouncingClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bouncingClickableScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

private fun formatDashboardDate(dateStr: String): String {
    val cleanDate = dateStr.substringBefore('T')
    return try {
        val parsed = LocalDate.parse(cleanDate)
        val today = LocalDate.now()
        when {
            parsed.isEqual(today) -> "Hoy"
            parsed.isEqual(today.minusDays(1)) -> "Ayer"
            else -> parsed.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-ES")))
        }
    } catch (_: Exception) {
        cleanDate
    }
}

private fun formatDashboardTime(dateStr: String): String {
    if (dateStr.contains('T')) {
        try {
            val timePart = dateStr.substringAfter('T').substringBefore('.')
            val cleanTime = timePart.substringBefore('Z')
            val localTime = LocalTime.parse(cleanTime)
            return localTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
        } catch (_: Exception) {}
    }
    return ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onNavigateToRegisterCollection: () -> Unit = {},
    onNavigateToRegisterAdvance: () -> Unit = {},
    onNavigateToMoneyDelivery: () -> Unit = {},
    onNavigateToManualResult: () -> Unit = {},
    onNavigateToRegisterExpense: () -> Unit = {},
    onNavigateToCollectors: () -> Unit = {},
    onNavigateToLedger: () -> Unit = {},
    onNavigateToBancas: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCashBox: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showNotifications by remember { mutableStateOf(false) }

    // Animación de rotación continua para el icono de refrescar
    val infiniteTransition = rememberInfiniteTransition(label = "refreshRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refreshRotationVal"
    )

    // Control de visibilidad escalonada para animar la entrada de secciones
    var headerVisible by remember { mutableStateOf(false) }
    var actionsVisible by remember { mutableStateOf(false) }
    var recentsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            headerVisible = true
            actionsVisible = true
            recentsVisible = true
        }
    }

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
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar datos",
                            tint = PrimaryBlue,
                            modifier = if (uiState.isLoading) Modifier.rotate(rotation) else Modifier
                        )
                    }
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
        if (uiState.isLoading && !headerVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tarjeta Principal, Métricas Operativas y Caja Chica
                item {
                    AnimatedVisibility(
                        visible = headerVisible,
                        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                            initialOffsetY = { 35 },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                    ) {
                        TopMetricsSection(
                            uiState = uiState,
                            onNavigateToBancas = onNavigateToBancas,
                            onNavigateToCashBox = onNavigateToCashBox
                        )
                    }
                }

                // Acciones Rápidas
                item {
                    AnimatedVisibility(
                        visible = actionsVisible,
                        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                    ) {
                        QuickActionsSection(
                            isAdmin = uiState.isAdmin,
                            onRegisterCollection = onNavigateToRegisterCollection,
                            onRegisterMoneyDelivery = onNavigateToMoneyDelivery,
                            onRegisterExpense = onNavigateToRegisterExpense,
                            onNavigateToCollectors = onNavigateToCollectors
                        )
                    }
                }

                // Cabecera de Movimientos Recientes (Solo Admin)
                if (uiState.isAdmin) {
                    item {
                        AnimatedVisibility(
                            visible = recentsVisible,
                            enter = fadeIn(animationSpec = tween(400))
                        ) {
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
                    }

                    // Historial de Movimientos Recientes con el diseño visual unificado de Caja Chica
                    item {
                        AnimatedVisibility(
                            visible = recentsVisible,
                            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                                initialOffsetY = { 30 },
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                        ) {
                            if (uiState.recentTransactions.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "No hay movimientos registrados",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                DashboardRecentTransactionsCard(
                                    transactions = uiState.recentTransactions,
                                    branchesMap = uiState.branchesMap,
                                    onItemClick = onNavigateToLedger
                                )
                            }
                        }
                    }
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

/**
 * Efecto de animación y conteo numérico progresivo (Fintech count-up) para métricas del dashboard.
 */
@Composable
fun AnimatedCurrencyText(
    targetAmount: BigDecimal,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Bold,
    durationMillis: Int = 1000
) {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(targetAmount) {
        animatable.snapTo(0f)
        animatable.animateTo(
            targetValue = targetAmount.toFloat(),
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing
            )
        )
    }

    val isFinished = animatable.value == targetAmount.toFloat()
    val textToDisplay = if (isFinished) {
        FinancialCalculator.formatCurrency(targetAmount)
    } else {
        val rounded = BigDecimal.valueOf(animatable.value.toDouble()).setScale(2, RoundingMode.HALF_UP)
        FinancialCalculator.formatCurrency(rounded)
    }

    Text(
        text = textToDisplay,
        style = style,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}

/**
 * Sección de Métricas Principales:
 * - Hero Card: Muestra la posición exigible clara de cobro a bancas con badge contextual.
 * - Fila Secundaria: Muestra métricas operativas reales (Cobrado vs Premios entregados/por enviar).
 *   Sin duplicación de montos.
 * - Tarjeta Caja Chica: Rediseñada con acabado premium, sólido, libre de artefactos visuales.
 */
@Composable
fun TopMetricsSection(
    uiState: DashboardUiState,
    onNavigateToBancas: () -> Unit,
    onNavigateToCashBox: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Hero Card - Balance Principal Exigible (Navy Gradient con sombra)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .bouncingClickable { onNavigateToBancas() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DeepNavy),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E293B),
                                DeepNavy
                            )
                        )
                    )
                    .padding(22.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.isAdmin) "TOTAL POR RECOGER" else "TOTAL PRÉSTAMOS / POR COBRAR",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${uiState.pendingBranchesCount} ${if (uiState.pendingBranchesCount == 1) "banca" else "bancas"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    AnimatedCurrencyText(
                        targetAmount = uiState.totalReceivable,
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sub-badge informativo contextual:
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.isAdmin) {
                                if (uiState.totalBranchCredit > BigDecimal.ZERO) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = StatusAlertContent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Por enviar: ${FinancialCalculator.formatCurrency(uiState.totalBranchCredit)} · Posición neta: ${FinancialCalculator.formatCurrency(uiState.netPosition)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.95f),
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusReadyContent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Al día en entrega de premios · Exigible neto consolidado",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.95f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Store,
                                    contentDescription = null,
                                    tint = StatusReadyContent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Saldos y préstamos pendientes en bancas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isAdmin) {
            // Fila de Métricas Secundarias (Cobrado vs Premios entregados/por enviar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Métrica 1: Cobrado del Ciclo
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .bouncingClickable { onNavigateToBancas() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "COBRADO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(StatusReadyBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = StatusReadyContent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        AnimatedCurrencyText(
                            targetAmount = uiState.collectionsTotal,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = StatusReadyContent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Recaudado este ciclo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                // Métrica 2: Premios Entregados / Saldo por Enviar
                val isCreditPending = uiState.totalBranchCredit > BigDecimal.ZERO
                val metricLabel = if (isCreditPending) "POR ENVIAR" else "PREMIOS"
                val metricValue = if (isCreditPending) uiState.totalBranchCredit else (if (uiState.moneyDeliveredTotal > BigDecimal.ZERO) uiState.moneyDeliveredTotal else uiState.advancesTotal)
                val metricSubtext = if (isCreditPending) "${uiState.creditBranchesCount} con saldo a favor" else "Llevado en el ciclo"

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .bouncingClickable { onNavigateToBancas() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = metricLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(StatusAlertBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalAtm,
                                    contentDescription = null,
                                    tint = StatusAlertContent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        AnimatedCurrencyText(
                            targetAmount = metricValue,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = StatusAlertContent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = metricSubtext,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        } else {
            // Métrica Cobrado a ancho completo para cobrador
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncingClickable { onNavigateToBancas() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL COBRADO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(StatusReadyBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = StatusReadyContent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    AnimatedCurrencyText(
                        targetAmount = uiState.collectionsTotal,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusReadyContent
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Recaudado este ciclo por cobros a bancas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        if (uiState.isAdmin) {
            // Tarjeta Caja Chica (Oficina) - Rediseño moderno, limpio y con máxima coherencia UI/UX
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncingClickable { onNavigateToCashBox() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = PrimaryBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Caja Chica",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "CAJA CHICA (OFICINA)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )
                            AnimatedCurrencyText(
                                targetAmount = uiState.cashBalance,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Fondo operativo disponible",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = PrimaryBlue.copy(alpha = 0.10f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Ver caja",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    isAdmin: Boolean = true,
    onRegisterCollection: () -> Unit,
    onRegisterMoneyDelivery: () -> Unit,
    onRegisterExpense: () -> Unit,
    onNavigateToCollectors: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Acciones Rápidas",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!isAdmin) {
            // Vista de Cobrador: Solo permiso de Cobrar
            QuickActionButton(
                label = "Registrar Cobro a Banca",
                icon = Icons.Default.Payments,
                backgroundColor = StatusReadyContent,
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
                onClick = onRegisterCollection
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    label = "Gasto",
                    icon = Icons.Default.ReceiptLong,
                    backgroundColor = Color(0xFFD97706),
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f),
                    onClick = onRegisterExpense
                )

                QuickActionButton(
                    label = "Cobradores",
                    icon = Icons.Default.Badge,
                    backgroundColor = PrimaryBlue,
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCollectors
                )
            }
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
        modifier = modifier.bouncingClickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

/**
 * Tarjeta unificada de transacciones recientes para el Dashboard,
 * con la misma identidad estética, pastel y jerarquía que el historial de Caja Chica.
 */
@Composable
fun DashboardRecentTransactionsCard(
    transactions: List<LedgerEntry>,
    branchesMap: Map<String, com.example.btmcontabilidad.domain.model.Branch> = emptyMap(),
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            // Cabecera del contenedor: Fecha/Contexto y conteo de movimientos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Últimas operaciones",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val countText = if (transactions.size == 1) "1 movimiento" else "${transactions.size} movimientos"
                Text(
                    text = countText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Filas de movimientos con divisores
            transactions.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.8.dp
                    )
                }
                val branch = branchesMap[entry.branchId]
                DashboardHistoryMovementRow(
                    entry = entry,
                    branch = branch,
                    onClick = onItemClick
                )
            }
        }
    }
}

/**
 * Fila individual con la misma estructura visual de HistoryMovementRow:
 * - Icono en caja pastel redondeada (rosa/verde/lavanda)
 * - Título en negrita + subtítulo con origen y hora/fecha
 * - Micro-chip azul pastel para el concepto/nota con icono de documento
 * - Importe destacado en color semántico + saldo/usuario + botón ⋮
 */
@Composable
fun DashboardHistoryMovementRow(
    entry: LedgerEntry,
    branch: com.example.btmcontabilidad.domain.model.Branch? = null,
    onClick: () -> Unit
) {
    val isNegative = entry.isNegativeMovement()
    val isMoneyDelivery = entry.isMoneyDelivery()
    val isCollection = entry.isCollection()
    val isAdjustment = !isMoneyDelivery && !isCollection

    val (iconBg, iconTint, iconVector) = when {
        isMoneyDelivery -> Triple(Color(0xFFFEECEB), Color(0xFFD32F2F), Icons.Default.SwapHoriz)
        isCollection -> Triple(Color(0xFFE7F7EE), Color(0xFF00875A), Icons.Default.Payments)
        isNegative -> Triple(Color(0xFFFEECEB), Color(0xFFD32F2F), Icons.Default.LocalAtm)
        else -> Triple(Color(0xFFEEEDFD), Color(0xFF5C59E8), Icons.Default.Add)
    }

    val title = when {
        isMoneyDelivery -> "Entrega de Premios"
        isCollection -> "Cobro a Banca"
        isNegative -> "Pérdida Operativa"
        else -> "Ganancia Operativa"
    }

    val branchLabel = branch?.let { "${it.code} · ${it.name}".trim() }
        ?: if (!entry.branchName.isNullOrBlank()) {
            val codePrefix = entry.branchCode?.takeIf { it.isNotBlank() }?.let { "$it · " }.orEmpty()
            "$codePrefix${entry.branchName}".trim()
        } else if (!entry.branchId.isNullOrBlank()) {
            "Banca #${entry.branchId}"
        } else {
            "Consorcio"
        }

    val subtitle = buildString {
        append(branchLabel)
        val time = formatDashboardTime(entry.businessDate)
        if (time.isNotBlank()) {
            append(" · ").append(time)
        } else {
            append(" · ").append(formatDashboardDate(entry.businessDate))
        }
    }

    val conceptTag = entry.description.takeIf { it.isNotBlank() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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

        // Columna Derecha: Monto + Subtítulo/Usuario + Menú ⋮
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val formattedAmount = FinancialCalculator.formatCurrency(entry.signedAmount.abs())
                val amountStr = if (isNegative) "-$formattedAmount" else "+$formattedAmount"
                val amountColor = if (isNegative) Color(0xFFD32F2F) else Color(0xFF00875A)
                Text(
                    text = amountStr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    textAlign = TextAlign.End
                )

                if (entry.balanceAfter != null) {
                    Text(
                        text = "Saldo: ${FinancialCalculator.formatCurrency(entry.balanceAfter)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                } else if (entry.createdBy.isNotBlank()) {
                    Text(
                        text = entry.createdBy,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
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

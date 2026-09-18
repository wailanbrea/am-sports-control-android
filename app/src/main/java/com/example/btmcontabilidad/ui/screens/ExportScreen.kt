package com.example.btmcontabilidad.ui.screens

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.domain.model.LedgerEntry
import com.example.btmcontabilidad.ui.theme.DeepNavy
import com.example.btmcontabilidad.ui.theme.PrimaryBlue
import com.example.btmcontabilidad.ui.theme.StatusAlertBg
import com.example.btmcontabilidad.ui.theme.StatusAlertContent
import com.example.btmcontabilidad.ui.theme.StatusReadyBg
import com.example.btmcontabilidad.ui.theme.StatusReadyContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val repository = remember { RepositoryContainer.Instance.ledgerRepository }
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<LedgerEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isExporting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    suspend fun loadEntries() {
        isLoading = true
        errorMessage = null
        try {
            entries = repository.getLedgerEntries().first()
        } catch (exception: Exception) {
            errorMessage = exception.message ?: "No se pudo cargar el libro mayor"
        } finally {
            isLoading = false
        }
    }

    fun startExport(
        uri: Uri?,
        format: String,
        writer: suspend (Context, Uri, List<LedgerEntry>) -> Unit
    ) {
        if (uri == null) {
            statusMessage = "Exportación cancelada"
            return
        }
        scope.launch {
            isExporting = true
            statusMessage = null
            errorMessage = null
            try {
                writer(context, uri, entries)
                statusMessage = "$format generado correctamente (${entries.size} movimientos)"
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "No se pudo generar el archivo $format"
            } finally {
                isExporting = false
            }
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        startExport(uri, "CSV", ::writeCsv)
    }
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        startExport(uri, "PDF", ::writePdf)
    }

    LaunchedEffect(repository) {
        loadEntries()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Exportar libro mayor", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isExporting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { scope.launch { loadEntries() } },
                        enabled = !isLoading && !isExporting
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar movimientos")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            errorMessage != null && entries.isEmpty() -> ExportErrorState(
                message = errorMessage!!,
                onRetry = { scope.launch { loadEntries() } },
                modifier = Modifier.padding(innerPadding)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DeepNavy)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Libro mayor listo para exportar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${entries.size} movimientos cargados desde el servidor",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    item {
                        MessageCard(
                            message = errorMessage!!,
                            background = StatusAlertBg,
                            contentColor = StatusAlertContent
                        )
                    }
                }

                statusMessage?.let { message ->
                    item {
                        MessageCard(
                            message = message,
                            background = StatusReadyBg,
                            contentColor = StatusReadyContent
                        )
                    }
                }

                item {
                    Text(
                        text = "Selecciona un formato",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }

                item {
                    ExportOptionCard(
                        title = "Archivo CSV para Excel",
                        description = "Incluye todos los campos del libro mayor en UTF-8.",
                        icon = Icons.Default.Description,
                        enabled = entries.isNotEmpty() && !isExporting,
                        onClick = {
                            csvLauncher.launch(
                                "libro_mayor_${todayForFileName()}.csv"
                            )
                        }
                    )
                }

                item {
                    ExportOptionCard(
                        title = "Documento PDF",
                        description = "Genera un documento paginado, listo para compartir o imprimir.",
                        icon = Icons.Default.PictureAsPdf,
                        enabled = entries.isNotEmpty() && !isExporting,
                        onClick = {
                            pdfLauncher.launch(
                                "libro_mayor_${todayForFileName()}.pdf"
                            )
                        }
                    )
                }

                if (entries.isEmpty()) {
                    item {
                        Text(
                            text = "No hay movimientos disponibles para exportar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isExporting) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 10.dp))
                            Text("Generando archivo…")
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun ExportErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No se pudo cargar el libro mayor", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Text("Reintentar", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun MessageCard(
    message: String,
    background: Color,
    contentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = message,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
private fun ExportOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = DeepNavy)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Elegir ubicación")
            }
        }
    }
}

private suspend fun writeCsv(context: Context, uri: Uri, entries: List<LedgerEntry>) {
    val content = buildString {
        append('\uFEFF')
        appendLine(
            listOf(
                "ID", "Banca", "Tipo de origen", "ID de origen", "Tipo de asiento",
                "Importe firmado", "Saldo anterior", "Saldo posterior", "Fecha",
                "Descripción", "Creado por", "ID de reverso"
            ).joinToString(",")
        )
        entries.forEach { entry ->
            appendLine(
                listOf(
                    entry.id,
                    entry.branchId,
                    entry.sourceType.name,
                    entry.sourceId,
                    entry.entryType.name,
                    entry.signedAmount.toPlainString(),
                    entry.balanceBefore.toPlainString(),
                    entry.balanceAfter.toPlainString(),
                    entry.businessDate,
                    entry.description,
                    entry.createdBy,
                    entry.reversalOfEntryId.orEmpty()
                ).joinToString(",") { it.toCsvField() }
            )
        }
    }

    withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
        } ?: error("No se pudo abrir el archivo seleccionado")
    }
}

private suspend fun writePdf(context: Context, uri: Uri, entries: List<LedgerEntry>) {
    withContext(Dispatchers.IO) {
        val document = PdfDocument()
        try {
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 18f
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f }
            val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.DKGRAY
                textSize = 9f
            }
            var pageNumber = 1
            var page = document.startPage(pageInfo(pageNumber))
            var canvas = page.canvas
            var y = 42f

            fun startNewPage() {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(pageInfo(pageNumber))
                canvas = page.canvas
                y = 42f
            }

            fun line(text: String, paint: Paint = bodyPaint, spacing: Float = 14f) {
                if (y > 800f) startNewPage()
                canvas.drawText(text, 36f, y, paint)
                y += spacing
            }

            line("Libro mayor", titlePaint, 24f)
            line("Generado: ${todayForDisplay()}", mutedPaint)
            line("Movimientos: ${entries.size}", mutedPaint, 24f)

            if (entries.isEmpty()) {
                line("No hay movimientos para mostrar.")
            } else {
                entries.forEachIndexed { index, entry ->
                    if (y > 750f) startNewPage()
                    line("${index + 1}. ${entry.businessDate}  |  Banca: ${entry.branchId}", titlePaint)
                    line("ID: ${entry.id}  |  ${entry.entryType.name}  |  ${entry.signedAmount.toPlainString()}")
                    line("Saldo: ${entry.balanceBefore.toPlainString()} -> ${entry.balanceAfter.toPlainString()}")
                    wrapText("Descripción: ${entry.description}", 86).forEach { text -> line(text, mutedPaint, 12f) }
                    line("Origen: ${entry.sourceType.name} ${entry.sourceId}  |  Creado por: ${entry.createdBy}", mutedPaint, 18f)
                }
            }
            document.finishPage(page)
            context.contentResolver.openOutputStream(uri)?.use { output ->
                document.writeTo(output)
            } ?: error("No se pudo abrir el archivo seleccionado")
        } finally {
            document.close()
        }
    }
}

private fun pageInfo(pageNumber: Int): PdfDocument.PageInfo =
    PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()

private fun wrapText(text: String, maxCharacters: Int): List<String> =
    text.chunked(maxCharacters)

private fun String.toCsvField(): String =
    if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${replace("\"", "\"\"")}\""
    } else {
        this
    }

private fun todayForFileName(): String =
    SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

private fun todayForDisplay(): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

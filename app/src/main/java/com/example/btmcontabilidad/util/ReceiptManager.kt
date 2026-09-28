package com.example.btmcontabilidad.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.LedgerEntry
import com.example.btmcontabilidad.domain.model.PaymentMethod
import com.example.btmcontabilidad.domain.model.isCollection
import com.example.btmcontabilidad.domain.model.isMoneyDelivery
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptManager {

    private const val COMPANY_NAME = "A & M Sports LLC"
    private const val APP_TAG = "BTM Contabilidad de Bancas"

    private fun currentDateTime(): String {
        return SimpleDateFormat("dd/MM/yyyy · hh:mm a", Locale.forLanguageTag("es-DO")).format(Date())
    }

    /**
     * Limpia y formatea un número de teléfono para la API de WhatsApp.
     * En República Dominicana / USA: 10 dígitos (ej: 809..., 829..., 849...) se prefijan con '1'.
     */
    fun formatPhoneForWhatsApp(phone: String?): String? {
        if (phone.isNullOrBlank()) return null
        val digitsOnly = phone.filter { it.isDigit() }
        if (digitsOnly.length < 10) return null

        return when {
            digitsOnly.length == 10 -> "1$digitsOnly"
            digitsOnly.length == 11 && digitsOnly.startsWith("1") -> digitsOnly
            else -> digitsOnly
        }
    }

    /**
     * Genera el texto formateado del recibo de Cobro.
     */
    fun buildCollectionReceiptText(
        collectionId: String,
        branch: Branch,
        amount: BigDecimal,
        paymentMethod: PaymentMethod,
        reference: String?,
        notes: String?,
        previousBalance: BigDecimal,
        newBalance: BigDecimal
    ): String {
        val dateStr = currentDateTime()
        val formattedAmount = FinancialCalculator.formatCurrency(amount)
        val formattedPrevBalance = FinancialCalculator.formatCurrency(previousBalance)
        val formattedNewBalance = FinancialCalculator.formatCurrency(newBalance)
        val cleanReceiptId = collectionId.takeLast(8).uppercase()

        val sb = StringBuilder()
        sb.appendLine("🧾 *COMPROBANTE DE COBRO*")
        sb.appendLine("*$COMPANY_NAME*")
        sb.appendLine("────────────────────────────")
        sb.appendLine("📍 *Banca:* ${branch.code} — ${branch.name}")
        if (!branch.ownerName.isNullOrBlank()) {
            sb.appendLine("👤 *Titular:* ${branch.ownerName}")
        } else if (!branch.operatorName.isNullOrBlank()) {
            sb.appendLine("👤 *Encargado:* ${branch.operatorName}")
        }
        sb.appendLine("📅 *Fecha y Hora:* $dateStr")
        sb.appendLine("🔖 *No. Recibo:* #COB-$cleanReceiptId")
        sb.appendLine("────────────────────────────")
        sb.appendLine("💵 *MONTO COBRADO:* $formattedAmount")
        sb.appendLine("💳 *Método de Pago:* ${paymentMethod.label}")
        if (!reference.isNullOrBlank()) {
            sb.appendLine("📄 *Referencia:* $reference")
        }
        if (!notes.isNullOrBlank()) {
            sb.appendLine("📝 *Observaciones:* $notes")
        }
        sb.appendLine("────────────────────────────")
        sb.appendLine("📊 *Balance Anterior:* $formattedPrevBalance")
        sb.appendLine("📉 *Nuevo Balance:* $formattedNewBalance")
        sb.appendLine("────────────────────────────")
        sb.appendLine("✅ *Estado:* Verificado y Registrado en Sistema")
        sb.appendLine("_${APP_TAG}_")

        return sb.toString().trimEnd()
    }

    /**
     * Genera el texto formateado del comprobante de Entrega de Dinero a Banca (premios/faltante).
     */
    fun buildMoneyDeliveryReceiptText(
        deliveryId: String,
        branch: Branch,
        amount: BigDecimal,
        reason: String,
        cashBoxName: String?,
        notes: String?,
        previousBalance: BigDecimal,
        newBalance: BigDecimal,
        grossAmount: BigDecimal? = null,
        commissionRate: BigDecimal? = null,
        commissionAmount: BigDecimal? = null
    ): String {
        val dateStr = currentDateTime()
        val formattedAmount = FinancialCalculator.formatCurrency(amount)
        val formattedPrevBalance = FinancialCalculator.formatCurrency(previousBalance)
        val formattedNewBalance = FinancialCalculator.formatCurrency(newBalance)
        val cleanReceiptId = deliveryId.takeLast(8).uppercase()

        val sb = StringBuilder()
        sb.appendLine("💸 *COMPROBANTE DE ENTREGA DE DINERO*")
        sb.appendLine("*$COMPANY_NAME*")
        sb.appendLine("────────────────────────────")
        sb.appendLine("📍 *Banca Receptora:* ${branch.code} — ${branch.name}")
        if (!branch.ownerName.isNullOrBlank()) {
            sb.appendLine("👤 *Recibe:* ${branch.ownerName}")
        } else if (!branch.operatorName.isNullOrBlank()) {
            sb.appendLine("👤 *Recibe:* ${branch.operatorName}")
        }
        sb.appendLine("📅 *Fecha y Hora:* $dateStr")
        sb.appendLine("🔖 *Comprobante:* #ENT-$cleanReceiptId")
        sb.appendLine("────────────────────────────")
        if (grossAmount != null && commissionAmount != null && commissionAmount > BigDecimal.ZERO) {
            sb.appendLine("💵 *Ganancia a Liquidar:* ${FinancialCalculator.formatCurrency(grossAmount)}")
            sb.appendLine("✂️ *Comisión Descontada (${commissionRate?.stripTrailingZeros()?.toPlainString() ?: "0"}%):* -${FinancialCalculator.formatCurrency(commissionAmount)}")
            sb.appendLine("💰 *TOTAL ENTREGADO EN EFECTIVO:* $formattedAmount")
        } else {
            sb.appendLine("💰 *MONTO ENTREGADO:* $formattedAmount")
        }
        sb.appendLine("🎯 *Motivo / Concepto:* $reason")
        if (!cashBoxName.isNullOrBlank()) {
            sb.appendLine("📦 *Origen de Fondos:* $cashBoxName")
        }
        if (!notes.isNullOrBlank()) {
            sb.appendLine("📝 *Observaciones:* $notes")
        }
        sb.appendLine("────────────────────────────")
        sb.appendLine("📊 *Balance Previo Banca:* $formattedPrevBalance")
        sb.appendLine("📈 *Nuevo Balance Banca:* $formattedNewBalance")
        sb.appendLine("────────────────────────────")
        sb.appendLine("✅ *Estado:* Entregado y Contabilizado")
        sb.appendLine("_${APP_TAG}_")

        return sb.toString().trimEnd()
    }

    /**
     * Genera el texto formateado del comprobante de Gasto Operativo (oficina o banca).
     */
    fun buildExpenseReceiptText(
        expenseId: String,
        amount: BigDecimal,
        reason: String,
        category: String? = null,
        cashBoxName: String? = null,
        branchName: String? = null,
        reference: String? = null,
        notes: String? = null
    ): String {
        val dateStr = currentDateTime()
        val formattedAmount = FinancialCalculator.formatCurrency(amount)
        val isComision = category?.contains("Comisión", ignoreCase = true) == true || reason.contains("Comisión", ignoreCase = true)
        val defaultPrefix = if (isComision) "COM" else "GAS"
        val cleanReceiptId = expenseId.takeLast(8).uppercase().ifBlank { defaultPrefix }

        val sb = StringBuilder()
        if (isComision) {
            sb.appendLine("🤝 *COMPROBANTE DE PAGO DE COMISIÓN*")
        } else {
            sb.appendLine("🧾 *COMPROBANTE DE GASTO OPERATIVO*")
        }
        sb.appendLine("*$COMPANY_NAME*")
        sb.appendLine("────────────────────────────")
        sb.appendLine("📅 *Fecha y Hora:* $dateStr")
        sb.appendLine("🔖 *Comprobante:* #$defaultPrefix-$cleanReceiptId")
        sb.appendLine("────────────────────────────")
        sb.appendLine("💵 *MONTO PAGADO:* $formattedAmount")
        sb.appendLine("🎯 *Motivo / Concepto:* $reason")
        if (!category.isNullOrBlank()) {
            sb.appendLine("🏷️ *Categoría:* $category")
        }
        if (!cashBoxName.isNullOrBlank()) {
            sb.appendLine("📦 *Caja Pagadora:* $cashBoxName")
        }
        if (!branchName.isNullOrBlank()) {
            sb.appendLine("📍 *Banca Beneficiaria:* $branchName")
        }
        if (!reference.isNullOrBlank()) {
            sb.appendLine("📄 *No. Factura / Ref:* $reference")
        }
        if (!notes.isNullOrBlank()) {
            sb.appendLine("📝 *Observaciones:* $notes")
        }
        sb.appendLine("────────────────────────────")
        val statusText = if (isComision) "Comisión Pagada y Deducida de Caja" else "Gasto Registrado y Deducido de Caja"
        sb.appendLine("✅ *Estado:* $statusText")
        sb.appendLine("_${APP_TAG}_")

        return sb.toString().trimEnd()
    }

    /**
     * Genera el texto formateado del Estado de Cuenta / Resumen de Banca.
     */
    fun buildBranchStatementText(
        branch: Branch
    ): String {
        val dateStr = currentDateTime()
        val formattedBalance = FinancialCalculator.formatCurrency(branch.currentBalance)

        val statusText = when {
            branch.currentBalance > BigDecimal.ZERO -> "⚠️ Saldo pendiente por cobrar"
            branch.currentBalance < BigDecimal.ZERO -> "🟢 Saldo a favor de la banca (Premios por enviar)"
            else -> "✅ Al día (Balance en RD$ 0.00)"
        }

        val sb = StringBuilder()
        sb.appendLine("📊 *ESTADO DE CUENTA DE BANCA*")
        sb.appendLine("*$COMPANY_NAME*")
        sb.appendLine("────────────────────────────")
        sb.appendLine("📍 *Banca:* ${branch.code} — ${branch.name}")
        if (!branch.ownerName.isNullOrBlank()) {
            sb.appendLine("👤 *Dueño:* ${branch.ownerName}")
        }
        if (!branch.operatorName.isNullOrBlank()) {
            sb.appendLine("👤 *Operador:* ${branch.operatorName}")
        }
        sb.appendLine("📅 *Fecha de Emisión:* $dateStr")
        sb.appendLine("────────────────────────────")
        sb.appendLine("💰 *BALANCE ACTUAL:* $formattedBalance")
        sb.appendLine("📌 *Condición:* $statusText")
        sb.appendLine("────────────────────────────")
        sb.appendLine("ℹ️ _Cualquier duda o aclaración sobre este balance, favor comunicarse con administración._")
        sb.appendLine("_${APP_TAG}_")

        return sb.toString().trimEnd()
    }

    /**
     * Genera el texto formateado para un movimiento general del Libro Diario (Ledger).
     */
    fun buildLedgerReceiptText(
        entry: LedgerEntry,
        branch: Branch?
    ): String {
        val dateStr = currentDateTime()
        val formattedAmount = FinancialCalculator.formatCurrency(entry.signedAmount.abs())
        val cleanId = entry.id.takeLast(8).uppercase()

        val typeTitle = when {
            entry.isCollection() -> "🧾 COMPROBANTE DE COBRO"
            entry.isMoneyDelivery() -> "💸 ENTREGA DE DINERO"
            entry.sourceType.name.contains("ADVANCE", ignoreCase = true) -> "💵 COMPROBANTE DE ADELANTO"
            entry.sourceType.name.contains("SETTLEMENT", ignoreCase = true) -> "📋 LIQUIDACIÓN SEMANAL"
            entry.sourceType.name.contains("MANUAL_RESULT", ignoreCase = true) -> "🎯 RESULTADO DE OPERACIONES"
            else -> "📄 COMPROBANTE DE MOVIMIENTO"
        }

        val sb = StringBuilder()
        sb.appendLine("*$typeTitle*")
        sb.appendLine("*$COMPANY_NAME*")
        sb.appendLine("────────────────────────────")
        if (branch != null) {
            sb.appendLine("📍 *Banca:* ${branch.code} — ${branch.name}")
            if (!branch.ownerName.isNullOrBlank()) {
                sb.appendLine("👤 *Dueño:* ${branch.ownerName}")
            }
        }
        sb.appendLine("📅 *Fecha Operativa:* ${entry.businessDate}")
        sb.appendLine("🔖 *Comprobante:* #MOV-$cleanId")
        sb.appendLine("────────────────────────────")
        sb.appendLine("💰 *MONTO:* $formattedAmount")
        sb.appendLine("📝 *Descripción:* ${entry.description}")
        if (entry.sourceId.isNotBlank()) {
            sb.appendLine("📄 *ID Origen:* ${entry.sourceId}")
        }
        sb.appendLine("────────────────────────────")
        sb.appendLine("📊 *Había:* ${FinancialCalculator.formatCurrency(entry.balanceBefore)}")
        sb.appendLine("📈 *Quedó:* ${FinancialCalculator.formatCurrency(entry.balanceAfter)}")
        sb.appendLine("────────────────────────────")
        sb.appendLine("✅ *Estado:* Verificado en Sistema")
        sb.appendLine("_${APP_TAG}_ · Generado el $dateStr")

        return sb.toString().trimEnd()
    }

    /**
     * Abre WhatsApp directamente hacia el número del destinatario (si se proporciona)
     * o muestra el selector de WhatsApp/aplicaciones con el mensaje pre-cargado.
     */
    fun shareViaWhatsApp(
        context: Context,
        phone: String?,
        messageText: String
    ) {
        val formattedPhone = formatPhoneForWhatsApp(phone)

        if (!formattedPhone.isNullOrBlank()) {
            // Envío directo al chat del número telefónico en WhatsApp
            try {
                val directUri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(messageText)}")
                val intent = Intent(Intent.ACTION_VIEW, directUri).apply {
                    setPackage("com.whatsapp")
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // Si la app regular de WhatsApp no responde, probar con WhatsApp Business
                try {
                    val directUri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(messageText)}")
                    val businessIntent = Intent(Intent.ACTION_VIEW, directUri).apply {
                        setPackage("com.whatsapp.w4b")
                    }
                    context.startActivity(businessIntent)
                    return
                } catch (_: Exception) {
                    // Probar abrir el navegador o chooser web si ninguna app nativa responde
                    try {
                        val directUri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(messageText)}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, directUri))
                        return
                    } catch (_: Exception) {}
                }
            }
        }

        // Si no tiene teléfono configurado o el directo falló, compartir a WhatsApp (o chooser)
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, messageText)
            }
            context.startActivity(shareIntent)
        } catch (_: Exception) {
            try {
                val businessShareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.whatsapp.w4b")
                    putExtra(Intent.EXTRA_TEXT, messageText)
                }
                context.startActivity(businessShareIntent)
            } catch (_: Exception) {
                // Fallback general del sistema
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, messageText)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Compartir Comprobante"))
            }
        }
    }
}

package com.example.btmcontabilidad.data.repository

import android.content.Context
import com.example.btmcontabilidad.data.network.AdvanceResponse
import com.example.btmcontabilidad.data.network.ApiClientFactory
import com.example.btmcontabilidad.data.network.ApiEnvelope
import com.example.btmcontabilidad.data.network.ApiService
import com.example.btmcontabilidad.data.network.BranchResponse
import com.example.btmcontabilidad.data.network.BranchRequest
import com.example.btmcontabilidad.data.network.CashBoxResponse
import com.example.btmcontabilidad.data.network.CashMovementRequest
import com.example.btmcontabilidad.data.network.CashMovementResponse
import com.example.btmcontabilidad.data.network.CollectionResponse
import com.example.btmcontabilidad.data.network.CreateAdvanceRequest
import com.example.btmcontabilidad.data.network.CreateCollectionRequest
import com.example.btmcontabilidad.data.network.CreateReversalRequest
import com.example.btmcontabilidad.data.network.CreateWeeklySettlementRequest
import com.example.btmcontabilidad.data.network.DashboardResponse
import com.example.btmcontabilidad.data.network.LedgerEntryResponse
import com.example.btmcontabilidad.data.network.LoginRequest
import com.example.btmcontabilidad.data.network.WeeklySettlementResponse
import com.example.btmcontabilidad.data.session.SessionStore
import com.example.btmcontabilidad.data.settings.ApiSettings
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.Advance
import com.example.btmcontabilidad.domain.model.AdvanceStatus
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.BranchStatus
import com.example.btmcontabilidad.domain.model.CashBox
import com.example.btmcontabilidad.domain.model.CashMovement
import com.example.btmcontabilidad.domain.model.CashMovementType
import com.example.btmcontabilidad.domain.model.Collection
import com.example.btmcontabilidad.domain.model.CollectionStatus
import com.example.btmcontabilidad.domain.model.LedgerEntry
import com.example.btmcontabilidad.domain.model.LedgerEntryType
import com.example.btmcontabilidad.domain.model.LedgerSourceType
import com.example.btmcontabilidad.domain.model.PaymentMethod
import com.example.btmcontabilidad.domain.model.WeeklySettlement
import com.example.btmcontabilidad.domain.repository.AdvanceRepository
import com.example.btmcontabilidad.domain.repository.BranchRepository
import com.example.btmcontabilidad.domain.repository.CollectionRepository
import com.example.btmcontabilidad.domain.repository.CashBoxRepository
import com.example.btmcontabilidad.domain.repository.LedgerRepository
import com.example.btmcontabilidad.domain.repository.WeeklySettlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class UnauthenticatedException : IllegalStateException("Debe iniciar sesión para consultar el servidor")
class BackendResponseException(message: String) : IllegalStateException(message)
class UnsupportedBackendOperationException(operation: String) : UnsupportedOperationException(
    "El servidor no expone la operación: $operation"
)

data class DashboardSnapshot(
    val receivableTotal: BigDecimal,
    val branchCreditTotal: BigDecimal,
    val netPosition: BigDecimal,
    val collectionsTotal: BigDecimal,
    val advancesTotal: BigDecimal,
    val currencyCode: String,
    val recentActivity: List<LedgerEntry>
)

class BackendApiProvider(context: Context) {
    private val appContext = context.applicationContext
    private val settings = ApiSettings(appContext)
    val session = SessionStore(appContext)

    suspend fun api(): ApiService = ApiClientFactory.create(settings.baseUrl.first())

    suspend fun login(email: String, password: String, deviceName: String) {
        require(email.isNotBlank()) { "Debe ingresar un correo electrónico" }
        require(password.isNotBlank()) { "Debe ingresar una contraseña" }
        require(deviceName.isNotBlank()) { "El nombre del dispositivo no es válido" }

        val token = api().login(LoginRequest(email.trim(), password, deviceName)).dataOrThrow().token
            .takeIf { it.isNotBlank() }
            ?: throw BackendResponseException("El servidor no incluyó un token de sesión")
        session.saveToken(token)
    }

    fun logout() {
        session.clear()
    }

    fun isAuthenticated(): Boolean = session.token()?.isNotBlank() == true

    fun authorization(): String = session.token()
        ?.takeIf { it.isNotBlank() }
        ?.let { "Bearer $it" }
        ?: throw UnauthenticatedException()
}

private suspend fun <T> Response<ApiEnvelope<T>>.dataOrThrow(): T {
    if (!isSuccessful) {
        throw BackendResponseException("El servidor respondió HTTP ${code()}")
    }
    val envelope = body() ?: throw BackendResponseException("El servidor devolvió una respuesta vacía")
    if (!envelope.success || envelope.data == null) {
        throw BackendResponseException(envelope.message?.ifBlank { "El servidor no pudo completar la operación" }
            ?: "El servidor no pudo completar la operación")
    }
    return envelope.data
}

private suspend fun <T> Response<ApiEnvelope<T>>.successOrThrow() {
    if (!isSuccessful) {
        throw BackendResponseException("El servidor respondió HTTP ${code()}")
    }
    val envelope = body() ?: throw BackendResponseException("El servidor devolvió una respuesta vacía")
    if (!envelope.success) {
        throw BackendResponseException(envelope.message?.ifBlank { "El servidor no pudo completar la operación" }
            ?: "El servidor no pudo completar la operación")
    }
}

private fun <T> remoteFlow(block: suspend () -> T): Flow<T> = flow { emit(block()) }

class BackendBranchRepository(private val provider: BackendApiProvider) : BranchRepository {
    override fun getBranches(): Flow<List<Branch>> = remoteFlow {
        provider.api().branches(provider.authorization()).dataOrThrow().map { it.toDomain() }
    }

    override fun getBranchById(id: String): Flow<Branch?> = remoteFlow {
        provider.api().branch(provider.authorization(), id).dataOrThrow().toDomain()
    }

    override fun getBranchByCode(code: String): Flow<Branch?> = remoteFlow {
        provider.api().branches(provider.authorization()).dataOrThrow()
            .firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?.toDomain()
    }

    override suspend fun updateBalance(branchId: String, newBalance: BigDecimal): Branch =
        throw UnsupportedBackendOperationException("actualizar saldo de banca")

    override suspend fun addBranch(branch: Branch): Branch =
        provider.api().createBranch(provider.authorization(), branch.toRequest()).dataOrThrow().toDomain()

    override suspend fun updateBranch(branch: Branch): Branch =
        provider.api().updateBranch(provider.authorization(), branch.id, branch.toRequest()).dataOrThrow().toDomain()

    override suspend fun deleteBranch(id: String): Boolean {
        provider.api().deleteBranch(provider.authorization(), id).successOrThrow()
        return true
    }
}

class BackendCollectionRepository(private val provider: BackendApiProvider) : CollectionRepository {
    override fun getCollections(): Flow<List<Collection>> = remoteFlow {
        provider.api().collections(provider.authorization()).dataOrThrow().map { it.toDomain() }
    }

    override fun getCollectionsForBranch(branchId: String): Flow<List<Collection>> = remoteFlow {
        provider.api().collections(provider.authorization()).dataOrThrow()
            .filter { it.branch_id?.toString() == branchId }
            .map { it.toDomain() }
    }

    override fun getCollectionById(id: String): Flow<Collection?> = remoteFlow {
        provider.api().collections(provider.authorization()).dataOrThrow()
            .firstOrNull { it.id?.toString() == id }
            ?.toDomain()
    }

    override suspend fun addCollection(collection: Collection): Collection {
        val branchId = collection.branchId.toLongOrNull()
            ?: throw BackendResponseException("El identificador de la banca no es válido")
        return provider.api().createCollection(
            provider.authorization(), UUID.randomUUID().toString(),
            CreateCollectionRequest(branchId, collection.amount.toPlainString(), collection.businessDate,
                collection.paymentMethod.toApiValue(), collection.reference, collection.notes)
        ).dataOrThrow().toDomain()
    }

    override suspend fun cancelCollection(id: String, reason: String): Collection =
        throw UnsupportedBackendOperationException("anular cobro")
}

class BackendAdvanceRepository(private val provider: BackendApiProvider) : AdvanceRepository {
    override fun getAdvances(): Flow<List<Advance>> = remoteFlow {
        provider.api().advances(provider.authorization()).dataOrThrow().map { it.toDomain() }
    }

    override fun getAdvancesForBranch(branchId: String): Flow<List<Advance>> = remoteFlow {
        provider.api().advances(provider.authorization()).dataOrThrow()
            .filter { it.branch_id?.toString() == branchId }
            .map { it.toDomain() }
    }

    override fun getAdvanceById(id: String): Flow<Advance?> = remoteFlow {
        provider.api().advances(provider.authorization()).dataOrThrow()
            .firstOrNull { it.id?.toString() == id }
            ?.toDomain()
    }

    override suspend fun addAdvance(advance: Advance): Advance {
        val branchId = advance.branchId.toLongOrNull()
            ?: throw BackendResponseException("El identificador de la banca no es válido")
        return provider.api().createAdvance(
            provider.authorization(), UUID.randomUUID().toString(),
            CreateAdvanceRequest(branchId, advance.amount.toPlainString(), advance.reason, advance.businessDate, advance.notes)
        ).dataOrThrow().toDomain()
    }

    override suspend fun cancelAdvance(id: String, reason: String): Advance =
        throw UnsupportedBackendOperationException("anular adelanto")
}

class BackendLedgerRepository(private val provider: BackendApiProvider) : LedgerRepository {
    override fun getLedgerEntries(): Flow<List<LedgerEntry>> = remoteFlow {
        provider.api().ledger(provider.authorization()).dataOrThrow().map { it.toDomain() }
    }

    override fun getBranchLedger(branchId: String): Flow<List<LedgerEntry>> = remoteFlow {
        provider.api().ledger(provider.authorization(), branchId).dataOrThrow().map { it.toDomain() }
    }

    override suspend fun addEntry(entry: LedgerEntry): LedgerEntry =
        throw UnsupportedBackendOperationException("crear entrada de libro mayor")

    override suspend fun reverseEntry(
        entryId: String,
        reason: String,
        reversedBy: String,
        balanceBefore: BigDecimal?
    ): LedgerEntry {
        val id = entryId.toLongOrNull() ?: throw BackendResponseException("El identificador del asiento no es válido")
        return provider.api().createReversal(
            provider.authorization(), UUID.randomUUID().toString(), id,
            CreateReversalRequest(
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                reason
            )
        ).dataOrThrow().toDomain()
    }
}

class BackendCashBoxRepository(private val provider: BackendApiProvider) : CashBoxRepository {
    override fun getCashBox(): Flow<CashBox> = remoteFlow {
        provider.api().cashBox(provider.authorization()).dataOrThrow().toDomain()
    }

    override suspend fun addIncome(movement: CashMovement): CashMovement =
        provider.api().createCashIncome(
            provider.authorization(), UUID.randomUUID().toString(), movement.toRequest()
        ).dataOrThrow().toDomain(CashMovementType.INCOME)

    override suspend fun addExpense(movement: CashMovement): CashMovement =
        provider.api().createCashExpense(
            provider.authorization(), UUID.randomUUID().toString(), movement.toRequest()
        ).dataOrThrow().toDomain(CashMovementType.EXPENSE)

    override suspend fun transferToBranch(movement: CashMovement): CashMovement =
        provider.api().createCashBranchTransfer(
            provider.authorization(), UUID.randomUUID().toString(), movement.toRequest()
        ).dataOrThrow().toDomain(CashMovementType.BRANCH_TRANSFER)
}

class BackendWeeklySettlementRepository(private val provider: BackendApiProvider) : WeeklySettlementRepository {
    override fun getForBranch(branchId: String): Flow<List<WeeklySettlement>> = remoteFlow {
        provider.api().weeklySettlements(provider.authorization(), branchId).dataOrThrow().map { it.toDomain() }
    }

    override suspend fun add(settlement: WeeklySettlement): WeeklySettlement {
        val branchId = settlement.branchId.toLongOrNull()
            ?: throw BackendResponseException("El identificador de la banca no es válido")
        return provider.api().createWeeklySettlement(
            provider.authorization(), UUID.randomUUID().toString(),
            CreateWeeklySettlementRequest(
                branch_id = branchId,
                week_start = settlement.weekStart,
                week_end = settlement.weekEnd,
                sales_amount = settlement.salesAmount.toPlainString(),
                prizes_amount = settlement.prizesAmount.toPlainString(),
                cash_delivered_amount = settlement.cashDeliveredAmount.toPlainString(),
                notes = settlement.notes
            )
        ).dataOrThrow().toDomain()
    }
}

suspend fun BackendApiProvider.dashboard(): DashboardSnapshot {
    val response = api().dashboard(authorization()).dataOrThrow()
    return DashboardSnapshot(
        receivableTotal = response.receivable_total.toAmount(),
        branchCreditTotal = response.branch_credit_total.toAmount(),
        netPosition = response.net_position.toAmount(),
        collectionsTotal = response.collections_total.toAmount(),
        advancesTotal = response.advances_total.toAmount(),
        currencyCode = response.currency_code ?: "USD",
        recentActivity = response.recent_activity.orEmpty().map { it.toDomain() }
    )
}

private fun CashBoxResponse.toDomain(): CashBox = CashBox(
    currencyCode = currency_code.required("moneda de caja"),
    currentBalance = current_balance.toAmount(),
    entries = entries?.map { it.toDomain() }
        ?: throw BackendResponseException("El servidor no incluyó los movimientos de caja")
)

private fun CashMovement.toRequest(): CashMovementRequest = CashMovementRequest(
    amount = FinancialCalculator.roundMoney(amount).toPlainString(),
    business_date = businessDate,
    reason = reason,
    branch_id = branchId?.toLongOrNull()
        ?: if (type == CashMovementType.BRANCH_TRANSFER) {
            throw BackendResponseException("El identificador de la banca no es válido")
        } else null,
    reference = reference,
    notes = notes
)

private fun CashMovementResponse.toDomain(expectedType: CashMovementType? = null): CashMovement {
    val apiType = movement_type ?: type
    return CashMovement(
        id = id.required("id del movimiento de caja"),
        type = apiType.toCashMovementType() ?: expectedType
            ?: throw BackendResponseException("El servidor no incluyó el tipo de movimiento de caja"),
        amount = amount.toAmount(),
        businessDate = business_date.required("fecha del movimiento de caja"),
        reason = reason.required("motivo del movimiento de caja"),
        branchId = branch_id?.toString(),
        reference = reference,
        notes = notes,
        createdAt = created_at
    )
}

private fun String?.toCashMovementType(): CashMovementType? = when (this?.trim()?.lowercase()) {
    "income", "entrada" -> CashMovementType.INCOME
    "expense", "expenses", "retiro", "gasto" -> CashMovementType.EXPENSE
    "branch_transfer", "branch-transfer", "transfer", "transferencia" -> CashMovementType.BRANCH_TRANSFER
    else -> null
}

private fun BranchResponse.collectionsOrThrow(): List<CollectionResponse> =
    collections ?: throw BackendResponseException("La banca no incluyó cobros")

private fun BranchResponse.advancesOrThrow(): List<AdvanceResponse> =
    advances ?: throw BackendResponseException("La banca no incluyó adelantos")

private fun BranchResponse.toDomain() = Branch(
    id = id.required("id de banca"),
    code = code.required("código de banca"),
    name = name.required("nombre de banca"),
    description = description,
    route = route.orEmpty(),
    operatorName = operator_name.orEmpty(),
    currentBalance = current_balance.toAmount(),
    status = status.toEnum(BranchStatus.ACTIVE)
)

private fun WeeklySettlementResponse.toDomain() = WeeklySettlement(
    id = id.required("id del cuadre semanal"),
    branchId = branch_id.required("banca del cuadre semanal"),
    weekStart = week_start.required("inicio del cuadre semanal"),
    weekEnd = week_end.required("fin del cuadre semanal"),
    salesAmount = sales_amount.toAmount(),
    prizesAmount = prizes_amount.toAmount(),
    cashDeliveredAmount = cash_delivered_amount.toAmount(),
    weeklyBalance = weekly_balance.toAmount(),
    balanceBefore = balance_before.toAmount(),
    balanceAfter = balance_after.toAmount(),
    notes = notes,
    status = status ?: "confirmed"
)

private fun Branch.toRequest() = BranchRequest(
    code = code.trim(),
    name = name.trim(),
    description = description?.trim()?.takeIf { it.isNotEmpty() },
    route = route.trim(),
    operator_name = operatorName.trim(),
    status = status.name.lowercase()
)

private fun CollectionResponse.toDomain() = Collection(
    id = id.required("id de cobro"),
    branchId = branch_id.required("banca del cobro"),
    amount = amount.toAmount(),
    paymentMethod = payment_method.toPaymentMethod(),
    reference = reference,
    businessDate = business_date.required("fecha del cobro"),
    notes = notes,
    status = status.toEnum(CollectionStatus.REGISTERED)
)

private fun AdvanceResponse.toDomain() = Advance(
    id = id.required("id de adelanto"),
    branchId = branch_id.required("banca del adelanto"),
    amount = amount.toAmount(),
    reason = reason.required("motivo del adelanto"),
    businessDate = business_date.required("fecha del adelanto"),
    notes = notes,
    status = status.toEnum(AdvanceStatus.REGISTERED)
)

private fun LedgerEntryResponse.toDomain() = LedgerEntry(
    id = id.required("id del asiento"),
    branchId = branch_id.required("banca del asiento"),
    sourceType = source_type.toEnum(LedgerSourceType.ADJUSTMENT),
    sourceId = source_id?.toString() ?: id.required("origen del asiento"),
    entryType = entry_type.toEnum(LedgerEntryType.DEBIT),
    signedAmount = signed_amount.toAmount(),
    balanceBefore = balance_before.toAmount(),
    balanceAfter = balance_after.toAmount(),
    businessDate = business_date.required("fecha del asiento"),
    description = description.orEmpty(),
    createdBy = created_by.orEmpty(),
    reversalOfEntryId = reversal_of_entry_id?.toString()
)

private fun Long?.required(field: String): String = this?.toString()
    ?: throw BackendResponseException("El servidor no incluyó $field")

private fun String?.required(field: String): String = this?.takeIf { it.isNotBlank() }
    ?: throw BackendResponseException("El servidor no incluyó $field")

private fun String?.toAmount(): BigDecimal = this?.toBigDecimalOrNull()
    ?: throw BackendResponseException("El servidor devolvió un monto inválido")

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    enumValues<T>().firstOrNull { it.name == this?.trim()?.uppercase() } ?: default

private fun PaymentMethod.toApiValue(): String = when (this) {
    PaymentMethod.EFECTIVO -> "cash"
    PaymentMethod.TRANSFERENCIA -> "transfer"
    PaymentMethod.OTRO -> "other"
}

private fun String?.toPaymentMethod(): PaymentMethod = when (this?.lowercase()) {
    "cash" -> PaymentMethod.EFECTIVO
    "transfer" -> PaymentMethod.TRANSFERENCIA
    else -> PaymentMethod.OTRO
}

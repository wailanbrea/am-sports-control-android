package com.example.btmcontabilidad.data.repository

import android.content.Context
import com.example.btmcontabilidad.data.network.AdvanceResponse
import com.example.btmcontabilidad.data.network.ApiClientFactory
import com.example.btmcontabilidad.data.network.ApiEnvelope
import com.example.btmcontabilidad.data.network.ApiService
import com.example.btmcontabilidad.data.network.BranchResponse
import com.example.btmcontabilidad.data.network.BranchRequest
import com.example.btmcontabilidad.data.network.CashBoxDto
import com.example.btmcontabilidad.data.network.CashBoxResponse
import com.example.btmcontabilidad.data.network.CashMovementRequest
import com.example.btmcontabilidad.data.network.CashMovementResponse
import com.example.btmcontabilidad.data.network.CollectionResponse
import com.example.btmcontabilidad.data.network.CreateAdvanceRequest
import com.example.btmcontabilidad.data.network.CreateCashBoxRequest
import com.example.btmcontabilidad.data.network.CreateCollectionRequest
import com.example.btmcontabilidad.data.network.CreateManualResultRequest
import com.example.btmcontabilidad.data.network.CreateMoneyDeliveryRequest
import com.example.btmcontabilidad.data.network.CreateReversalRequest
import com.example.btmcontabilidad.data.network.CreateWeeklySettlementRequest
import com.example.btmcontabilidad.data.network.DashboardResponse
import com.example.btmcontabilidad.data.network.LedgerEntryResponse
import com.example.btmcontabilidad.data.network.LoginRequest
import com.example.btmcontabilidad.data.network.ManualResultResponse
import com.example.btmcontabilidad.data.network.MoneyDeliveryResponse
import com.example.btmcontabilidad.data.network.WeeklySettlementResponse
import com.example.btmcontabilidad.data.session.SessionStore
import com.example.btmcontabilidad.data.settings.ApiSettings
import com.example.btmcontabilidad.domain.calculator.FinancialCalculator
import com.example.btmcontabilidad.domain.model.Advance
import com.example.btmcontabilidad.domain.model.AdvanceStatus
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.BranchStatus
import com.example.btmcontabilidad.domain.model.CashBox
import com.example.btmcontabilidad.domain.model.CashBoxEntity
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
    val moneyDeliveredTotal: BigDecimal = BigDecimal.ZERO,
    val currencyCode: String,
    val recentActivity: List<LedgerEntry>,
    val cashBalance: BigDecimal = BigDecimal.ZERO
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

        val loginData = api().login(LoginRequest(email.trim(), password, deviceName)).dataOrThrow()
        val token = loginData.token.takeIf { it.isNotBlank() }
            ?: throw BackendResponseException("El servidor no incluyó un token de sesión")
        session.saveToken(token)
        session.saveUser(loginData.user.name, loginData.user.email, loginData.user.role ?: "collector")
    }

    fun logout() {
        session.clear()
    }

    fun isAuthenticated(): Boolean = session.token()?.isNotBlank() == true

    fun currentUserName(): String? = session.userName()
    fun currentUserRole(): String = session.userRole()
    val isAdmin: Boolean get() = session.isAdmin
    val isCollector: Boolean get() = session.isCollector

    fun currentUserEmail(): String? = session.userEmail()

    suspend fun fetchCurrentUser(): com.example.btmcontabilidad.data.network.ApiUser? {
        val token = session.token()?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            val user = api().me("Bearer $token").dataOrThrow()
            session.saveUser(user.name, user.email, user.role ?: if (isAdmin) "admin" else "collector")
            user
        }.getOrNull()
    }

    fun authorization(): String = session.token()
        ?.takeIf { it.isNotBlank() }
        ?.let { "Bearer $it" }
        ?: throw UnauthenticatedException()
}

internal suspend fun <T> Response<ApiEnvelope<T>>.dataOrThrow(): T {
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

internal suspend fun <T> Response<ApiEnvelope<T>>.successOrThrow() {
    if (!isSuccessful) {
        throw BackendResponseException("El servidor respondió HTTP ${code()}")
    }
    val envelope = body() ?: throw BackendResponseException("El servidor devolvió una respuesta vacía")
    if (!envelope.success) {
        throw BackendResponseException(envelope.message?.ifBlank { "El servidor no pudo completar la operación" }
            ?: "El servidor no pudo completar la operación")
    }
}

internal fun <T> remoteFlow(block: suspend () -> T): Flow<T> = flow { emit(block()) }


class BackendBranchRepository(private val provider: BackendApiProvider) : BranchRepository {
    @Volatile private var cachedBranches: List<Branch>? = null

    override fun invalidateCache() {
        cachedBranches = null
    }

    override suspend fun fetchBranches(): List<Branch> {
        val fresh = provider.api().branches(provider.authorization()).dataOrThrow().map { it.toDomain() }
        cachedBranches = fresh
        return fresh
    }

    override fun getBranches(): Flow<List<Branch>> = flow {
        cachedBranches?.let { emit(it) }
        try {
            val fresh = provider.api().branches(provider.authorization()).dataOrThrow().map { it.toDomain() }
            cachedBranches = fresh
            emit(fresh)
        } catch (e: Exception) {
            if (cachedBranches == null) throw e
        }
    }

    override fun getBranchById(id: String): Flow<Branch?> = flow {
        val cached = cachedBranches?.firstOrNull { it.id == id || (it.code.isNotBlank() && it.code.equals(id, ignoreCase = true)) }
        if (cached != null) emit(cached)
        try {
            val fresh = if (!provider.isAdmin) {
                val branches = provider.api().branches(provider.authorization()).dataOrThrow().map { it.toDomain() }
                cachedBranches = branches
                branches.firstOrNull { it.id == id || (it.code.isNotBlank() && it.code.equals(id, ignoreCase = true)) }
            } else {
                try {
                    val freshBranch = provider.api().branch(provider.authorization(), id).dataOrThrow().toDomain()
                    cachedBranches = cachedBranches?.map { if (it.id == freshBranch.id) freshBranch else it }
                    freshBranch
                } catch (e: Exception) {
                    val branches = provider.api().branches(provider.authorization()).dataOrThrow().map { it.toDomain() }
                    cachedBranches = branches
                    branches.firstOrNull { it.id == id || (it.code.isNotBlank() && it.code.equals(id, ignoreCase = true)) } ?: throw e
                }
            }
            if (fresh != null) {
                emit(fresh)
            } else if (cached == null) {
                emit(null)
            }
        } catch (e: Exception) {
            if (cached == null) {
                val fallback = cachedBranches?.firstOrNull { it.id == id || (it.code.isNotBlank() && it.code.equals(id, ignoreCase = true)) }
                if (fallback != null) {
                    emit(fallback)
                } else {
                    throw e
                }
            }
        }
    }

    override fun getBranchByCode(code: String): Flow<Branch?> = flow {
        val cached = cachedBranches?.firstOrNull { it.code.equals(code, ignoreCase = true) }
        if (cached != null) emit(cached)
        try {
            val fresh = provider.api().branches(provider.authorization()).dataOrThrow()
                .firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?.toDomain()
            emit(fresh)
        } catch (e: Exception) {
            if (cached == null) throw e
        }
    }

    override suspend fun updateBalance(branchId: String, newBalance: BigDecimal): Branch =
        throw UnsupportedBackendOperationException("actualizar saldo de banca")

    override suspend fun addBranch(branch: Branch): Branch {
        val created = provider.api().createBranch(provider.authorization(), branch.toRequest()).dataOrThrow().toDomain()
        cachedBranches = (cachedBranches ?: emptyList()) + created
        return created
    }

    override suspend fun updateBranch(branch: Branch): Branch {
        val updated = provider.api().updateBranch(provider.authorization(), branch.id, branch.toRequest()).dataOrThrow().toDomain()
        cachedBranches = cachedBranches?.map { if (it.id == updated.id) updated else it } ?: listOf(updated)
        return updated
    }

    override suspend fun deleteBranch(id: String): Boolean {
        provider.api().deleteBranch(provider.authorization(), id).successOrThrow()
        cachedBranches = cachedBranches?.filterNot { it.id == id }
        return true
    }

    override suspend fun assignCollector(branchId: String, collectorUserId: Long?): Branch {
        val updated = provider.api().assignCollector(
            provider.authorization(),
            branchId,
            com.example.btmcontabilidad.data.network.AssignCollectorRequest(collectorUserId)
        ).dataOrThrow().toDomain()
        cachedBranches = cachedBranches?.map { if (it.id == updated.id) updated else it }
        return updated
    }

    override suspend fun absorbLoss(
        branchId: String,
        amount: BigDecimal?,
        cashBoxId: Long?,
        deductCashBox: Boolean,
        businessDate: String?,
        reason: String?,
        notes: String?
    ): Boolean {
        val req = com.example.btmcontabilidad.data.network.AbsorbLossRequest(
            amount = amount?.toPlainString(),
            business_date = businessDate,
            cash_box_id = cashBoxId,
            deduct_cash_box = deductCashBox,
            reason = reason,
            notes = notes
        )
        provider.api().absorbLoss(
            provider.authorization(),
            UUID.randomUUID().toString(),
            branchId,
            req
        ).successOrThrow()
        invalidateCache()
        runCatching { RepositoryContainer.Instance.clearCaches() }
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
        val result = provider.api().createCollection(
            provider.authorization(), UUID.randomUUID().toString(),
            CreateCollectionRequest(
                branch_id = branchId,
                amount = collection.amount.toPlainString(),
                business_date = collection.businessDate,
                payment_method = collection.paymentMethod.toApiValue(),
                reference = collection.reference,
                notes = collection.notes,
                force_overcollection = if (provider.isAdmin) true else null
            )
        ).dataOrThrow().toDomain()
        runCatching { RepositoryContainer.Instance.clearCaches() }
        return result
    }

    override suspend fun updateCollection(collection: Collection): Collection {
        val req = com.example.btmcontabilidad.data.network.UpdateCollectionRequest(
            amount = collection.amount.toPlainString(),
            business_date = collection.businessDate,
            payment_method = collection.paymentMethod.toApiValue(),
            reference = collection.reference,
            notes = collection.notes
        )
        val result = provider.api().updateCollection(
            provider.authorization(),
            collection.id,
            req
        ).dataOrThrow().toDomain()
        runCatching { RepositoryContainer.Instance.clearCaches() }
        return result
    }

    override suspend fun cancelCollection(id: String, reason: String): Collection =
        throw UnsupportedBackendOperationException("anular cobro")
}

class BackendAdvanceRepository(private val provider: BackendApiProvider) : AdvanceRepository {
    override fun getAdvances(): Flow<List<Advance>> = remoteFlow {
        if (!provider.isAdmin) {
            emptyList()
        } else {
            try {
                provider.api().advances(provider.authorization()).dataOrThrow().map { it.toDomain() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override fun getAdvancesForBranch(branchId: String): Flow<List<Advance>> = remoteFlow {
        if (!provider.isAdmin) {
            emptyList()
        } else {
            try {
                provider.api().advances(provider.authorization()).dataOrThrow()
                    .filter { it.branch_id?.toString() == branchId }
                    .map { it.toDomain() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override fun getAdvanceById(id: String): Flow<Advance?> = remoteFlow {
        if (!provider.isAdmin) {
            null
        } else {
            try {
                provider.api().advances(provider.authorization()).dataOrThrow()
                    .firstOrNull { it.id?.toString() == id }
                    ?.toDomain()
            } catch (e: Exception) {
                null
            }
        }
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
    @Volatile private var cachedLedger: List<LedgerEntry>? = null

    fun invalidateCache() {
        cachedLedger = null
    }

    override fun getLedgerEntries(): Flow<List<LedgerEntry>> = flow {
        cachedLedger?.let { emit(it) }
        try {
            val fresh = provider.api().ledger(provider.authorization()).dataOrThrow().map { it.toDomain() }
            cachedLedger = fresh
            emit(fresh)
        } catch (e: Exception) {
            if (cachedLedger == null) throw e
        }
    }

    override fun getBranchLedger(branchId: String): Flow<List<LedgerEntry>> = flow {
        val cached = cachedLedger?.filter { it.branchId == branchId }
        if (!cached.isNullOrEmpty()) emit(cached)
        try {
            val fresh = provider.api().ledger(provider.authorization(), branchId).dataOrThrow()
                .filter { it.branch_id?.toString() == branchId }
                .map { it.toDomain() }
            emit(fresh)
        } catch (e: Exception) {
            if (cached.isNullOrEmpty()) throw e
        }
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
        invalidateCache()
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
    override fun getCashBox(cashBoxId: Long?): Flow<CashBox> = remoteFlow {
        provider.api().cashBox(provider.authorization(), cashBoxId).dataOrThrow().toDomain()
    }

    override suspend fun getCashBoxes(): List<CashBoxEntity> = runCatching {
        val payload = provider.api().cashBoxes(provider.authorization()).dataOrThrow()
        payload.boxes.orEmpty().map { it.toDomain() }
    }.getOrElse {
        emptyList()
    }

    override suspend fun createCashBox(
        name: String,
        initialBalance: BigDecimal,
        description: String?,
        isDefault: Boolean
    ): CashBoxEntity {
        val response = provider.api().createCashBox(
            provider.authorization(),
            UUID.randomUUID().toString(),
            CreateCashBoxRequest(
                name = name.trim(),
                initial_balance = FinancialCalculator.roundMoney(initialBalance).toPlainString(),
                description = description?.trim()?.ifBlank { null },
                is_default = isDefault
            )
        ).dataOrThrow()
        return response.toDomain()
    }

    override suspend fun addIncome(movement: CashMovement, cashBoxId: Long?): CashMovement =
        provider.api().createCashIncome(
            provider.authorization(), UUID.randomUUID().toString(), movement.toRequest(cashBoxId)
        ).dataOrThrow().toDomain(CashMovementType.INCOME)

    override suspend fun addExpense(movement: CashMovement, cashBoxId: Long?): CashMovement =
        provider.api().createCashExpense(
            provider.authorization(), UUID.randomUUID().toString(), movement.toRequest(cashBoxId)
        ).dataOrThrow().toDomain(CashMovementType.EXPENSE)

    override suspend fun transferToBranch(movement: CashMovement, cashBoxId: Long?): CashMovement =
        provider.api().createCashBranchTransfer(
            provider.authorization(), UUID.randomUUID().toString(), movement.toRequest(cashBoxId)
        ).dataOrThrow().toDomain(CashMovementType.BRANCH_TRANSFER)
}

class BackendWeeklySettlementRepository(private val provider: BackendApiProvider) : WeeklySettlementRepository {
    override fun getForBranch(branchId: String): Flow<List<WeeklySettlement>> = remoteFlow {
        if (!provider.isAdmin) {
            emptyList()
        } else {
            try {
                provider.api().weeklySettlements(provider.authorization(), branchId).dataOrThrow()
                    .filter { it.branch_id?.toString() == branchId }
                    .map { it.toDomain() }
            } catch (e: Exception) {
                emptyList()
            }
        }
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
                commission_rate = settlement.commissionRate.toPlainString(),
                cash_delivered_amount = settlement.cashDeliveredAmount.toPlainString(),
                absorb_loss = settlement.settlementType == "loss_absorbed" || settlement.lossAbsorbedAmount > BigDecimal.ZERO,
                notes = settlement.notes
            )
        ).dataOrThrow().toDomain()
    }
}

data class ManualResultEntry(
    val id: Long,
    val branchId: Long,
    val amount: BigDecimal,
    val classification: String,
    val businessDate: String,
    val notes: String? = null,
    val requiresMoneyDelivery: Boolean = false,
    val balanceAfter: BigDecimal = BigDecimal.ZERO
)

data class MoneyDeliveryEntry(
    val id: Long,
    val branchId: Long,
    val manualResultId: Long? = null,
    val suggestedAmount: BigDecimal = BigDecimal.ZERO,
    val grossAmount: BigDecimal = BigDecimal.ZERO,
    val commissionRate: BigDecimal = BigDecimal.ZERO,
    val commissionAmount: BigDecimal = BigDecimal.ZERO,
    val deliveredAmount: BigDecimal = BigDecimal.ZERO,
    val businessDate: String,
    val reason: String,
    val notes: String? = null,
    val branchBalanceAfter: BigDecimal = BigDecimal.ZERO
)

data class MoneyDeliveriesSummary(
    val deliveries: List<MoneyDeliveryEntry>,
    val totalDelivered: BigDecimal
)

interface ManualResultRepository {
    suspend fun addResult(branchId: String, amount: BigDecimal, businessDate: String, notes: String? = null): ManualResultEntry
}

interface MoneyDeliveryRepository {
    suspend fun addDelivery(
        branchId: String,
        amount: BigDecimal,
        grossAmount: BigDecimal? = null,
        commissionRate: BigDecimal? = null,
        commissionAmount: BigDecimal? = null,
        suggestedAmount: BigDecimal? = null,
        manualResultId: Long? = null,
        businessDate: String,
        reason: String,
        notes: String? = null,
        cashBoxId: Long? = null
    ): MoneyDeliveryEntry
    fun getDeliveries(period: String? = null, branchId: String? = null): Flow<MoneyDeliveriesSummary>
}

class BackendManualResultRepository(private val provider: BackendApiProvider) : ManualResultRepository {
    override suspend fun addResult(
        branchId: String,
        amount: BigDecimal,
        businessDate: String,
        notes: String?
    ): ManualResultEntry {
        val bId = branchId.toLongOrNull() ?: throw BackendResponseException("El identificador de la banca no es válido")
        val response = provider.api().createResult(
            provider.authorization(),
            UUID.randomUUID().toString(),
            CreateManualResultRequest(
                branch_id = bId,
                amount = amount.toPlainString(),
                business_date = businessDate,
                notes = notes
            )
        ).dataOrThrow()
        return ManualResultEntry(
            id = response.id ?: 0L,
            branchId = response.branch_id ?: bId,
            amount = response.amount.toAmount(),
            classification = response.classification ?: "zero",
            businessDate = response.business_date ?: businessDate,
            notes = response.notes,
            requiresMoneyDelivery = response.requires_money_delivery == true,
            balanceAfter = response.balance_after.toAmount()
        )
    }
}

class BackendMoneyDeliveryRepository(private val provider: BackendApiProvider) : MoneyDeliveryRepository {
    override suspend fun addDelivery(
        branchId: String,
        amount: BigDecimal,
        grossAmount: BigDecimal?,
        commissionRate: BigDecimal?,
        commissionAmount: BigDecimal?,
        suggestedAmount: BigDecimal?,
        manualResultId: Long?,
        businessDate: String,
        reason: String,
        notes: String?,
        cashBoxId: Long?
    ): MoneyDeliveryEntry {
        val bId = branchId.toLongOrNull() ?: throw BackendResponseException("El identificador de la banca no es válido")
        val response = provider.api().createMoneyDelivery(
            provider.authorization(),
            UUID.randomUUID().toString(),
            CreateMoneyDeliveryRequest(
                branch_id = bId,
                amount = amount.toPlainString(),
                gross_amount = grossAmount?.toPlainString(),
                commission_rate = commissionRate?.toPlainString(),
                commission_amount = commissionAmount?.toPlainString(),
                suggested_amount = suggestedAmount?.toPlainString(),
                manual_result_id = manualResultId,
                cash_box_id = cashBoxId,
                business_date = businessDate,
                reason = reason,
                notes = notes
            )
        ).dataOrThrow()
        return MoneyDeliveryEntry(
            id = response.id ?: 0L,
            branchId = response.branch_id ?: bId,
            manualResultId = response.manual_result_id,
            suggestedAmount = response.suggested_amount.toAmount(BigDecimal.ZERO),
            grossAmount = response.gross_amount.toAmount(BigDecimal.ZERO),
            commissionRate = response.commission_rate.toAmount(BigDecimal.ZERO),
            commissionAmount = response.commission_amount.toAmount(BigDecimal.ZERO),
            deliveredAmount = response.delivered_amount.toAmount(BigDecimal.ZERO),
            businessDate = response.business_date ?: businessDate,
            reason = response.reason.orEmpty(),
            notes = response.notes,
            branchBalanceAfter = response.branch_balance_after.toAmount(BigDecimal.ZERO)
        )
    }

    override fun getDeliveries(period: String?, branchId: String?): Flow<MoneyDeliveriesSummary> = remoteFlow {
        if (!provider.isAdmin) {
            MoneyDeliveriesSummary(emptyList(), BigDecimal.ZERO)
        } else {
            try {
                val response = provider.api().moneyDeliveries(
                    provider.authorization(),
                    period,
                    branchId?.toLongOrNull()
                ).dataOrThrow()
                val list = response.deliveries.orEmpty()
                    .filter { branchId == null || it.branch_id?.toString() == branchId }
                    .map { d ->
                        MoneyDeliveryEntry(
                            id = d.id ?: 0L,
                            branchId = d.branch_id ?: 0L,
                            manualResultId = d.manual_result_id,
                            suggestedAmount = d.suggested_amount.toAmount(BigDecimal.ZERO),
                            grossAmount = d.gross_amount.toAmount(BigDecimal.ZERO),
                            commissionRate = d.commission_rate.toAmount(BigDecimal.ZERO),
                            commissionAmount = d.commission_amount.toAmount(BigDecimal.ZERO),
                            deliveredAmount = d.delivered_amount.toAmount(BigDecimal.ZERO),
                            businessDate = d.business_date.orEmpty(),
                            reason = d.reason.orEmpty(),
                            notes = d.notes,
                            branchBalanceAfter = d.branch_balance_after.toAmount(BigDecimal.ZERO)
                        )
                    }
                MoneyDeliveriesSummary(list, response.total_delivered.toAmount(BigDecimal.ZERO))
            } catch (e: Exception) {
                MoneyDeliveriesSummary(emptyList(), BigDecimal.ZERO)
            }
        }
    }
}

suspend fun BackendApiProvider.dashboard(): DashboardSnapshot {
    val response = api().dashboard(authorization()).dataOrThrow()
    val deliveredWeek = response.total_money_delivered_this_week.toAmount()
    val advances = response.advances_total.toAmount()
    return DashboardSnapshot(
        receivableTotal = response.receivable_total.toAmount(),
        branchCreditTotal = response.branch_credit_total.toAmount(),
        netPosition = response.net_position.toAmount(),
        collectionsTotal = response.collections_total.toAmount(),
        advancesTotal = advances,
        moneyDeliveredTotal = if (deliveredWeek > BigDecimal.ZERO) deliveredWeek else advances,
        currencyCode = response.currency_code ?: "USD",
        recentActivity = response.recent_activity.orEmpty().map { it.toDomain() },
        cashBalance = response.cash_balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    )
}

private fun CashBoxDto.toDomain(): CashBoxEntity = CashBoxEntity(
    id = id,
    name = name,
    balance = balance?.toAmount() ?: BigDecimal.ZERO,
    currencyCode = currency_code ?: "DOP",
    isDefault = is_default,
    description = description
)

private fun CashBoxResponse.toDomain(): CashBox = CashBox(
    id = cash_box?.id,
    name = cash_box?.name,
    currencyCode = currency_code.required("moneda de caja"),
    currentBalance = current_balance.toAmount(),
    entries = entries?.map { it.toDomain() }
        ?: throw BackendResponseException("El servidor no incluyó los movimientos de caja")
)

private fun CashMovement.toRequest(cashBoxId: Long? = null): CashMovementRequest = CashMovementRequest(
    amount = FinancialCalculator.roundMoney(amount).toPlainString(),
    business_date = businessDate,
    reason = reason,
    branch_id = branchId?.toLongOrNull()
        ?: if (type == CashMovementType.BRANCH_TRANSFER) {
            throw BackendResponseException("El identificador de la banca no es válido")
        } else null,
    cash_box_id = cashBoxId,
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
        balanceBefore = balance_before?.toBigDecimalOrNull(),
        balanceAfter = balance_after?.toBigDecimalOrNull(),
        businessDate = business_date.required("fecha del movimiento de caja"),
        reason = reason.required("motivo del movimiento de caja"),
        branchId = branch_id?.toString(),
        reference = reference,
        notes = notes,
        createdAt = created_at,
        createdBy = creator_name?.takeIf { it.isNotBlank() }
            ?: creator?.name?.takeIf { it.isNotBlank() }
            ?: "SISTEMA",
        branchCode = branch?.code,
        branchName = branch?.name
    )
}

private fun String?.toCashMovementType(): CashMovementType? = when (this?.trim()?.lowercase()) {
    "income", "entrada" -> CashMovementType.INCOME
    "expense", "expenses", "retiro", "gasto" -> CashMovementType.EXPENSE
    "branch_transfer", "branch_delivery", "branch-transfer", "transfer", "transferencia" -> CashMovementType.BRANCH_TRANSFER
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
    description = description ?: address,
    route = route.orEmpty(),
    operatorName = operator_name.orEmpty(),
    phone = phone,
    ownerName = owner_name,
    ownerPhone = owner_phone ?: owner_whatsapp,
    currentBalance = current_balance.toAmount(BigDecimal.ZERO),
    historicalDebt = historical_debt.toAmount(BigDecimal.ZERO),
    commissionRate = commission_rate.toAmount(BigDecimal.ZERO),
    status = status.toEnum(BranchStatus.ACTIVE),
    collectorUserId = collector_user_id ?: collector?.id,
    collectorName = collector?.name ?: collector_name
)

private fun WeeklySettlementResponse.toDomain() = WeeklySettlement(
    id = id.required("id del cuadre semanal"),
    branchId = branch_id.required("banca del cuadre semanal"),
    weekStart = week_start.required("inicio del cuadre semanal"),
    weekEnd = week_end.required("fin del cuadre semanal"),
    salesAmount = sales_amount.toAmount(BigDecimal.ZERO),
    prizesAmount = prizes_amount.toAmount(BigDecimal.ZERO),
    commissionRate = commission_rate.toAmount(BigDecimal.ZERO),
    commissionAmount = commission_amount.toAmount(BigDecimal.ZERO),
    cashDeliveredAmount = cash_delivered_amount.toAmount(BigDecimal.ZERO),
    lossAbsorbedAmount = loss_absorbed_amount.toAmount(BigDecimal.ZERO),
    weeklyBalance = weekly_balance.toAmount(BigDecimal.ZERO),
    balanceBefore = balance_before.toAmount(BigDecimal.ZERO),
    balanceAfter = balance_after.toAmount(BigDecimal.ZERO),
    notes = notes,
    status = status ?: "confirmed",
    settlementType = settlement_type ?: "standard"
)

private fun Branch.toRequest() = BranchRequest(
    code = code.trim(),
    name = name.trim(),
    phone = phone?.trim()?.takeIf { it.isNotEmpty() },
    owner_name = ownerName?.trim()?.takeIf { it.isNotEmpty() },
    owner_phone = ownerPhone?.trim()?.takeIf { it.isNotEmpty() },
    owner_whatsapp = ownerPhone?.trim()?.takeIf { it.isNotEmpty() },
    address = description?.trim()?.takeIf { it.isNotEmpty() },
    description = description?.trim()?.takeIf { it.isNotEmpty() },
    route = route.trim().ifEmpty { "General" },
    operator_name = operatorName.trim().ifEmpty { "General" },
    commission_rate = commissionRate.stripTrailingZeros().toPlainString(),
    status = status.name.lowercase(),
    collector_user_id = collectorUserId
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

private fun String?.toLedgerSourceType(entryType: String?, description: String?): LedgerSourceType {
    val s = (this.orEmpty() + " " + entryType.orEmpty() + " " + description.orEmpty()).lowercase()
    return when {
        s.contains("loss_absorb") || s.contains("asumida por el consorcio") || s.contains("semana a cero") || s.contains("pérdida semanal") -> LedgerSourceType.WEEKLY_LOSS_ABSORPTION
        s.contains("prize_fund") || s.contains("fondo de premios") -> LedgerSourceType.PRIZE_FUND
        s.contains("collection") || s.contains("cobro") -> LedgerSourceType.COLLECTION
        s.contains("moneydelivery") || s.contains("money_delivery") || s.contains("dinero llevado") -> LedgerSourceType.MONEY_DELIVERY
        s.contains("manualresult") || s.contains("manual_result") || s.contains("resultado") -> LedgerSourceType.MANUAL_RESULT
        s.contains("advance") || s.contains("adelanto") -> LedgerSourceType.ADVANCE
        s.contains("settlement") || s.contains("cuadre") -> LedgerSourceType.WEEKLY_SETTLEMENT
        else -> LedgerSourceType.ADJUSTMENT
    }
}

private fun LedgerEntryResponse.toDomain(): LedgerEntry {
    val amt = signed_amount.toAmount()
    val srcType = source_type.toLedgerSourceType(entry_type, description)
    val parsedEntryType = when {
        amt < BigDecimal.ZERO || srcType == LedgerSourceType.COLLECTION -> LedgerEntryType.CREDIT
        else -> LedgerEntryType.DEBIT
    }
    return LedgerEntry(
        id = id.required("id del asiento"),
        branchId = branch_id.required("banca del asiento"),
        sourceType = srcType,
        sourceId = source_id?.toString() ?: id.required("origen del asiento"),
        entryType = parsedEntryType,
        signedAmount = amt,
        balanceBefore = balance_before.toAmount(),
        balanceAfter = balance_after.toAmount(),
        businessDate = business_date.required("fecha del asiento"),
        description = description.orEmpty(),
        createdBy = creator_name?.takeIf { it.isNotBlank() }
            ?: creator?.name?.takeIf { it.isNotBlank() }
            ?: "SISTEMA",
        reversalOfEntryId = reversal_of_entry_id?.toString(),
        branchCode = branch?.code,
        branchName = branch?.name
    )
}

private fun Long?.required(field: String): String = this?.toString()
    ?: throw BackendResponseException("El servidor no incluyó $field")

private fun String?.required(field: String): String = this?.takeIf { it.isNotBlank() }
    ?: throw BackendResponseException("El servidor no incluyó $field")

private fun String?.toAmount(default: BigDecimal? = null): BigDecimal {
    val parsed = this?.toBigDecimalOrNull()
    if (parsed != null) return parsed
    if (default != null) return default
    throw BackendResponseException("El servidor devolvió un monto inválido")
}

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

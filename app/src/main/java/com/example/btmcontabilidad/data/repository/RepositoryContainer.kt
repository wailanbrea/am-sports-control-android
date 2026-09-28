package com.example.btmcontabilidad.data.repository

import android.content.Context
import com.example.btmcontabilidad.data.session.SessionStore
import com.example.btmcontabilidad.domain.model.Advance
import com.example.btmcontabilidad.domain.model.Collection
import com.example.btmcontabilidad.domain.model.LedgerEntry
import com.example.btmcontabilidad.domain.model.WeeklySettlement
import com.example.btmcontabilidad.domain.repository.AdvanceRepository
import com.example.btmcontabilidad.domain.repository.BranchRepository
import com.example.btmcontabilidad.domain.repository.CashBoxRepository
import com.example.btmcontabilidad.domain.repository.CollectionRepository
import com.example.btmcontabilidad.domain.repository.LedgerRepository
import com.example.btmcontabilidad.domain.repository.WeeklySettlementRepository

class RepositoryContainer(
    context: Context
) {
    private val provider = BackendApiProvider(context)
    val branchRepository: BranchRepository = BackendBranchRepository(provider)
    val ledgerRepository: LedgerRepository = BackendLedgerRepository(provider)
    val collectionRepository: CollectionRepository = BackendCollectionRepository(provider)
    val advanceRepository: AdvanceRepository = BackendAdvanceRepository(provider)
    val cashBoxRepository: CashBoxRepository = BackendCashBoxRepository(provider)
    val weeklySettlementRepository: WeeklySettlementRepository = BackendWeeklySettlementRepository(provider)
    val manualResultRepository: ManualResultRepository = BackendManualResultRepository(provider)
    val moneyDeliveryRepository: MoneyDeliveryRepository = BackendMoneyDeliveryRepository(provider)
    val collectorRepository: CollectorRepository = BackendCollectorRepository(provider)
    val session: SessionStore get() = provider.session
    val isAdmin: Boolean get() = provider.isAdmin
    val isCollector: Boolean get() = provider.isCollector
    fun currentUserRole(): String = provider.currentUserRole()

    fun clearCaches() {
        (branchRepository as? BackendBranchRepository)?.invalidateCache()
        (collectorRepository as? BackendCollectorRepository)?.invalidateCache()
        (ledgerRepository as? BackendLedgerRepository)?.invalidateCache()
    }

    suspend fun dashboard(): DashboardSnapshot = provider.dashboard()
    suspend fun registerCollection(collection: Collection, createdBy: String = "SISTEMA"): Collection =
        collectionRepository.addCollection(collection)

    suspend fun registerAdvance(advance: Advance, createdBy: String = "SISTEMA"): Advance =
        advanceRepository.addAdvance(advance)

    suspend fun registerWeeklySettlement(settlement: WeeklySettlement): WeeklySettlement =
        weeklySettlementRepository.add(settlement)

    /**
     * Reverses a ledger entry, creates a reversal ledger transaction, and updates the branch balance accordingly.
     */
    suspend fun reverseLedgerEntry(entryId: String, reason: String, reversedBy: String = "SISTEMA"): LedgerEntry {
        require(reason.isNotBlank()) { "Debe especificar un motivo de reversión" }
        return ledgerRepository.reverseEntry(entryId, reason, reversedBy)
    }

    companion object {
        @Volatile private var instance: RepositoryContainer? = null

        fun initialize(context: Context) {
            if (instance == null) synchronized(this) {
                if (instance == null) instance = RepositoryContainer(context.applicationContext)
            }
        }

        fun get(context: Context): RepositoryContainer {
            return instance ?: synchronized(this) {
                instance ?: RepositoryContainer(context.applicationContext).also { instance = it }
            }
        }

        val Instance: RepositoryContainer
            get() = instance ?: error("RepositoryContainer no está inicializado")
    }
}

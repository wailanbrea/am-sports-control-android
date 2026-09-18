package com.example.btmcontabilidad.domain.repository

import com.example.btmcontabilidad.domain.model.LedgerEntry
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface LedgerRepository {
    fun getLedgerEntries(): Flow<List<LedgerEntry>>
    fun getBranchLedger(branchId: String): Flow<List<LedgerEntry>>
    suspend fun addEntry(entry: LedgerEntry): LedgerEntry
    suspend fun reverseEntry(
        entryId: String,
        reason: String,
        reversedBy: String,
        balanceBefore: BigDecimal? = null
    ): LedgerEntry
}

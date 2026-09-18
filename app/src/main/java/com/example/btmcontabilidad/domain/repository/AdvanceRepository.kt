package com.example.btmcontabilidad.domain.repository

import com.example.btmcontabilidad.domain.model.Advance
import kotlinx.coroutines.flow.Flow

interface AdvanceRepository {
    fun getAdvances(): Flow<List<Advance>>
    fun getAdvancesForBranch(branchId: String): Flow<List<Advance>>
    fun getAdvanceById(id: String): Flow<Advance?>
    suspend fun addAdvance(advance: Advance): Advance
    suspend fun cancelAdvance(id: String, reason: String): Advance
}

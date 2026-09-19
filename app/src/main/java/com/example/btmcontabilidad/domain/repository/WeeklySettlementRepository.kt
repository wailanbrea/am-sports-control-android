package com.example.btmcontabilidad.domain.repository

import com.example.btmcontabilidad.domain.model.WeeklySettlement
import kotlinx.coroutines.flow.Flow

interface WeeklySettlementRepository {
    fun getForBranch(branchId: String): Flow<List<WeeklySettlement>>
    suspend fun add(settlement: WeeklySettlement): WeeklySettlement
}

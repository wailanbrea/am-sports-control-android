package com.example.btmcontabilidad.domain.repository

import com.example.btmcontabilidad.domain.model.CashBox
import com.example.btmcontabilidad.domain.model.CashMovement
import kotlinx.coroutines.flow.Flow

interface CashBoxRepository {
    fun getCashBox(): Flow<CashBox>
    suspend fun addIncome(movement: CashMovement): CashMovement
    suspend fun addExpense(movement: CashMovement): CashMovement
    suspend fun transferToBranch(movement: CashMovement): CashMovement
}

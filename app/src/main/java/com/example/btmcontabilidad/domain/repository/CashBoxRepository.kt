package com.example.btmcontabilidad.domain.repository

import com.example.btmcontabilidad.domain.model.CashBox
import com.example.btmcontabilidad.domain.model.CashMovement
import kotlinx.coroutines.flow.Flow

import com.example.btmcontabilidad.domain.model.CashBoxEntity
import java.math.BigDecimal

interface CashBoxRepository {
    fun getCashBox(cashBoxId: Long? = null): Flow<CashBox>
    suspend fun getCashBoxes(): List<CashBoxEntity>
    suspend fun createCashBox(name: String, initialBalance: BigDecimal, description: String?, isDefault: Boolean): CashBoxEntity
    suspend fun addIncome(movement: CashMovement, cashBoxId: Long? = null): CashMovement
    suspend fun addExpense(movement: CashMovement, cashBoxId: Long? = null): CashMovement
    suspend fun transferToBranch(movement: CashMovement, cashBoxId: Long? = null): CashMovement
}

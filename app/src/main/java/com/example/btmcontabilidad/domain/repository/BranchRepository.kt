package com.example.btmcontabilidad.domain.repository

import com.example.btmcontabilidad.domain.model.Branch
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface BranchRepository {
    fun getBranches(): Flow<List<Branch>>
    fun getBranchById(id: String): Flow<Branch?>
    fun getBranchByCode(code: String): Flow<Branch?>
    suspend fun updateBalance(branchId: String, newBalance: BigDecimal): Branch
    suspend fun addBranch(branch: Branch): Branch
    suspend fun updateBranch(branch: Branch): Branch
    suspend fun deleteBranch(id: String): Boolean
}

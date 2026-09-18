package com.example.btmcontabilidad.domain.repository

import com.example.btmcontabilidad.domain.model.Collection
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    fun getCollections(): Flow<List<Collection>>
    fun getCollectionsForBranch(branchId: String): Flow<List<Collection>>
    fun getCollectionById(id: String): Flow<Collection?>
    suspend fun addCollection(collection: Collection): Collection
    suspend fun cancelCollection(id: String, reason: String): Collection
}

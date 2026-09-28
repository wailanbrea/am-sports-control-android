package com.example.btmcontabilidad.data.repository

import com.example.btmcontabilidad.data.network.CollectorDto
import com.example.btmcontabilidad.data.network.CreateCollectorRequest
import com.example.btmcontabilidad.data.network.UpdateCollectorRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface CollectorRepository {
    fun getCollectors(): Flow<List<CollectorDto>>
    suspend fun createCollector(request: CreateCollectorRequest): CollectorDto
    suspend fun updateCollector(id: Long, request: UpdateCollectorRequest): CollectorDto
    suspend fun assignBranches(collectorId: Long, branchIds: List<Long>)
}

class BackendCollectorRepository(
    private val provider: BackendApiProvider
) : CollectorRepository {

    @Volatile private var cachedCollectors: List<CollectorDto>? = null

    fun invalidateCache() {
        cachedCollectors = null
    }

    override fun getCollectors(): Flow<List<CollectorDto>> = flow {
        if (!provider.isAdmin) {
            emit(emptyList())
            return@flow
        }
        cachedCollectors?.let { emit(it) }
        try {
            val fresh = provider.api().collectors(provider.authorization()).dataOrThrow()
            cachedCollectors = fresh
            emit(fresh)
        } catch (e: Exception) {
            if (cachedCollectors == null) emit(emptyList())
        }
    }

    override suspend fun createCollector(request: CreateCollectorRequest): CollectorDto {
        val created = provider.api().createCollector(provider.authorization(), request).dataOrThrow()
        cachedCollectors = (cachedCollectors ?: emptyList()) + created
        return created
    }

    override suspend fun updateCollector(id: Long, request: UpdateCollectorRequest): CollectorDto {
        val updated = provider.api().updateCollector(provider.authorization(), id, request).dataOrThrow()
        cachedCollectors = cachedCollectors?.map { if (it.id == updated.id) updated else it }
        return updated
    }

    override suspend fun assignBranches(collectorId: Long, branchIds: List<Long>) {
        provider.api().assignBranches(
            provider.authorization(),
            collectorId,
            com.example.btmcontabilidad.data.network.AssignBranchesRequest(branchIds)
        ).successOrThrow()
        // Invalidate collector cache to reflect changed counts
        invalidateCache()
    }
}

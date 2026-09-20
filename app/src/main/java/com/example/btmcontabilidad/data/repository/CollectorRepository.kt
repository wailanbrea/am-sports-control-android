package com.example.btmcontabilidad.data.repository

import com.example.btmcontabilidad.data.network.CollectorDto
import com.example.btmcontabilidad.data.network.CreateCollectorRequest
import com.example.btmcontabilidad.data.network.UpdateCollectorRequest
import kotlinx.coroutines.flow.Flow

interface CollectorRepository {
    fun getCollectors(): Flow<List<CollectorDto>>
    suspend fun createCollector(request: CreateCollectorRequest): CollectorDto
    suspend fun updateCollector(id: Long, request: UpdateCollectorRequest): CollectorDto
}

class BackendCollectorRepository(
    private val provider: BackendApiProvider
) : CollectorRepository {

    override fun getCollectors(): Flow<List<CollectorDto>> = remoteFlow {
        provider.api().collectors(provider.authorization()).dataOrThrow()
    }

    override suspend fun createCollector(request: CreateCollectorRequest): CollectorDto {
        return provider.api().createCollector(provider.authorization(), request).dataOrThrow()
    }

    override suspend fun updateCollector(id: Long, request: UpdateCollectorRequest): CollectorDto {
        return provider.api().updateCollector(provider.authorization(), id, request).dataOrThrow()
    }
}

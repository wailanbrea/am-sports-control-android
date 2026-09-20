package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.network.CollectorDto
import com.example.btmcontabilidad.data.network.CreateCollectorRequest
import com.example.btmcontabilidad.data.repository.CollectorRepository
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CollectorsUiState(
    val isLoading: Boolean = true,
    val collectors: List<CollectorDto> = emptyList(),
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

class CollectorsViewModel(
    private val repository: CollectorRepository = RepositoryContainer.Instance.collectorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectorsUiState())
    val uiState: StateFlow<CollectorsUiState> = _uiState.asStateFlow()

    init {
        loadCollectors()
    }

    fun loadCollectors() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repository.getCollectors().collect { list ->
                    _uiState.update { it.copy(isLoading = false, collectors = list) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Error al cargar cobradores") }
            }
        }
    }

    fun createCollector(name: String, email: String, password: String, role: String = "collector") {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Todos los campos son obligatorios") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }
            try {
                repository.createCollector(
                    CreateCollectorRequest(
                        name = name.trim(),
                        email = email.trim(),
                        password = password,
                        role = role,
                        status = "active"
                    )
                )
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                loadCollectors()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Error al crear cobrador") }
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}

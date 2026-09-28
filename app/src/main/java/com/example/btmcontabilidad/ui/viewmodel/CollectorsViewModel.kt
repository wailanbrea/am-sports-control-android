package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.network.CollectorDto
import com.example.btmcontabilidad.data.network.CreateCollectorRequest
import com.example.btmcontabilidad.data.network.UpdateCollectorRequest
import com.example.btmcontabilidad.data.repository.CollectorRepository
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.domain.model.Branch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CollectorsUiState(
    val isLoading: Boolean = true,
    val collectors: List<CollectorDto> = emptyList(),
    val allBranches: List<Branch> = emptyList(),
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val isAssigning: Boolean = false,
    val assignmentSuccess: Boolean = false
)

class CollectorsViewModel(
    private val repository: CollectorRepository = RepositoryContainer.Instance.collectorRepository,
    private val repositoryContainer: RepositoryContainer = RepositoryContainer.Instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectorsUiState())
    val uiState: StateFlow<CollectorsUiState> = _uiState.asStateFlow()

    init {
        loadCollectors()
        loadBranches()
    }

    fun loadCollectors() {
        viewModelScope.launch {
            if (_uiState.value.collectors.isEmpty()) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(errorMessage = null) }
            }
            try {
                repository.getCollectors().collect { list ->
                    _uiState.update { it.copy(isLoading = false, collectors = list) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Error al cargar cobradores") }
            }
        }
    }

    fun loadBranches() {
        viewModelScope.launch {
            try {
                repositoryContainer.branchRepository.getBranches().collect { list ->
                    _uiState.update { it.copy(allBranches = list) }
                }
            } catch (_: Exception) {}
        }
    }

    fun assignBranches(collectorId: Long, branchIds: List<Long>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAssigning = true, errorMessage = null, assignmentSuccess = false) }
            try {
                repository.assignBranches(collectorId, branchIds)
                _uiState.update { it.copy(isAssigning = false, assignmentSuccess = true) }
                loadCollectors()
                loadBranches()
            } catch (e: Exception) {
                _uiState.update { it.copy(isAssigning = false, errorMessage = e.message ?: "Error al asignar bancas") }
            }
        }
    }

    fun clearAssignmentSuccess() {
        _uiState.update { it.copy(assignmentSuccess = false) }
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

    fun updateCollector(
        id: Long,
        name: String,
        email: String,
        password: String?,
        role: String = "collector",
        status: String = "active"
    ) {
        if (name.isBlank() || email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El nombre y el correo son obligatorios") }
            return
        }
        if (!password.isNullOrBlank() && password.length < 6) {
            _uiState.update { it.copy(errorMessage = "La nueva contraseña debe tener al menos 6 caracteres") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }
            try {
                repository.updateCollector(
                    id = id,
                    request = UpdateCollectorRequest(
                        name = name.trim(),
                        email = email.trim(),
                        password = password?.trim()?.takeIf { it.isNotBlank() },
                        role = role,
                        status = status
                    )
                )
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                loadCollectors()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Error al actualizar cobrador") }
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

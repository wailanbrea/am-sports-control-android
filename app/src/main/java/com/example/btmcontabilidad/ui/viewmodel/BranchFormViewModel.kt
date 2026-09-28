package com.example.btmcontabilidad.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.btmcontabilidad.data.network.CollectorDto
import com.example.btmcontabilidad.data.repository.RepositoryContainer
import com.example.btmcontabilidad.domain.model.Branch
import com.example.btmcontabilidad.domain.model.BranchStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BranchFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedBranchId: String? = null,
    val code: String = "",
    val name: String = "",
    val phone: String = "",
    val ownerName: String = "",
    val ownerPhone: String = "",
    val description: String = "",
    val route: String = "",
    val operatorName: String = "",
    val commissionRate: String = "",
    val status: BranchStatus = BranchStatus.ACTIVE,
    val collectors: List<CollectorDto> = emptyList(),
    val selectedCollectorId: Long? = null,
    val selectedCollectorName: String? = null
)

class BranchFormViewModel(
    private val repositoryContainer: RepositoryContainer = RepositoryContainer.Instance
) : ViewModel() {
    private val _uiState = MutableStateFlow(BranchFormUiState())
    val uiState: StateFlow<BranchFormUiState> = _uiState.asStateFlow()

    init {
        loadCollectors()
    }

    private fun loadCollectors() {
        viewModelScope.launch {
            try {
                repositoryContainer.collectorRepository.getCollectors().collect { collectorList ->
                    _uiState.update { current ->
                        current.copy(
                            collectors = collectorList,
                            selectedCollectorName = current.selectedCollectorName
                                ?: collectorList.firstOrNull { it.id == current.selectedCollectorId }?.name
                        )
                    }
                }
            } catch (_: Exception) {
                // Ignore silent collector loading failure
            }
        }
    }

    fun loadBranch(branchId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val branch = repositoryContainer.branchRepository.getBranchById(branchId).first()
                    ?: throw IllegalStateException("Banca no encontrada")
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        code = branch.code,
                        name = branch.name,
                        phone = branch.phone.orEmpty(),
                        ownerName = branch.ownerName.orEmpty(),
                        ownerPhone = branch.ownerPhone.orEmpty(),
                        description = branch.description.orEmpty(),
                        route = branch.route,
                        operatorName = branch.operatorName,
                        commissionRate = if (branch.commissionRate > java.math.BigDecimal.ZERO) {
                            branch.commissionRate.stripTrailingZeros().toPlainString()
                        } else "",
                        status = branch.status,
                        selectedCollectorId = branch.collectorUserId,
                        selectedCollectorName = branch.collectorName
                            ?: current.collectors.firstOrNull { it.id == branch.collectorUserId }?.name
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "No se pudo cargar la banca")
                }
            }
        }
    }

    fun updateCode(value: String) = update { copy(code = value) }
    fun updateName(value: String) = update { copy(name = value) }
    fun updatePhone(value: String) = update { copy(phone = value) }
    fun updateOwnerName(value: String) = update { copy(ownerName = value) }
    fun updateOwnerPhone(value: String) = update { copy(ownerPhone = value) }
    fun updateDescription(value: String) = update { copy(description = value) }
    fun updateRoute(value: String) = update { copy(route = value) }
    fun updateOperatorName(value: String) = update { copy(operatorName = value) }
    fun updateCommissionRate(value: String) = update { copy(commissionRate = value) }
    fun updateStatus(value: BranchStatus) = update { copy(status = value) }

    fun updateCollector(collectorId: Long?, collectorName: String?) {
        _uiState.update {
            it.copy(
                selectedCollectorId = collectorId,
                selectedCollectorName = collectorName,
                errorMessage = null
            )
        }
    }

    fun save(branchId: String?) {
        val state = _uiState.value
        val validationError = when {
            state.code.isBlank() -> "El código es obligatorio"
            state.name.isBlank() -> "El nombre es obligatorio"
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val rate = state.commissionRate.trim().replace(",", ".").toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
                val branch = Branch(
                    id = branchId.orEmpty(),
                    code = state.code,
                    name = state.name,
                    phone = state.phone.ifBlank { null },
                    ownerName = state.ownerName.ifBlank { null },
                    ownerPhone = state.ownerPhone.ifBlank { null },
                    description = state.description,
                    route = state.route.ifBlank { "General" },
                    operatorName = state.operatorName.ifBlank { state.name },
                    commissionRate = rate,
                    status = state.status,
                    collectorUserId = state.selectedCollectorId,
                    collectorName = state.selectedCollectorName
                )
                val saved = if (branchId == null) {
                    repositoryContainer.branchRepository.addBranch(branch)
                } else {
                    repositoryContainer.branchRepository.updateBranch(branch)
                }
                repositoryContainer.branchRepository.invalidateCache()
                _uiState.update { it.copy(isSaving = false, savedBranchId = saved.id) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "No se pudo guardar la banca")
                }
            }
        }
    }

    private fun update(transform: BranchFormUiState.() -> BranchFormUiState) {
        _uiState.update { it.transform().copy(errorMessage = null) }
    }
}

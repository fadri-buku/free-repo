package com.bukuwarung.paymentsadapter.presentation.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bukuwarung.paymentsadapter.domain.model.FeatureFlags
import com.bukuwarung.paymentsadapter.domain.model.ProviderHealth
import com.bukuwarung.paymentsadapter.domain.usecase.admin.GetFlagsUseCase
import com.bukuwarung.paymentsadapter.domain.usecase.admin.GetProviderHealthUseCase
import com.bukuwarung.paymentsadapter.domain.usecase.admin.ReloadRoutingUseCase
import com.bukuwarung.paymentsadapter.domain.usecase.admin.ToggleFlagUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val getProviderHealthUseCase: GetProviderHealthUseCase,
    private val getFlagsUseCase: GetFlagsUseCase,
    private val toggleFlagUseCase: ToggleFlagUseCase,
    private val reloadRoutingUseCase: ReloadRoutingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Idle)
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private var lastHealth: ProviderHealth = ProviderHealth(emptyMap())
    private var lastFlags: FeatureFlags = FeatureFlags(emptyMap())
    private var lastReloadedAt: String? = null

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            val healthDeferred = async { getProviderHealthUseCase() }
            val flagsDeferred = async { getFlagsUseCase() }
            val health = healthDeferred.await()
            val flags = flagsDeferred.await()
            if (health.isFailure || flags.isFailure) {
                val err = (health.exceptionOrNull() ?: flags.exceptionOrNull())!!
                _uiState.value = AdminUiState.Error(err.toUserMessage())
            } else {
                lastHealth = health.getOrThrow()
                lastFlags = flags.getOrThrow()
                _uiState.value = AdminUiState.Success(lastHealth, lastFlags, lastReloadedAt)
            }
        }
    }

    fun toggleFlag(key: String, enabled: Boolean) {
        viewModelScope.launch {
            toggleFlagUseCase(key, enabled)
                .onSuccess { result ->
                    val updated = lastFlags.copy(
                        flags = lastFlags.flags.toMutableMap().also { it[result.key] = result.enabled }
                    )
                    lastFlags = updated
                    _uiState.value = AdminUiState.Success(lastHealth, updated, lastReloadedAt)
                }
                .onFailure { _uiState.value = AdminUiState.Error(it.toUserMessage()) }
        }
    }

    fun reloadRouting() {
        viewModelScope.launch {
            reloadRoutingUseCase()
                .onSuccess { reload ->
                    lastReloadedAt = reload.reloadedAt
                    _uiState.value = AdminUiState.Success(lastHealth, lastFlags, reload.reloadedAt)
                }
                .onFailure { _uiState.value = AdminUiState.Error(it.toUserMessage()) }
        }
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is HttpException -> "Error ${code()} — ${message()}"
        is IOException -> "Network error"
        else -> message ?: "Unexpected error"
    }
}

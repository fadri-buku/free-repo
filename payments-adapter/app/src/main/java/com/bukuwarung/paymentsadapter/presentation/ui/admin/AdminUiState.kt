package com.bukuwarung.paymentsadapter.presentation.ui.admin

import com.bukuwarung.paymentsadapter.domain.model.FeatureFlags
import com.bukuwarung.paymentsadapter.domain.model.ProviderHealth

sealed interface AdminUiState {
    object Idle : AdminUiState
    object Loading : AdminUiState
    data class Success(
        val health: ProviderHealth,
        val flags: FeatureFlags,
        val routingReloadedAt: String?
    ) : AdminUiState
    data class Error(val message: String) : AdminUiState
}

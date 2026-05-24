package com.bukuwarung.paymentsadapter.presentation.ui.auth

import com.bukuwarung.paymentsadapter.domain.model.AuthToken

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val token: AuthToken) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

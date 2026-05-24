package com.bukuwarung.paymentsadapter.presentation.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bukuwarung.paymentsadapter.domain.usecase.auth.GetTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val getTokenUseCase: GetTokenUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun getToken(clientKey: String, privateKeyPem: String) {
        if (clientKey.isBlank() || privateKeyPem.isBlank()) {
            _uiState.value = AuthUiState.Error("Client key and private key are required")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            getTokenUseCase(clientKey, privateKeyPem)
                .onSuccess { _uiState.value = AuthUiState.Success(it) }
                .onFailure { _uiState.value = AuthUiState.Error(it.toUserMessage()) }
        }
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is HttpException -> "Server error ${code()} — ${message()}"
        is IOException -> "Network error — check your connection"
        else -> message ?: "Unexpected error"
    }
}

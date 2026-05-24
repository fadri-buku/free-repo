package com.bukuwarung.paymentsadapter.presentation.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bukuwarung.paymentsadapter.domain.model.PaymentCancel
import com.bukuwarung.paymentsadapter.domain.usecase.payment.CancelPaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

sealed interface CancelPaymentUiState {
    object Idle : CancelPaymentUiState
    object Loading : CancelPaymentUiState
    data class Success(val result: PaymentCancel) : CancelPaymentUiState
    data class Error(val message: String) : CancelPaymentUiState
}

@HiltViewModel
class CancelPaymentViewModel @Inject constructor(
    private val cancelPaymentUseCase: CancelPaymentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CancelPaymentUiState>(CancelPaymentUiState.Idle)
    val uiState: StateFlow<CancelPaymentUiState> = _uiState.asStateFlow()

    fun cancel(referenceNo: String, reason: String) {
        viewModelScope.launch {
            _uiState.value = CancelPaymentUiState.Loading
            cancelPaymentUseCase(referenceNo, reason)
                .onSuccess { _uiState.value = CancelPaymentUiState.Success(it) }
                .onFailure { _uiState.value = CancelPaymentUiState.Error(it.toUserMessage()) }
        }
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is HttpException -> "Error ${code()} — ${message()}"
        is IOException -> "Network error"
        else -> message ?: "Unexpected error"
    }
}

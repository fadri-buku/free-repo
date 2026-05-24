package com.bukuwarung.paymentsadapter.presentation.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bukuwarung.paymentsadapter.domain.model.Payment
import com.bukuwarung.paymentsadapter.domain.usecase.payment.GetPaymentStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

sealed interface PaymentStatusUiState {
    object Idle : PaymentStatusUiState
    object Loading : PaymentStatusUiState
    data class Success(val payment: Payment) : PaymentStatusUiState
    data class Error(val message: String) : PaymentStatusUiState
}

@HiltViewModel
class PaymentStatusViewModel @Inject constructor(
    private val getPaymentStatusUseCase: GetPaymentStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PaymentStatusUiState>(PaymentStatusUiState.Idle)
    val uiState: StateFlow<PaymentStatusUiState> = _uiState.asStateFlow()

    fun fetchStatus(referenceNo: String) {
        viewModelScope.launch {
            _uiState.value = PaymentStatusUiState.Loading
            getPaymentStatusUseCase(referenceNo)
                .onSuccess { _uiState.value = PaymentStatusUiState.Success(it) }
                .onFailure { _uiState.value = PaymentStatusUiState.Error(it.toUserMessage()) }
        }
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is HttpException -> "Error ${code()} — ${message()}"
        is IOException -> "Network error"
        else -> message ?: "Unexpected error"
    }
}

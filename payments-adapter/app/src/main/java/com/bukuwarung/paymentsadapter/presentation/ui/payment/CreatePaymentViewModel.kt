package com.bukuwarung.paymentsadapter.presentation.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bukuwarung.paymentsadapter.domain.usecase.payment.CreatePaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreatePaymentViewModel @Inject constructor(
    private val createPaymentUseCase: CreatePaymentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreatePaymentUiState>(CreatePaymentUiState.Idle)
    val uiState: StateFlow<CreatePaymentUiState> = _uiState.asStateFlow()

    fun create(
        partnerReferenceNo: String,
        amount: String,
        channelId: String,
        payerId: String,
        payerAccountNo: String,
        expiredAt: String,
        orderId: String
    ) {
        viewModelScope.launch {
            _uiState.value = CreatePaymentUiState.Loading
            createPaymentUseCase(
                externalId = UUID.randomUUID().toString().take(36),
                partnerReferenceNo = partnerReferenceNo,
                amount = amount,
                currency = "IDR",
                channelId = channelId,
                payerId = payerId,
                payerAccountNo = payerAccountNo,
                expiredAt = expiredAt,
                metadata = if (orderId.isNotBlank()) mapOf("order_id" to orderId) else emptyMap()
            )
                .onSuccess { _uiState.value = CreatePaymentUiState.Success(it) }
                .onFailure { _uiState.value = CreatePaymentUiState.Error(it.toUserMessage()) }
        }
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is HttpException -> "Error ${code()} — ${message()}"
        is IOException -> "Network error"
        else -> message ?: "Unexpected error"
    }
}

package com.bukuwarung.paymentsadapter.presentation.ui.payment

import com.bukuwarung.paymentsadapter.domain.model.Payment

sealed interface CreatePaymentUiState {
    object Idle : CreatePaymentUiState
    object Loading : CreatePaymentUiState
    data class Success(val payment: Payment) : CreatePaymentUiState
    data class Error(val message: String) : CreatePaymentUiState
}

package com.bukuwarung.paymentsadapter.presentation.ui.disbursement

import com.bukuwarung.paymentsadapter.domain.model.AccountInquiry

sealed interface AccountInquiryUiState {
    object Idle : AccountInquiryUiState
    object Loading : AccountInquiryUiState
    data class Success(val inquiry: AccountInquiry) : AccountInquiryUiState
    data class Error(val message: String) : AccountInquiryUiState
}

package com.bukuwarung.paymentsadapter.presentation.ui.disbursement

import com.bukuwarung.paymentsadapter.domain.model.Disbursement

sealed interface CreateDisbursementUiState {
    object Idle : CreateDisbursementUiState
    object Loading : CreateDisbursementUiState
    data class Success(val disbursement: Disbursement) : CreateDisbursementUiState
    data class Error(val message: String) : CreateDisbursementUiState
}

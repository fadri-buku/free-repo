package com.bukuwarung.paymentsadapter.presentation.ui.disbursement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bukuwarung.paymentsadapter.domain.usecase.disbursement.AccountInquiryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AccountInquiryViewModel @Inject constructor(
    private val accountInquiryUseCase: AccountInquiryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountInquiryUiState>(AccountInquiryUiState.Idle)
    val uiState: StateFlow<AccountInquiryUiState> = _uiState.asStateFlow()

    fun inquire(beneficiaryAccountNo: String, beneficiaryBankCode: String, sourceAccountNo: String) {
        if (beneficiaryAccountNo.isBlank() || beneficiaryBankCode.isBlank() || sourceAccountNo.isBlank()) {
            _uiState.value = AccountInquiryUiState.Error("All fields are required")
            return
        }
        viewModelScope.launch {
            _uiState.value = AccountInquiryUiState.Loading
            accountInquiryUseCase(beneficiaryAccountNo, beneficiaryBankCode, sourceAccountNo)
                .onSuccess { _uiState.value = AccountInquiryUiState.Success(it) }
                .onFailure { _uiState.value = AccountInquiryUiState.Error(it.toUserMessage()) }
        }
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is HttpException -> "Error ${code()} — ${message()}"
        is IOException -> "Network error"
        else -> message ?: "Unexpected error"
    }
}

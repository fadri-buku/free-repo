package com.bukuwarung.paymentsadapter.presentation.ui.disbursement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bukuwarung.paymentsadapter.domain.usecase.disbursement.CreateDisbursementUseCase
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
class CreateDisbursementViewModel @Inject constructor(
    private val createDisbursementUseCase: CreateDisbursementUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateDisbursementUiState>(CreateDisbursementUiState.Idle)
    val uiState: StateFlow<CreateDisbursementUiState> = _uiState.asStateFlow()

    fun create(
        partnerReferenceNo: String,
        amount: String,
        beneficiaryAccountNo: String,
        beneficiaryBankCode: String,
        beneficiaryName: String,
        sourceAccountNo: String,
        channelId: String,
        remark: String,
        inquiryKey: String?
    ) {
        viewModelScope.launch {
            _uiState.value = CreateDisbursementUiState.Loading
            createDisbursementUseCase(
                externalId = UUID.randomUUID().toString().take(36),
                partnerReferenceNo = partnerReferenceNo,
                amount = amount,
                currency = "IDR",
                beneficiaryAccountNo = beneficiaryAccountNo,
                beneficiaryBankCode = beneficiaryBankCode,
                beneficiaryName = beneficiaryName,
                sourceAccountNo = sourceAccountNo,
                channelId = channelId,
                remark = remark,
                inquiryKey = inquiryKey
            )
                .onSuccess { _uiState.value = CreateDisbursementUiState.Success(it) }
                .onFailure { _uiState.value = CreateDisbursementUiState.Error(it.toUserMessage()) }
        }
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is HttpException -> "Error ${code()} — ${message()}"
        is IOException -> "Network error"
        else -> message ?: "Unexpected error"
    }
}

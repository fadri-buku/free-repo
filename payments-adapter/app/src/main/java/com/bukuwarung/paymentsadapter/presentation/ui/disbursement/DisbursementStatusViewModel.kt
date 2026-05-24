package com.bukuwarung.paymentsadapter.presentation.ui.disbursement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bukuwarung.paymentsadapter.domain.model.Disbursement
import com.bukuwarung.paymentsadapter.domain.usecase.disbursement.GetDisbursementStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

sealed interface DisbursementStatusUiState {
    object Idle : DisbursementStatusUiState
    object Loading : DisbursementStatusUiState
    data class Success(val disbursement: Disbursement) : DisbursementStatusUiState
    data class Error(val message: String) : DisbursementStatusUiState
}

@HiltViewModel
class DisbursementStatusViewModel @Inject constructor(
    private val getDisbursementStatusUseCase: GetDisbursementStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DisbursementStatusUiState>(DisbursementStatusUiState.Idle)
    val uiState: StateFlow<DisbursementStatusUiState> = _uiState.asStateFlow()

    // polling schedule: t+5s, t+10s, t+20s, t+30s, t+60s, t+2min, then escalate
    private val pollIntervals = longArrayOf(5_000, 10_000, 20_000, 30_000, 60_000, 120_000)

    fun fetchStatus(referenceNo: String) {
        viewModelScope.launch {
            _uiState.value = DisbursementStatusUiState.Loading
            getDisbursementStatusUseCase(referenceNo)
                .onSuccess { _uiState.value = DisbursementStatusUiState.Success(it) }
                .onFailure { _uiState.value = DisbursementStatusUiState.Error(it.toUserMessage()) }
        }
    }

    fun pollUntilTerminal(referenceNo: String) {
        viewModelScope.launch {
            for (intervalMs in pollIntervals) {
                delay(intervalMs)
                val result = getDisbursementStatusUseCase(referenceNo)
                result.onSuccess { disbursement ->
                    _uiState.value = DisbursementStatusUiState.Success(disbursement)
                    if (disbursement.status.isTerminal) return@launch
                }.onFailure {
                    _uiState.value = DisbursementStatusUiState.Error(it.toUserMessage())
                    return@launch
                }
            }
        }
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is HttpException -> "Error ${code()} — ${message()}"
        is IOException -> "Network error"
        else -> message ?: "Unexpected error"
    }
}

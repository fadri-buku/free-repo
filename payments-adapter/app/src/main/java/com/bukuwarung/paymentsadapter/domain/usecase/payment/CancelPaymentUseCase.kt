package com.bukuwarung.paymentsadapter.domain.usecase.payment

import com.bukuwarung.paymentsadapter.domain.model.PaymentCancel
import com.bukuwarung.paymentsadapter.domain.repository.PaymentRepository
import javax.inject.Inject

class CancelPaymentUseCase @Inject constructor(
    private val repository: PaymentRepository
) {
    suspend operator fun invoke(referenceNo: String, reason: String): Result<PaymentCancel> =
        repository.cancelPayment(referenceNo, reason)
}

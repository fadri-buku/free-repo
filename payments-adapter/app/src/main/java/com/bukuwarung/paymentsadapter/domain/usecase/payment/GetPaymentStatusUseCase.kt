package com.bukuwarung.paymentsadapter.domain.usecase.payment

import com.bukuwarung.paymentsadapter.domain.model.Payment
import com.bukuwarung.paymentsadapter.domain.repository.PaymentRepository
import javax.inject.Inject

class GetPaymentStatusUseCase @Inject constructor(
    private val repository: PaymentRepository
) {
    suspend operator fun invoke(referenceNo: String): Result<Payment> =
        repository.getPaymentStatus(referenceNo)
}

package com.bukuwarung.paymentsadapter.domain.usecase.payment

import com.bukuwarung.paymentsadapter.domain.model.Payment
import com.bukuwarung.paymentsadapter.domain.repository.PaymentRepository
import javax.inject.Inject

class CreatePaymentUseCase @Inject constructor(
    private val repository: PaymentRepository
) {
    suspend operator fun invoke(
        externalId: String,
        partnerReferenceNo: String,
        amount: String,
        currency: String,
        channelId: String,
        payerId: String,
        payerAccountNo: String,
        expiredAt: String,
        metadata: Map<String, String>
    ): Result<Payment> = repository.createPayment(
        externalId, partnerReferenceNo, amount, currency,
        channelId, payerId, payerAccountNo, expiredAt, metadata
    )
}

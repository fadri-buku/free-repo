package com.bukuwarung.paymentsadapter.domain.repository

import com.bukuwarung.paymentsadapter.domain.model.Payment
import com.bukuwarung.paymentsadapter.domain.model.PaymentCancel

interface PaymentRepository {
    suspend fun createPayment(
        externalId: String,
        partnerReferenceNo: String,
        amount: String,
        currency: String,
        channelId: String,
        payerId: String,
        payerAccountNo: String,
        expiredAt: String,
        metadata: Map<String, String>
    ): Result<Payment>

    suspend fun getPaymentStatus(referenceNo: String): Result<Payment>

    suspend fun cancelPayment(referenceNo: String, reason: String): Result<PaymentCancel>
}

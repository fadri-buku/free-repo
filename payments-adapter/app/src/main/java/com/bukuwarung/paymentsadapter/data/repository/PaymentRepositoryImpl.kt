package com.bukuwarung.paymentsadapter.data.repository

import com.bukuwarung.paymentsadapter.common.TokenProvider
import com.bukuwarung.paymentsadapter.data.remote.api.PaymentApi
import com.bukuwarung.paymentsadapter.data.remote.dto.payment.CancelPaymentRequestDto
import com.bukuwarung.paymentsadapter.data.remote.dto.payment.CreatePaymentRequestDto
import com.bukuwarung.paymentsadapter.data.remote.mapper.PaymentMapper
import com.bukuwarung.paymentsadapter.domain.model.Payment
import com.bukuwarung.paymentsadapter.domain.model.PaymentCancel
import com.bukuwarung.paymentsadapter.domain.repository.PaymentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PaymentRepositoryImpl @Inject constructor(
    private val api: PaymentApi,
    private val mapper: PaymentMapper,
    private val tokenProvider: TokenProvider
) : PaymentRepository {

    private fun bearerToken(): String = "Bearer ${tokenProvider.getAccessToken().orEmpty()}"

    override suspend fun createPayment(
        externalId: String,
        partnerReferenceNo: String,
        amount: String,
        currency: String,
        channelId: String,
        payerId: String,
        payerAccountNo: String,
        expiredAt: String,
        metadata: Map<String, String>
    ): Result<Payment> = withContext(Dispatchers.IO) {
        runCatching {
            mapper.toDomain(
                api.createPayment(
                    authorization = bearerToken(),
                    externalId = externalId,
                    request = CreatePaymentRequestDto(
                        partnerReferenceNo, amount, currency, channelId,
                        payerId, payerAccountNo, expiredAt, metadata
                    )
                )
            )
        }
    }

    override suspend fun getPaymentStatus(referenceNo: String): Result<Payment> =
        withContext(Dispatchers.IO) {
            runCatching { mapper.toDomain(api.getPaymentStatus(bearerToken(), referenceNo)) }
        }

    override suspend fun cancelPayment(referenceNo: String, reason: String): Result<PaymentCancel> =
        withContext(Dispatchers.IO) {
            runCatching {
                mapper.toCancelDomain(
                    api.cancelPayment(bearerToken(), referenceNo, CancelPaymentRequestDto(reason))
                )
            }
        }
}

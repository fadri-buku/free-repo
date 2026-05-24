package com.bukuwarung.paymentsadapter.data.remote.mapper

import com.bukuwarung.paymentsadapter.data.remote.dto.payment.CancelPaymentResponseDto
import com.bukuwarung.paymentsadapter.data.remote.dto.payment.PaymentResponseDto
import com.bukuwarung.paymentsadapter.domain.model.Payment
import com.bukuwarung.paymentsadapter.domain.model.PaymentCancel
import com.bukuwarung.paymentsadapter.domain.model.PaymentStatus
import javax.inject.Inject

class PaymentMapper @Inject constructor() {
    fun toDomain(dto: PaymentResponseDto): Payment = Payment(
        referenceNo = requireNotNull(dto.referenceNo),
        partnerReferenceNo = dto.partnerReferenceNo ?: "",
        status = parseStatus(dto.status),
        amount = dto.amount ?: "",
        currency = dto.currency ?: "IDR",
        provider = dto.provider ?: "",
        providerReferenceNo = dto.providerReferenceNo,
        paymentUrl = dto.paymentUrl,
        expiredAt = dto.expiredAt,
        occurredAt = dto.occurredAt ?: "",
        completedAt = dto.completedAt
    )

    fun toCancelDomain(dto: CancelPaymentResponseDto): PaymentCancel = PaymentCancel(
        referenceNo = requireNotNull(dto.referenceNo),
        status = dto.status ?: ""
    )

    private fun parseStatus(raw: String?): PaymentStatus = runCatching {
        PaymentStatus.valueOf(raw ?: "PENDING")
    }.getOrDefault(PaymentStatus.PENDING)
}

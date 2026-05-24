package com.bukuwarung.paymentsadapter.domain.model

data class Payment(
    val referenceNo: String,
    val partnerReferenceNo: String,
    val status: PaymentStatus,
    val amount: String,
    val currency: String,
    val provider: String,
    val providerReferenceNo: String?,
    val paymentUrl: String?,
    val expiredAt: String?,
    val occurredAt: String,
    val completedAt: String?
)

enum class PaymentStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, EXPIRED, REVERSED;

    val isTerminal get() = this == COMPLETED || this == FAILED || this == EXPIRED || this == REVERSED
}

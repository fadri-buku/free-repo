package com.bukuwarung.paymentsadapter.domain.model

data class Disbursement(
    val referenceNo: String,
    val partnerReferenceNo: String,
    val providerReferenceNo: String?,
    val status: DisbursementStatus,
    val amount: String,
    val currency: String,
    val beneficiaryAccountNo: String,
    val beneficiaryName: String,
    val provider: String,
    val occurredAt: String,
    val settledAt: String?,
    val estimatedSettlementAt: String?
)

enum class DisbursementStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, REVERSED;

    val isTerminal get() = this == COMPLETED || this == FAILED || this == REVERSED
}

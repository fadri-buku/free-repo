package com.bukuwarung.paymentsadapter.data.remote.dto.disbursement

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DisbursementResponseDto(
    @Json(name = "response_code") val responseCode: String,
    @Json(name = "response_message") val responseMessage: String,
    @Json(name = "reference_no") val referenceNo: String?,
    @Json(name = "partner_reference_no") val partnerReferenceNo: String?,
    @Json(name = "provider_reference_no") val providerReferenceNo: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "amount") val amount: String?,
    @Json(name = "currency") val currency: String?,
    @Json(name = "beneficiary_account_no") val beneficiaryAccountNo: String?,
    @Json(name = "beneficiary_name") val beneficiaryName: String?,
    @Json(name = "provider") val provider: String?,
    @Json(name = "occurred_at") val occurredAt: String?,
    @Json(name = "settled_at") val settledAt: String?,
    @Json(name = "estimated_settlement_at") val estimatedSettlementAt: String?
)

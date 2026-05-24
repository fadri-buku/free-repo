package com.bukuwarung.paymentsadapter.data.remote.dto.payment

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaymentResponseDto(
    @Json(name = "response_code") val responseCode: String,
    @Json(name = "response_message") val responseMessage: String,
    @Json(name = "reference_no") val referenceNo: String?,
    @Json(name = "partner_reference_no") val partnerReferenceNo: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "amount") val amount: String?,
    @Json(name = "currency") val currency: String?,
    @Json(name = "provider") val provider: String?,
    @Json(name = "provider_reference_no") val providerReferenceNo: String?,
    @Json(name = "payment_url") val paymentUrl: String?,
    @Json(name = "expired_at") val expiredAt: String?,
    @Json(name = "occurred_at") val occurredAt: String?,
    @Json(name = "completed_at") val completedAt: String?
)

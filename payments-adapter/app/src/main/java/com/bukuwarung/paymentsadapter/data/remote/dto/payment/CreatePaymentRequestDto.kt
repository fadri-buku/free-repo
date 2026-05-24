package com.bukuwarung.paymentsadapter.data.remote.dto.payment

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreatePaymentRequestDto(
    @Json(name = "partner_reference_no") val partnerReferenceNo: String,
    @Json(name = "amount") val amount: String,
    @Json(name = "currency") val currency: String,
    @Json(name = "channel_id") val channelId: String,
    @Json(name = "payer_id") val payerId: String,
    @Json(name = "payer_account_no") val payerAccountNo: String,
    @Json(name = "expired_at") val expiredAt: String,
    @Json(name = "metadata") val metadata: Map<String, String>
)

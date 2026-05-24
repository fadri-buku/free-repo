package com.bukuwarung.paymentsadapter.data.remote.dto.payment

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CancelPaymentResponseDto(
    @Json(name = "response_code") val responseCode: String,
    @Json(name = "response_message") val responseMessage: String,
    @Json(name = "reference_no") val referenceNo: String?,
    @Json(name = "status") val status: String?
)

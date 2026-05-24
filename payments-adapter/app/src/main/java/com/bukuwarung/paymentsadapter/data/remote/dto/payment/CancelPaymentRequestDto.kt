package com.bukuwarung.paymentsadapter.data.remote.dto.payment

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CancelPaymentRequestDto(
    @Json(name = "reason") val reason: String
)

package com.bukuwarung.paymentsadapter.data.remote.dto.admin

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReloadRoutingResponseDto(
    @Json(name = "response_code") val responseCode: String,
    @Json(name = "response_message") val responseMessage: String,
    @Json(name = "reloaded_at") val reloadedAt: String?
)

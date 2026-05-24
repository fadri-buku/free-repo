package com.bukuwarung.paymentsadapter.data.remote.dto.admin

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FlagsResponseDto(
    @Json(name = "response_code") val responseCode: String,
    @Json(name = "response_message") val responseMessage: String,
    @Json(name = "flags") val flags: Map<String, Boolean>?
)

package com.bukuwarung.paymentsadapter.data.remote.dto.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenResponseDto(
    @Json(name = "response_code") val responseCode: String,
    @Json(name = "response_message") val responseMessage: String,
    @Json(name = "access_token") val accessToken: String?,
    @Json(name = "token_type") val tokenType: String?,
    @Json(name = "expires_in") val expiresIn: Int?
)

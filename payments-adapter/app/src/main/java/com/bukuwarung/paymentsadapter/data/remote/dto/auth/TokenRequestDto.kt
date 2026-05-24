package com.bukuwarung.paymentsadapter.data.remote.dto.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenRequestDto(
    @Json(name = "grantType") val grantType: String = "client_credentials"
)

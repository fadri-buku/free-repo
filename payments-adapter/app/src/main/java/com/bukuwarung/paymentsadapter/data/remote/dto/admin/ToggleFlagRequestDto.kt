package com.bukuwarung.paymentsadapter.data.remote.dto.admin

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ToggleFlagRequestDto(
    @Json(name = "enabled") val enabled: Boolean
)

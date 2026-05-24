package com.bukuwarung.paymentsadapter.data.remote.mapper

import com.bukuwarung.paymentsadapter.data.remote.dto.auth.TokenResponseDto
import com.bukuwarung.paymentsadapter.domain.model.AuthToken
import javax.inject.Inject

class AuthMapper @Inject constructor() {
    fun toDomain(dto: TokenResponseDto): AuthToken = AuthToken(
        accessToken = requireNotNull(dto.accessToken) { "access_token missing in response" },
        tokenType = dto.tokenType ?: "Bearer",
        expiresIn = dto.expiresIn ?: 900
    )
}

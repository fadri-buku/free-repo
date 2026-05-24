package com.bukuwarung.paymentsadapter.domain.model

data class AuthToken(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Int
)

package com.bukuwarung.paymentsadapter.domain.repository

import com.bukuwarung.paymentsadapter.domain.model.AuthToken

interface AuthRepository {
    suspend fun getToken(clientKey: String, privateKeyPem: String): Result<AuthToken>
}

package com.bukuwarung.paymentsadapter.data.repository

import com.bukuwarung.paymentsadapter.common.TokenProvider
import com.bukuwarung.paymentsadapter.data.remote.api.AuthApi
import com.bukuwarung.paymentsadapter.data.remote.dto.auth.TokenRequestDto
import com.bukuwarung.paymentsadapter.data.remote.mapper.AuthMapper
import com.bukuwarung.paymentsadapter.data.remote.signing.RsaSigningService
import com.bukuwarung.paymentsadapter.domain.model.AuthToken
import com.bukuwarung.paymentsadapter.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val rsaSigningService: RsaSigningService,
    private val authMapper: AuthMapper,
    private val tokenProvider: TokenProvider
) : AuthRepository {

    override suspend fun getToken(clientKey: String, privateKeyPem: String): Result<AuthToken> =
        withContext(Dispatchers.IO) {
            runCatching {
                val timestamp = rsaSigningService.currentTimestamp()
                val signature = rsaSigningService.sign(clientKey, timestamp, privateKeyPem)
                val response = authApi.getToken(
                    clientKey = clientKey,
                    timestamp = timestamp,
                    signature = signature,
                    request = TokenRequestDto()
                )
                val token = authMapper.toDomain(response)
                tokenProvider.setAccessToken(token.accessToken)
                token
            }
        }
}

package com.bukuwarung.paymentsadapter.domain.usecase.auth

import com.bukuwarung.paymentsadapter.domain.model.AuthToken
import com.bukuwarung.paymentsadapter.domain.repository.AuthRepository
import javax.inject.Inject

class GetTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(clientKey: String, privateKeyPem: String): Result<AuthToken> =
        authRepository.getToken(clientKey, privateKeyPem)
}

package com.bukuwarung.paymentsadapter.data.remote.api

import com.bukuwarung.paymentsadapter.data.remote.dto.auth.TokenRequestDto
import com.bukuwarung.paymentsadapter.data.remote.dto.auth.TokenResponseDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("v1/auth/token")
    suspend fun getToken(
        @Header("X-CLIENT-KEY") clientKey: String,
        @Header("X-TIMESTAMP") timestamp: String,
        @Header("X-SIGNATURE") signature: String,
        @Body request: TokenRequestDto
    ): TokenResponseDto
}

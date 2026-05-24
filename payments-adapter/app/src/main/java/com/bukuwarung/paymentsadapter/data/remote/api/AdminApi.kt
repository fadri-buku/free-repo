package com.bukuwarung.paymentsadapter.data.remote.api

import com.bukuwarung.paymentsadapter.data.remote.dto.admin.FlagsResponseDto
import com.bukuwarung.paymentsadapter.data.remote.dto.admin.ProviderHealthResponseDto
import com.bukuwarung.paymentsadapter.data.remote.dto.admin.ReloadRoutingResponseDto
import com.bukuwarung.paymentsadapter.data.remote.dto.admin.ToggleFlagRequestDto
import com.bukuwarung.paymentsadapter.data.remote.dto.admin.ToggleFlagResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AdminApi {
    @GET("v1/admin/providers/health")
    suspend fun getProviderHealth(
        @Header("Authorization") authorization: String
    ): ProviderHealthResponseDto

    @GET("v1/admin/flags")
    suspend fun getFlags(
        @Header("Authorization") authorization: String
    ): FlagsResponseDto

    @PUT("v1/admin/flags/{key}")
    suspend fun toggleFlag(
        @Header("Authorization") authorization: String,
        @Path("key") key: String,
        @Body request: ToggleFlagRequestDto
    ): ToggleFlagResponseDto

    @POST("v1/admin/routing/reload")
    suspend fun reloadRouting(
        @Header("Authorization") authorization: String
    ): ReloadRoutingResponseDto
}

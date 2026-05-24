package com.bukuwarung.paymentsadapter.data.remote.api

import com.bukuwarung.paymentsadapter.data.remote.dto.disbursement.AccountInquiryRequestDto
import com.bukuwarung.paymentsadapter.data.remote.dto.disbursement.AccountInquiryResponseDto
import com.bukuwarung.paymentsadapter.data.remote.dto.disbursement.CreateDisbursementRequestDto
import com.bukuwarung.paymentsadapter.data.remote.dto.disbursement.DisbursementResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface DisbursementApi {
    @POST("v1/disbursements/account-inquiry")
    suspend fun accountInquiry(
        @Header("Authorization") authorization: String,
        @Body request: AccountInquiryRequestDto
    ): AccountInquiryResponseDto

    @POST("v1/disbursements")
    suspend fun createDisbursement(
        @Header("Authorization") authorization: String,
        @Header("X-EXTERNAL-ID") externalId: String,
        @Body request: CreateDisbursementRequestDto
    ): DisbursementResponseDto

    @GET("v1/disbursements/{reference_no}")
    suspend fun getDisbursementStatus(
        @Header("Authorization") authorization: String,
        @Path("reference_no") referenceNo: String
    ): DisbursementResponseDto
}

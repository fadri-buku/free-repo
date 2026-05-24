package com.bukuwarung.paymentsadapter.data.remote.api

import com.bukuwarung.paymentsadapter.data.remote.dto.payment.CancelPaymentRequestDto
import com.bukuwarung.paymentsadapter.data.remote.dto.payment.CancelPaymentResponseDto
import com.bukuwarung.paymentsadapter.data.remote.dto.payment.CreatePaymentRequestDto
import com.bukuwarung.paymentsadapter.data.remote.dto.payment.PaymentResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface PaymentApi {
    @POST("v1/payments")
    suspend fun createPayment(
        @Header("Authorization") authorization: String,
        @Header("X-EXTERNAL-ID") externalId: String,
        @Body request: CreatePaymentRequestDto
    ): PaymentResponseDto

    @GET("v1/payments/{reference_no}")
    suspend fun getPaymentStatus(
        @Header("Authorization") authorization: String,
        @Path("reference_no") referenceNo: String
    ): PaymentResponseDto

    @POST("v1/payments/{reference_no}/cancel")
    suspend fun cancelPayment(
        @Header("Authorization") authorization: String,
        @Path("reference_no") referenceNo: String,
        @Body request: CancelPaymentRequestDto
    ): CancelPaymentResponseDto
}

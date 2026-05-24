package com.bukuwarung.paymentsadapter.data.remote.dto.disbursement

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AccountInquiryResponseDto(
    @Json(name = "response_code") val responseCode: String,
    @Json(name = "response_message") val responseMessage: String,
    @Json(name = "beneficiary_account_no") val beneficiaryAccountNo: String?,
    @Json(name = "beneficiary_bank_code") val beneficiaryBankCode: String?,
    @Json(name = "beneficiary_name") val beneficiaryName: String?,
    @Json(name = "beneficiary_account_status") val beneficiaryAccountStatus: String?,
    @Json(name = "inquiry_key") val inquiryKey: String?,
    @Json(name = "inquiry_expires_at") val inquiryExpiresAt: String?
)

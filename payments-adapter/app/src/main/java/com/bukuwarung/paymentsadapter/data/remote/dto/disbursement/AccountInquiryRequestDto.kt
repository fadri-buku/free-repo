package com.bukuwarung.paymentsadapter.data.remote.dto.disbursement

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AccountInquiryRequestDto(
    @Json(name = "beneficiary_account_no") val beneficiaryAccountNo: String,
    @Json(name = "beneficiary_bank_code") val beneficiaryBankCode: String,
    @Json(name = "source_account_no") val sourceAccountNo: String
)

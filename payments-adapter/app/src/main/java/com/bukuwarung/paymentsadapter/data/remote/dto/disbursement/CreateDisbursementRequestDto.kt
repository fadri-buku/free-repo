package com.bukuwarung.paymentsadapter.data.remote.dto.disbursement

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateDisbursementRequestDto(
    @Json(name = "partner_reference_no") val partnerReferenceNo: String,
    @Json(name = "amount") val amount: String,
    @Json(name = "currency") val currency: String,
    @Json(name = "beneficiary_account_no") val beneficiaryAccountNo: String,
    @Json(name = "beneficiary_bank_code") val beneficiaryBankCode: String,
    @Json(name = "beneficiary_name") val beneficiaryName: String,
    @Json(name = "source_account_no") val sourceAccountNo: String,
    @Json(name = "channel_id") val channelId: String,
    @Json(name = "remark") val remark: String,
    @Json(name = "inquiry_key") val inquiryKey: String?
)

package com.bukuwarung.paymentsadapter.domain.model

data class AccountInquiry(
    val beneficiaryAccountNo: String,
    val beneficiaryBankCode: String,
    val beneficiaryName: String,
    val beneficiaryAccountStatus: String,
    val inquiryKey: String,
    val inquiryExpiresAt: String
)

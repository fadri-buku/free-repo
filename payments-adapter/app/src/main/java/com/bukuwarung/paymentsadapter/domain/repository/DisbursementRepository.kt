package com.bukuwarung.paymentsadapter.domain.repository

import com.bukuwarung.paymentsadapter.domain.model.AccountInquiry
import com.bukuwarung.paymentsadapter.domain.model.Disbursement

interface DisbursementRepository {
    suspend fun accountInquiry(
        beneficiaryAccountNo: String,
        beneficiaryBankCode: String,
        sourceAccountNo: String
    ): Result<AccountInquiry>

    suspend fun createDisbursement(
        externalId: String,
        partnerReferenceNo: String,
        amount: String,
        currency: String,
        beneficiaryAccountNo: String,
        beneficiaryBankCode: String,
        beneficiaryName: String,
        sourceAccountNo: String,
        channelId: String,
        remark: String,
        inquiryKey: String?
    ): Result<Disbursement>

    suspend fun getDisbursementStatus(referenceNo: String): Result<Disbursement>
}

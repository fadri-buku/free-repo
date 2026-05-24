package com.bukuwarung.paymentsadapter.domain.usecase.disbursement

import com.bukuwarung.paymentsadapter.domain.model.Disbursement
import com.bukuwarung.paymentsadapter.domain.repository.DisbursementRepository
import javax.inject.Inject

class CreateDisbursementUseCase @Inject constructor(
    private val repository: DisbursementRepository
) {
    suspend operator fun invoke(
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
    ): Result<Disbursement> = repository.createDisbursement(
        externalId, partnerReferenceNo, amount, currency,
        beneficiaryAccountNo, beneficiaryBankCode, beneficiaryName,
        sourceAccountNo, channelId, remark, inquiryKey
    )
}

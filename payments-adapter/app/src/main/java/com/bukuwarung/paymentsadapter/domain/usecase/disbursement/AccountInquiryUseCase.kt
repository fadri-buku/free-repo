package com.bukuwarung.paymentsadapter.domain.usecase.disbursement

import com.bukuwarung.paymentsadapter.domain.model.AccountInquiry
import com.bukuwarung.paymentsadapter.domain.repository.DisbursementRepository
import javax.inject.Inject

class AccountInquiryUseCase @Inject constructor(
    private val repository: DisbursementRepository
) {
    suspend operator fun invoke(
        beneficiaryAccountNo: String,
        beneficiaryBankCode: String,
        sourceAccountNo: String
    ): Result<AccountInquiry> =
        repository.accountInquiry(beneficiaryAccountNo, beneficiaryBankCode, sourceAccountNo)
}

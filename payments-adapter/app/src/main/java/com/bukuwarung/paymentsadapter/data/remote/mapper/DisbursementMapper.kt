package com.bukuwarung.paymentsadapter.data.remote.mapper

import com.bukuwarung.paymentsadapter.data.remote.dto.disbursement.AccountInquiryResponseDto
import com.bukuwarung.paymentsadapter.data.remote.dto.disbursement.DisbursementResponseDto
import com.bukuwarung.paymentsadapter.domain.model.AccountInquiry
import com.bukuwarung.paymentsadapter.domain.model.Disbursement
import com.bukuwarung.paymentsadapter.domain.model.DisbursementStatus
import javax.inject.Inject

class DisbursementMapper @Inject constructor() {
    fun toAccountInquiryDomain(dto: AccountInquiryResponseDto): AccountInquiry = AccountInquiry(
        beneficiaryAccountNo = requireNotNull(dto.beneficiaryAccountNo),
        beneficiaryBankCode = requireNotNull(dto.beneficiaryBankCode),
        beneficiaryName = requireNotNull(dto.beneficiaryName),
        beneficiaryAccountStatus = requireNotNull(dto.beneficiaryAccountStatus),
        inquiryKey = requireNotNull(dto.inquiryKey),
        inquiryExpiresAt = requireNotNull(dto.inquiryExpiresAt)
    )

    fun toDisbursementDomain(dto: DisbursementResponseDto): Disbursement = Disbursement(
        referenceNo = requireNotNull(dto.referenceNo),
        partnerReferenceNo = dto.partnerReferenceNo ?: "",
        providerReferenceNo = dto.providerReferenceNo,
        status = parseStatus(dto.status),
        amount = dto.amount ?: "",
        currency = dto.currency ?: "IDR",
        beneficiaryAccountNo = dto.beneficiaryAccountNo ?: "",
        beneficiaryName = dto.beneficiaryName ?: "",
        provider = dto.provider ?: "",
        occurredAt = dto.occurredAt ?: "",
        settledAt = dto.settledAt,
        estimatedSettlementAt = dto.estimatedSettlementAt
    )

    private fun parseStatus(raw: String?): DisbursementStatus = runCatching {
        DisbursementStatus.valueOf(raw ?: "PENDING")
    }.getOrDefault(DisbursementStatus.PENDING)
}

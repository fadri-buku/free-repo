package com.bukuwarung.paymentsadapter.data.repository

import com.bukuwarung.paymentsadapter.common.TokenProvider
import com.bukuwarung.paymentsadapter.data.remote.api.DisbursementApi
import com.bukuwarung.paymentsadapter.data.remote.dto.disbursement.AccountInquiryRequestDto
import com.bukuwarung.paymentsadapter.data.remote.dto.disbursement.CreateDisbursementRequestDto
import com.bukuwarung.paymentsadapter.data.remote.mapper.DisbursementMapper
import com.bukuwarung.paymentsadapter.domain.model.AccountInquiry
import com.bukuwarung.paymentsadapter.domain.model.Disbursement
import com.bukuwarung.paymentsadapter.domain.repository.DisbursementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DisbursementRepositoryImpl @Inject constructor(
    private val api: DisbursementApi,
    private val mapper: DisbursementMapper,
    private val tokenProvider: TokenProvider
) : DisbursementRepository {

    private fun bearerToken(): String = "Bearer ${tokenProvider.getAccessToken().orEmpty()}"

    override suspend fun accountInquiry(
        beneficiaryAccountNo: String,
        beneficiaryBankCode: String,
        sourceAccountNo: String
    ): Result<AccountInquiry> = withContext(Dispatchers.IO) {
        runCatching {
            mapper.toAccountInquiryDomain(
                api.accountInquiry(
                    authorization = bearerToken(),
                    request = AccountInquiryRequestDto(beneficiaryAccountNo, beneficiaryBankCode, sourceAccountNo)
                )
            )
        }
    }

    override suspend fun createDisbursement(
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
    ): Result<Disbursement> = withContext(Dispatchers.IO) {
        runCatching {
            mapper.toDisbursementDomain(
                api.createDisbursement(
                    authorization = bearerToken(),
                    externalId = externalId,
                    request = CreateDisbursementRequestDto(
                        partnerReferenceNo, amount, currency, beneficiaryAccountNo,
                        beneficiaryBankCode, beneficiaryName, sourceAccountNo,
                        channelId, remark, inquiryKey
                    )
                )
            )
        }
    }

    override suspend fun getDisbursementStatus(referenceNo: String): Result<Disbursement> =
        withContext(Dispatchers.IO) {
            runCatching {
                mapper.toDisbursementDomain(
                    api.getDisbursementStatus(bearerToken(), referenceNo)
                )
            }
        }
}

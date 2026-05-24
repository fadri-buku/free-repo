package com.bukuwarung.paymentsadapter.domain.usecase.disbursement

import com.bukuwarung.paymentsadapter.domain.model.Disbursement
import com.bukuwarung.paymentsadapter.domain.repository.DisbursementRepository
import javax.inject.Inject

class GetDisbursementStatusUseCase @Inject constructor(
    private val repository: DisbursementRepository
) {
    suspend operator fun invoke(referenceNo: String): Result<Disbursement> =
        repository.getDisbursementStatus(referenceNo)
}

package com.bukuwarung.paymentsadapter.domain.usecase.admin

import com.bukuwarung.paymentsadapter.domain.model.FeatureFlags
import com.bukuwarung.paymentsadapter.domain.repository.AdminRepository
import javax.inject.Inject

class GetFlagsUseCase @Inject constructor(
    private val repository: AdminRepository
) {
    suspend operator fun invoke(): Result<FeatureFlags> = repository.getFlags()
}

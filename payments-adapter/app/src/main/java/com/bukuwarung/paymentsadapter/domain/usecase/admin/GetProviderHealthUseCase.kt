package com.bukuwarung.paymentsadapter.domain.usecase.admin

import com.bukuwarung.paymentsadapter.domain.model.ProviderHealth
import com.bukuwarung.paymentsadapter.domain.repository.AdminRepository
import javax.inject.Inject

class GetProviderHealthUseCase @Inject constructor(
    private val repository: AdminRepository
) {
    suspend operator fun invoke(): Result<ProviderHealth> = repository.getProviderHealth()
}

package com.bukuwarung.paymentsadapter.domain.usecase.admin

import com.bukuwarung.paymentsadapter.domain.model.RoutingReload
import com.bukuwarung.paymentsadapter.domain.repository.AdminRepository
import javax.inject.Inject

class ReloadRoutingUseCase @Inject constructor(
    private val repository: AdminRepository
) {
    suspend operator fun invoke(): Result<RoutingReload> = repository.reloadRouting()
}

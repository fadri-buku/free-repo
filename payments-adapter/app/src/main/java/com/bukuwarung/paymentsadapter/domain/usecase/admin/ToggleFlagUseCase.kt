package com.bukuwarung.paymentsadapter.domain.usecase.admin

import com.bukuwarung.paymentsadapter.domain.model.FlagToggleResult
import com.bukuwarung.paymentsadapter.domain.repository.AdminRepository
import javax.inject.Inject

class ToggleFlagUseCase @Inject constructor(
    private val repository: AdminRepository
) {
    suspend operator fun invoke(key: String, enabled: Boolean): Result<FlagToggleResult> =
        repository.toggleFlag(key, enabled)
}

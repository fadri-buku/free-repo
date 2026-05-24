package com.bukuwarung.paymentsadapter.data.repository

import com.bukuwarung.paymentsadapter.common.TokenProvider
import com.bukuwarung.paymentsadapter.data.remote.api.AdminApi
import com.bukuwarung.paymentsadapter.data.remote.dto.admin.ToggleFlagRequestDto
import com.bukuwarung.paymentsadapter.data.remote.mapper.AdminMapper
import com.bukuwarung.paymentsadapter.domain.model.FeatureFlags
import com.bukuwarung.paymentsadapter.domain.model.FlagToggleResult
import com.bukuwarung.paymentsadapter.domain.model.ProviderHealth
import com.bukuwarung.paymentsadapter.domain.model.RoutingReload
import com.bukuwarung.paymentsadapter.domain.repository.AdminRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AdminRepositoryImpl @Inject constructor(
    private val api: AdminApi,
    private val mapper: AdminMapper,
    private val tokenProvider: TokenProvider
) : AdminRepository {

    private fun bearerToken(): String = "Bearer ${tokenProvider.getAccessToken().orEmpty()}"

    override suspend fun getProviderHealth(): Result<ProviderHealth> =
        withContext(Dispatchers.IO) {
            runCatching { mapper.toProviderHealthDomain(api.getProviderHealth(bearerToken())) }
        }

    override suspend fun getFlags(): Result<FeatureFlags> =
        withContext(Dispatchers.IO) {
            runCatching { mapper.toFlagsDomain(api.getFlags(bearerToken())) }
        }

    override suspend fun toggleFlag(key: String, enabled: Boolean): Result<FlagToggleResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                mapper.toFlagToggleDomain(api.toggleFlag(bearerToken(), key, ToggleFlagRequestDto(enabled)))
            }
        }

    override suspend fun reloadRouting(): Result<RoutingReload> =
        withContext(Dispatchers.IO) {
            runCatching { mapper.toReloadDomain(api.reloadRouting(bearerToken())) }
        }
}

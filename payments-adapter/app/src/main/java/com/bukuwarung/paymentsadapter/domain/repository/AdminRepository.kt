package com.bukuwarung.paymentsadapter.domain.repository

import com.bukuwarung.paymentsadapter.domain.model.FeatureFlags
import com.bukuwarung.paymentsadapter.domain.model.FlagToggleResult
import com.bukuwarung.paymentsadapter.domain.model.ProviderHealth
import com.bukuwarung.paymentsadapter.domain.model.RoutingReload

interface AdminRepository {
    suspend fun getProviderHealth(): Result<ProviderHealth>
    suspend fun getFlags(): Result<FeatureFlags>
    suspend fun toggleFlag(key: String, enabled: Boolean): Result<FlagToggleResult>
    suspend fun reloadRouting(): Result<RoutingReload>
}

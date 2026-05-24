package com.bukuwarung.paymentsadapter.data.remote.mapper

import com.bukuwarung.paymentsadapter.data.remote.dto.admin.FlagsResponseDto
import com.bukuwarung.paymentsadapter.data.remote.dto.admin.ProviderHealthResponseDto
import com.bukuwarung.paymentsadapter.data.remote.dto.admin.ReloadRoutingResponseDto
import com.bukuwarung.paymentsadapter.data.remote.dto.admin.ToggleFlagResponseDto
import com.bukuwarung.paymentsadapter.domain.model.FeatureFlags
import com.bukuwarung.paymentsadapter.domain.model.FlagToggleResult
import com.bukuwarung.paymentsadapter.domain.model.ProviderHealth
import com.bukuwarung.paymentsadapter.domain.model.RoutingReload
import javax.inject.Inject

class AdminMapper @Inject constructor() {
    fun toProviderHealthDomain(dto: ProviderHealthResponseDto): ProviderHealth =
        ProviderHealth(providers = dto.providers ?: emptyMap())

    fun toFlagsDomain(dto: FlagsResponseDto): FeatureFlags =
        FeatureFlags(flags = dto.flags ?: emptyMap())

    fun toFlagToggleDomain(dto: ToggleFlagResponseDto): FlagToggleResult = FlagToggleResult(
        key = requireNotNull(dto.key),
        enabled = dto.enabled ?: false
    )

    fun toReloadDomain(dto: ReloadRoutingResponseDto): RoutingReload =
        RoutingReload(reloadedAt = dto.reloadedAt ?: "")
}

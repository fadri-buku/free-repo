package com.bukuwarung.paymentsadapter.domain.model

data class FeatureFlags(
    val flags: Map<String, Boolean>
)

data class FlagToggleResult(
    val key: String,
    val enabled: Boolean
)

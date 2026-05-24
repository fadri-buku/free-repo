package com.bukuwarung.paymentsadapter.common

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenProvider @Inject constructor() {

    private var accessToken: String? = null

    fun getAccessToken(): String? = accessToken

    fun setAccessToken(token: String) {
        accessToken = token
    }

    fun clearToken() {
        accessToken = null
    }
}

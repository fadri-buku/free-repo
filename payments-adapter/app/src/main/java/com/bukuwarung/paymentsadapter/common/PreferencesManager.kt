package com.bukuwarung.paymentsadapter.common

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("payments_adapter_prefs", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "") ?: ""
        set(value) = prefs.edit { putString(KEY_BASE_URL, value) }

    var clientKey: String
        get() = prefs.getString(KEY_CLIENT_KEY, "") ?: ""
        set(value) = prefs.edit { putString(KEY_CLIENT_KEY, value) }

    var privateKeyPem: String
        get() = prefs.getString(KEY_PRIVATE_KEY, "") ?: ""
        set(value) = prefs.edit { putString(KEY_PRIVATE_KEY, value) }

    var clientSecret: String
        get() = prefs.getString(KEY_CLIENT_SECRET, "") ?: ""
        set(value) = prefs.edit { putString(KEY_CLIENT_SECRET, value) }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_CLIENT_KEY = "client_key"
        private const val KEY_PRIVATE_KEY = "private_key_pem"
        private const val KEY_CLIENT_SECRET = "client_secret"
    }
}

package com.bukuwarung.paymentsadapter.di

import com.bukuwarung.paymentsadapter.BuildConfig
import com.bukuwarung.paymentsadapter.common.TokenProvider
import com.bukuwarung.paymentsadapter.data.remote.api.AdminApi
import com.bukuwarung.paymentsadapter.data.remote.api.AuthApi
import com.bukuwarung.paymentsadapter.data.remote.api.DisbursementApi
import com.bukuwarung.paymentsadapter.data.remote.api.PaymentApi
import com.bukuwarung.paymentsadapter.data.remote.interceptor.AuthInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenProvider: TokenProvider): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides @Singleton
    fun provideDisbursementApi(retrofit: Retrofit): DisbursementApi = retrofit.create(DisbursementApi::class.java)

    @Provides @Singleton
    fun providePaymentApi(retrofit: Retrofit): PaymentApi = retrofit.create(PaymentApi::class.java)

    @Provides @Singleton
    fun provideAdminApi(retrofit: Retrofit): AdminApi = retrofit.create(AdminApi::class.java)
}

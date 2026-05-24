package com.bukuwarung.paymentsadapter.di

import com.bukuwarung.paymentsadapter.data.repository.AdminRepositoryImpl
import com.bukuwarung.paymentsadapter.data.repository.AuthRepositoryImpl
import com.bukuwarung.paymentsadapter.data.repository.DisbursementRepositoryImpl
import com.bukuwarung.paymentsadapter.data.repository.PaymentRepositoryImpl
import com.bukuwarung.paymentsadapter.domain.repository.AdminRepository
import com.bukuwarung.paymentsadapter.domain.repository.AuthRepository
import com.bukuwarung.paymentsadapter.domain.repository.DisbursementRepository
import com.bukuwarung.paymentsadapter.domain.repository.PaymentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindDisbursementRepository(impl: DisbursementRepositoryImpl): DisbursementRepository

    @Binds @Singleton
    abstract fun bindPaymentRepository(impl: PaymentRepositoryImpl): PaymentRepository

    @Binds @Singleton
    abstract fun bindAdminRepository(impl: AdminRepositoryImpl): AdminRepository
}

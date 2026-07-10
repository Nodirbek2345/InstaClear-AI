package com.instaclear.data.processing.di

import com.instaclear.data.processing.repository.ProcessingRepositoryImpl
import com.instaclear.domain.repository.ProcessingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProcessingModule {

    @Binds
    @Singleton
    abstract fun bindProcessingRepository(
        processingRepositoryImpl: ProcessingRepositoryImpl
    ): ProcessingRepository
}

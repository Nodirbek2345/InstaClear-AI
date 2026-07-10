package com.instaclear.data.media.di

import com.instaclear.data.media.analyzer.MediaAnalyzerImpl
import com.instaclear.domain.repository.MediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        mediaAnalyzerImpl: MediaAnalyzerImpl
    ): MediaRepository
}

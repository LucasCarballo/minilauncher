package com.minilauncher.di

import android.content.pm.PackageManager
import com.minilauncher.data.repository.AppRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun providePackageManager(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
    ): PackageManager = context.packageManager
}
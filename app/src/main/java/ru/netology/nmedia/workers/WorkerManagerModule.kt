package ru.netology.nmedia.workers

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.netology.nmedia.dto.WorkerKeys
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class WorkerManagerModule {

    @Singleton
    @Provides
    fun provideWorkManager(
        @ApplicationContext
        context: Context,
    ): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Singleton
    @Provides
    fun provideTokenKey() = WorkerKeys()
}
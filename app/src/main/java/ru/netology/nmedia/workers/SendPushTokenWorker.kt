package ru.netology.nmedia.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.tasks.await
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dto.PushToken
import ru.netology.nmedia.dto.WorkerKeys
import javax.inject.Singleton

@HiltWorker
class SendPushTokenWorker @AssistedInject constructor(
    @Assisted
    private val context: Context,
    @Assisted
    workerParameters: WorkerParameters,
    private val workerKeys: WorkerKeys,
    private val firebaseMessaging: FirebaseMessaging,
) :
    CoroutineWorker(context, workerParameters) {

    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface AppAuthEntryPoint {
        fun getApiService(): ApiService
    }

    override suspend fun doWork(): Result =
        try {
            val pushToken = PushToken(
                inputData.getString(workerKeys.tokenKey) ?: firebaseMessaging.token.await()
            )
            val entryPoint =
                EntryPointAccessors.fromApplication(context, AppAuthEntryPoint::class.java)
            entryPoint.getApiService().sendPushToken(pushToken)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }

}
package ru.netology.nmedia.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dto.PushToken

class SendPushTokenWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    companion object {
        const val TOKEN_KEY = "TOKEN_KEY"
    }

    override suspend fun doWork(): Result =
        try {
            val pushToken = PushToken(
                inputData.getString(TOKEN_KEY) ?: FirebaseMessaging.getInstance().token.await()
            )
            PostsApi.service.sendPushToken(pushToken)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }

}
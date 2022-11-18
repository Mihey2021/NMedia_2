package ru.netology.nmedia.auth

import android.content.Context
import androidx.core.content.edit
import androidx.work.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dto.PushToken
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.workers.SendPushTokenWorker

class AppAuth private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val _authStateFlow: MutableStateFlow<Token?> = MutableStateFlow(null)
    val authStateFlow = _authStateFlow.asStateFlow()

    private val workManager: WorkManager = WorkManager.getInstance(context)

    init {
        val token = prefs.getString(TOKEN_KEY, null)
        val id = prefs.getLong(ID_KEY, 0L)

        if (token == null || id == 0L) {
            removeAuth()
        } else {
            _authStateFlow.value = Token(id, token)
        }
    }

    fun sendPushToken(token: String? = null) {
        val data = workDataOf(SendPushTokenWorker.TOKEN_KEY to token)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SendPushTokenWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .build()
        workManager.enqueue(request)
    }

    @Synchronized
    fun setAuth(id: Long, token: String) {
        _authStateFlow.value = Token(id, token)
        prefs.edit {
            putString(TOKEN_KEY, token)
            putLong(ID_KEY, id)
        }
        sendPushToken()
    }

    @Synchronized
    fun removeAuth() {
        _authStateFlow.value = null
        prefs.edit {
            remove(TOKEN_KEY)
            remove(ID_KEY)
        }
        sendPushToken()
    }

    companion object {
        private const val ID_KEY = "ID_KEY"
        private const val TOKEN_KEY = "TOKEN_KEY"

        @Volatile
        private var instance: AppAuth? = null

        fun getInstance(): AppAuth = requireNotNull(instance)

        fun initAuth(context: Context) {
            instance = AppAuth(context)
        }

    }
}
package ru.netology.nmedia.auth

import android.content.Context
import androidx.core.content.edit
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.dto.WorkerKeys
import ru.netology.nmedia.workers.SendPushTokenWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAuth @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val workManager: WorkManager,
    private val workerKeys: WorkerKeys,
) {

    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val _authStateFlow: MutableStateFlow<Token?> = MutableStateFlow(null)
    val authStateFlow = _authStateFlow.asStateFlow()


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
        val data = workDataOf(workerKeys.tokenKey to token)
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
    }
}
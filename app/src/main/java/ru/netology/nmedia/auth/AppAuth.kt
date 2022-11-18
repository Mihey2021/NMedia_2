package ru.netology.nmedia.auth

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.netology.nmedia.dto.Token

class AppAuth private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val _authStateFlow: MutableStateFlow<Token?> = MutableStateFlow(null)
    val authStateFlow = _authStateFlow.asStateFlow()

    init {
        val token = prefs.getString(TOKEN_KEY, null)
        val id = prefs.getLong(ID_KEY, 0L)

        if(token == null || id == 0L) {
            removeAuth()
        } else {
            _authStateFlow.value = Token(id, token)
        }
    }

    @Synchronized
    fun setAuth(id: Long, token: String) {
        _authStateFlow.value = Token(id, token)
        prefs.edit {
            putString(TOKEN_KEY, token)
            putLong(ID_KEY, id)
        }
    }

    @Synchronized
    fun removeAuth() {
        _authStateFlow.value = null
        prefs.edit {
            remove(TOKEN_KEY)
            remove(ID_KEY)
        }
    }

    companion object {
        private const val ID_KEY = "ID_KEY"
        private const val TOKEN_KEY = "TOKEN_KEY"
        @Volatile
        private var instance: AppAuth? = null

        fun getInstance(): AppAuth = requireNotNull(instance)

        fun initAuth(context: Context){
            instance = AppAuth(context)
        }

    }
}
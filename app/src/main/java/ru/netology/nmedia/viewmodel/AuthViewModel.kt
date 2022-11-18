package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.Dispatchers
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Token

class AuthViewModel : ViewModel() {

    val authData: LiveData<Token?> = AppAuth.getInstance().authStateFlow.asLiveData(Dispatchers.Default)

    val authorized: Boolean
        get() = AppAuth.getInstance().authStateFlow?.value?.token != null
}
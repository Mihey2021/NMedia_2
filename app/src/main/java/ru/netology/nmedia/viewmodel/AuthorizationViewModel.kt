package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.repository.AuthAndRegisterRepository
import ru.netology.nmedia.repository.AuthAndRegisterRepositoryImpl
import javax.inject.Inject

@HiltViewModel
class AuthorizationViewModel @Inject constructor(
    private val repository: AuthAndRegisterRepository,
) : ViewModel() {


    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val _authorizationData: MutableLiveData<Token?> = MutableLiveData(null)
    val authorizationData: LiveData<Token?>
        get() = _authorizationData

    fun authorization(login: String, pass: String) {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
                _authorizationData.value = repository.authentication(login, pass)
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                _authorizationData.value = null
                _dataState.value = FeedModelState(error = true, errorMessage = e.message)
            }
        }
    }
}
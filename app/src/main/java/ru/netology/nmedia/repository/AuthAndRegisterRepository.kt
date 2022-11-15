package ru.netology.nmedia.repository

import okhttp3.RequestBody
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Token

interface AuthAndRegisterRepository {

    suspend fun authentication(login: String, pass: String): Token
    suspend fun registration(login: String, pass: String, name: String): Token
    suspend fun registerWithPhoto(login: RequestBody, pass: RequestBody, name: RequestBody, avatar: MediaUpload): Token

}
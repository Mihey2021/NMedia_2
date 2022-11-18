package ru.netology.nmedia.repository

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.error.*
import java.io.IOException

class AuthAndRegisterRepositoryImpl : AuthAndRegisterRepository {

    override suspend fun authentication(login: String, pass: String): Token {
        try {
            val response = PostsApi.service.authentication(login, pass)
            if (!response.isSuccessful) {
                //При неверном пароле сервер возвращает код 400, при неверном логине 404
                if (response.code() == 400 || response.code() == 404)
                    throw AuthorizationError
                else
                    throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: AuthorizationError) {
            throw AuthorizationError
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun registration(login: String, pass: String, name: String): Token {
        try {
            val response = PostsApi.service.registration(login, pass, name)
            if (!response.isSuccessful) {
                //Если пользователь с таким логином существует, сервер возвращает код 403
                if (response.code() == 403)
                    throw RegistrationError
                else
                    throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: RegistrationError) {
            throw RegistrationError
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun registerWithPhoto(
        login: RequestBody,
        pass: RequestBody,
        name: RequestBody,
        avatar: MediaUpload
    ): Token {
        try {
            val media = MultipartBody.Part.createFormData(
                "media", avatar.file.name, avatar.file.asRequestBody()
            )
            val response = PostsApi.service.registerWithPhoto(login, pass, name, media)
            if (!response.isSuccessful) {
                //Если пользователь с таким логином существует, сервер возвращает код 403.
                if (response.code() == 403)
                    throw RegistrationError
                else
                    throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: RegistrationError) {
            throw RegistrationError
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }
}
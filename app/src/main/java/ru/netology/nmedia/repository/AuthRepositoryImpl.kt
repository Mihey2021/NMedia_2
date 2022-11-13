package ru.netology.nmedia.repository

import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.AuthorizationError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import java.io.IOException

class AuthRepositoryImpl : AuthRepository {

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
}
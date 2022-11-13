package ru.netology.nmedia.repository

import ru.netology.nmedia.dto.Token

interface AuthRepository {

    suspend fun authentication(login: String, pass: String): Token

}
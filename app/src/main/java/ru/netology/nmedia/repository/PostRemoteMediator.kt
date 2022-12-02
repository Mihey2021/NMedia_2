package ru.netology.nmedia.repository

import androidx.paging.*
import androidx.room.withTransaction
import retrofit2.HttpException
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val apiService: ApiService,
    private val postDao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val appDb: AppDb,
) : RemoteMediator<Int, PostEntity>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {
            val result = when (loadType) {
                LoadType.APPEND -> {
                    val id = postRemoteKeyDao.min() ?: return MediatorResult.Success(false)
                    apiService.getBefore(id, state.config.pageSize)
                }
                LoadType.PREPEND -> {
                    val id = postRemoteKeyDao.max() ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    apiService.getAfter(id, state.config.pageSize)
                }
                LoadType.REFRESH -> {
                    apiService.getLatest(state.config.initialLoadSize)
                }

            }
            if (!result.isSuccessful)
                throw HttpException(result)

            val body = result.body() ?: throw ApiError(result.code(), result.message())

            appDb.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
                        postDao.clear()
                        postRemoteKeyDao.clear()
                        postRemoteKeyDao.insert(
                            listOf(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.AFTER,
                                    body.first().id
                                )
                            )
                        )
                        if (postDao.isEmpty()) {
                            postRemoteKeyDao.insert(
                                listOf(
                                    PostRemoteKeyEntity(
                                        PostRemoteKeyEntity.KeyType.BEFORE,
                                        body.last().id
                                    )
                                )
                            )
                        }
                    }
                    LoadType.APPEND -> {
                        if (body.isNotEmpty()) {
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    type = PostRemoteKeyEntity.KeyType.BEFORE,
                                    key = body.last().id,
                                )
                            )
                        }
                    }
                    LoadType.PREPEND -> {
                        if (body.isNotEmpty()) {
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    type = PostRemoteKeyEntity.KeyType.AFTER,
                                    key = body.first().id,
                                )
                            )
                        }
                    }
                }
                if (body.isNotEmpty())
                    postDao.insert(body.toEntity())
            }

            return MediatorResult.Success(endOfPaginationReached = body.isEmpty())

        } catch (e: IOException) {
            return MediatorResult.Error(e)
        }
    }
}
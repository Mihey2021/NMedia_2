package ru.netology.nmedia.repository

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.paging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.internal.toLongOrDefault
import ru.netology.nmedia.api.*
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.*
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.enumeration.AttachmentType
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.AppError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val PAGE_SIZE = 10

class PostRepositoryImpl @Inject constructor(
    private val postDao: PostDao,
    private val apiService: ApiService,
    postRemoteKeyDao: PostRemoteKeyDao,
    appDb: AppDb,
) : PostRepository {
//    override val data = dao.getAll()
//        .map(List<PostEntity>::toDto)
//        .flowOn(Dispatchers.Default)

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalPagingApi::class)
    override val data: Flow<PagingData<FeedItem>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            //initialLoadSize = PAGE_SIZE,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { postDao.getPagingSource() },
        remoteMediator = PostRemoteMediator(
            apiService = apiService,
            postDao = postDao,
            postRemoteKeyDao = postRemoteKeyDao,
            appDb = appDb,
        )
    ).flow
        .map {
            var todayShowing = false
            var yesterdayShowing = false
            var lastWeekShowing = false
            it.map(PostEntity::toDto)
                .insertSeparators { previous, _ ->
                    val currentDateInSeconds = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                    if (previous != null) {
                        val text = getTimingText(
                            previous.published.toLongOrDefault(0L),
                            currentDateInSeconds
                        )
                        if (text == Timing.TODAY && todayShowing) return@insertSeparators null
                        if (text == Timing.YESTERDAY && yesterdayShowing) return@insertSeparators null
                        if (text == Timing.LAST_WEEK && lastWeekShowing) return@insertSeparators null

                        if (text == Timing.TODAY) todayShowing = true
                        if (text == Timing.YESTERDAY) yesterdayShowing = true
                        if (text == Timing.LAST_WEEK) lastWeekShowing = true

                        Timing(currentDateInSeconds, text)
                    } else {
                        null
                    }
                }
        }

    private fun getTimingText(publishedDate: Long, currentDateInSeconds: Long): String {

        val hoursDifference = TimeUnit.SECONDS.toHours(currentDateInSeconds - publishedDate)

        if (hoursDifference <= 24) {
            return Timing.TODAY
        }

        if (hoursDifference in 24..48) {
            return Timing.YESTERDAY
        }

        return Timing.LAST_WEEK
    }

    override suspend fun getAll() {
        try {
            val response = apiService.getAll()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override fun getNewerCount(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000L)
            val notShowingPostsCount = postDao.count()
            val response = apiService.getNewer(id + notShowingPostsCount)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())

            postDao.insert(body.toEntity().map { it.copy(notShownYet = true) })
            emit(postDao.getNotShownPosts().size)
        }
    }
        .catch { e -> throw AppError.from(e) }
        .flowOn(Dispatchers.IO)

    override suspend fun getNotShownPosts() = postDao.getNotShownPosts().map { it.toDto() }

    override suspend fun updatePostShowingState() {
        val posts = getNotShownPosts()
        postDao.insert(posts.map { PostEntity.fromDto(it).copy(notShownYet = false) })
    }

    override suspend fun processingNotSavedPosts() {
        try {
            postDao.getNotSavedPosts().forEach { postEntity ->
                save(postEntity.toDto())
            }
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun save(post: Post) {
        try {
            val id = postDao.getNotSavedPostById(post.id)?.id ?: ((postDao.getMinNotSavePostId()
                ?: 0L) - 1L)
            val notSaved = (id == post.id)
            postDao.insert(PostEntity.fromDto(post.copy(id = id)).copy(notSaved = true))
            val response =
                if (notSaved) apiService.save(post.copy(id = 0L)) else apiService.save(post)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.removeById(id)
            postDao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeById(id: Long) {
        try {
            val response = apiService.removeById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            if (response.code() == 200) postDao.removeById(id)
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun likeById(id: Long, likedByMe: Boolean) {
        if (likedByMe) {
            disLikeById(id)
            return
        }

        try {
            val response = apiService.likeById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }

    }

    private suspend fun disLikeById(id: Long) {
        try {
            val response = apiService.dislikeById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveWithAttachment(post: Post, upload: MediaUpload) {
        try {
            val media = upload(upload)
            // TODO: add support for other types
            val postWithAttachment =
                post.copy(attachment = Attachment(media.id, AttachmentType.IMAGE))
            save(postWithAttachment)
        } catch (e: AppError) {
            throw e
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun upload(upload: MediaUpload): Media {
        try {
            val media = MultipartBody.Part.createFormData(
                "file", upload.file.name, upload.file.asRequestBody()
            )

            val response = apiService.upload(media)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

}

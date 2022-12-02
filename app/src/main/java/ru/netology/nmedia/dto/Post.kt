package ru.netology.nmedia.dto

import ru.netology.nmedia.enumeration.AttachmentType

sealed interface FeedItem {
    val id: Long
}

data class Post(
    override val id: Long,
    val author: String,
    val authorId: Long,
    val authorAvatar: String,
    val content: String,
    val published: String,
    val likedByMe: Boolean,
    val likes: Int = 0,
    val notSaved: Boolean = false,
    val attachment: Attachment? = null,
    val ownedByMe: Boolean = false,
) : FeedItem

data class Timing(
    override val id: Long,
    val timingText: String,
) : FeedItem {
    companion object {
        const val TODAY = "Сегодня"
        const val YESTERDAY = "Вчера"
        const val LAST_WEEK = "На прошлой неделе"
    }
}

data class Attachment(
    val url: String,
    val type: AttachmentType,
)

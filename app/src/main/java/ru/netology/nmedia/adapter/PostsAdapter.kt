package ru.netology.nmedia.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import okhttp3.internal.toLongOrDefault
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.databinding.CardTimingBinding
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.Timing
import ru.netology.nmedia.enumeration.AttachmentType
import ru.netology.nmedia.view.loadCircleCrop
import java.text.SimpleDateFormat
import java.util.*

interface OnInteractionListener {
    fun onLike(post: Post) {}
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
    fun onShare(post: Post) {}
    fun onPhotoView(photoUrl: String) {}
}

class PostsAdapter(
    private val onInteractionListener: OnInteractionListener,
) : PagingDataAdapter<FeedItem, RecyclerView.ViewHolder>(PostDiffCallback()) {
    override fun getItemViewType(position: Int) =
        when (getItem(position)) {
            is Post -> R.id.postCardView
            is Timing -> R.id.cardTiming
            null -> error("Unknown item type")
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.id.postCardView -> {
                val binding =
                    CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PostViewHolder(binding, onInteractionListener)
            }
            R.id.cardTiming -> {
                val binding =
                    CardTimingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                TimingViewHolder(binding)
            }
            else -> error("Unknown view type: $viewType")
        }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(val item = getItem(position)) {
            is Post -> (holder as? PostViewHolder)?.bind(item)
            is Timing -> (holder as? TimingViewHolder)?.bind(item)
            null -> error("Unknown item type")
        }
    }
}

class TimingViewHolder(
    private val binding: CardTimingBinding,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(timing: Timing) {
        binding.timingTextView.text = timing.timingText
    }
}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SimpleDateFormat")
    fun bind(post: Post) {
        binding.apply {
            notSavedTextView.visibility = if (post.notSaved) View.VISIBLE else View.GONE
            author.text = post.author
            published.text = SimpleDateFormat("dd.MM.yyyy HH:MM:ss").format(Date(post.published.toLongOrDefault(0L) * 1000))
            content.text = post.content
            avatar.loadCircleCrop("${BuildConfig.BASE_URL}/avatars/${post.authorAvatar}")
            like.isChecked = post.likedByMe
            like.text = "${post.likes}"
            like.isEnabled = !post.notSaved
            share.isEnabled = !post.notSaved

            menu.isVisible = post.ownedByMe

            val attachment = post.attachment
            if (attachment != null && attachment.type == AttachmentType.IMAGE) {
                attachmentImageView.visibility = View.VISIBLE

                Glide.with(attachmentImageView)
                    .load("${BuildConfig.BASE_URL}/media/${post.attachment.url}")
                    .placeholder(R.drawable.ic_baseline_loading_24)
                    .error(R.drawable.ic_baseline_non_loaded_image_24)
                    .timeout(10_000)
                    .into(attachmentImageView)

                attachmentImageView.setOnClickListener {
                    onInteractionListener.onPhotoView(post.attachment.url)
                }
            } else {
                attachmentImageView.visibility = View.GONE
            }

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                onInteractionListener.onRemove(post)
                                true
                            }
                            R.id.edit -> {
                                onInteractionListener.onEdit(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            like.setOnClickListener {
                onInteractionListener.onLike(post)
            }

            share.setOnClickListener {
                onInteractionListener.onShare(post)
            }
        }
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
    override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        if (oldItem::class != newItem::class) {
            return false
        }

        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        return oldItem == newItem
    }
}

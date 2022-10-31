package ru.netology.nmedia.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.NewPostFragment.Companion.textArg
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.databinding.FragmentViewPhotoBinding
import ru.netology.nmedia.util.StringArg


class ViewPhotoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentViewPhotoBinding.inflate(
            inflater,
            container,
            false
        )

        val photoUrl = arguments?.attachmentPhotoUrl
        val attachmentImageView = binding.attachmentImageView
        Glide.with(attachmentImageView)
            .load("${BuildConfig.BASE_URL}/media/${photoUrl}")
            .placeholder(R.drawable.ic_baseline_loading_24)
            .error(R.drawable.ic_baseline_non_loaded_image_24)
            .timeout(10_000)
            .into(attachmentImageView)

        return binding.root
    }

    companion object {
        var Bundle.attachmentPhotoUrl: String? by StringArg
    }
}
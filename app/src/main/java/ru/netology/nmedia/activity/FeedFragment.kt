package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearSmoothScroller
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.ViewPhotoFragment.Companion.attachmentPhotoUrl
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dialogs.NetologyDialogs
import ru.netology.nmedia.dialogs.OnDialogsInteractionListener
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.view.MenuState
import ru.netology.nmedia.view.MenuStates
import ru.netology.nmedia.viewmodel.PostViewModel

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        binding.newPostsShowing.setOnClickListener {
            it.isVisible = false

            viewModel.updateShowingPostsState()

            val smoothScroller = object : LinearSmoothScroller(requireContext()) {
                override fun getVerticalSnapPreference(): Int {
                    return SNAP_TO_START
                }
            }
            smoothScroller.targetPosition = 0
            binding.list.layoutManager?.startSmoothScroll(smoothScroller)
            binding.list.smoothScrollToPosition(0)
        }

        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                viewModel.edit(post)
            }

            override fun onLike(post: Post) {
                if (!viewModel.authorized)
                    showAuthorizationQuestionDialog()
                else
                    viewModel.likeById(post.id, post.likedByMe)
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun onShare(post: Post) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }

                val shareIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }

            override fun onPhotoView(photoUrl: String) {
                findNavController().navigate(
                    R.id.action_feedFragment_to_viewPhotoFragment,
                    Bundle().apply { attachmentPhotoUrl = photoUrl })
            }
        })

        binding.list.adapter = adapter

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            //binding.swiperefresh.isRefreshing = state.refreshing
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) { viewModel.loadPosts() }
                    .show()
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.data.collectLatest {
                adapter.submitData(it)
            }
        }

//        viewModel.data.observe(viewLifecycleOwner) { state ->
//            adapter.submitList(state.posts)
//            binding.emptyText.isVisible = state.empty
//        }
//        viewModel.newerCount.observe(viewLifecycleOwner) { state ->
//            binding.newPostsShowing.isVisible = state > 0
//            println(state)
//        }

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collectLatest {
                binding.swiperefresh.isRefreshing = it.refresh is LoadState.Loading
                        || it.append is LoadState.Loading
                        || it.prepend is LoadState.Loading
            }

        }

        viewModel.authData.observe(viewLifecycleOwner) {
            adapter.refresh()
        }

        binding.swiperefresh.setOnRefreshListener {
            viewModel.loadPosts()
        }

        binding.fab.setOnClickListener {
            if (!viewModel.authorized)
                showAuthorizationQuestionDialog()
            else
                findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)

        }

        return binding.root
    }

    private fun showAuthorizationQuestionDialog() {
        NetologyDialogs.getDialog(requireContext(),
            NetologyDialogs.QUESTION_DIALOG,
            title = getString(R.string.authorization),
            message = getString(R.string.do_you_want_to_login),
            titleIcon = R.drawable.ic_baseline_lock_24,
            positiveButtonTitle = getString(R.string.yes_text),
            onDialogsInteractionListener = object : OnDialogsInteractionListener {
                override fun onPositiveClickButton() {
                    findNavController().navigate(R.id.action_feedFragment_to_authFragment)
                }
            })
    }

    override fun onResume() {
        super.onResume()
        MenuState.setMenuState(MenuStates.SHOW_STATE)
        requireActivity().invalidateMenu()
    }
}

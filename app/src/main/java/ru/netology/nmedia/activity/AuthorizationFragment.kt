package ru.netology.nmedia.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnKeyListener
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.annotation.RequiresApi
import androidx.core.view.marginTop
import androidx.core.view.setMargins
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.FragmentAuthorizationBinding
import ru.netology.nmedia.error.AuthorizationError
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.viewmodel.AuthorizationViewModel

class AuthorizationFragment : Fragment() {

    private val viewModel by viewModels<AuthorizationViewModel>()
    private var dialog: AlertDialog? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = FragmentAuthorizationBinding.inflate(inflater, container, false)

        with(binding) {
            signInButton.setOnClickListener {
                if (checkForm(binding)) {
                    AndroidUtils.hideKeyboard(requireView())
                    //Авторизация на сервере
                    viewModel.getAuthorizationToken(
                        binding.loginEditText.text.toString(),
                        binding.passwordEditText.text.toString()
                    )
                }
            }

            loginEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    loginTextInputLayout.error = null
                }

                override fun afterTextChanged(p0: Editable?) {
                }
            })

            passwordEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    passwordTextInputLayout.error = null
                }

                override fun afterTextChanged(p0: Editable?) {
                }
            })

        }

        viewModel.authorizationData.observe(viewLifecycleOwner) { token ->
            if (token == null) return@observe

            AppAuth.getInstance().setAuth(token.id, token.token ?: "")
            findNavController().navigateUp()
        }

        viewModel.dataState.observe(viewLifecycleOwner) {

            dialog?.dismiss()

            if (it.loading) {
                dialog = AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.authorization))
                    .setView(ProgressBar(requireContext()).apply {
                        this.layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setPadding(0, 52, 0, 52)
                        }
                    }

                    )

                    .setCancelable(false)
                    .show()
            }

            if (it.error) {
                dialog = AlertDialog.Builder(requireContext())
                    .setTitle(it.errorMessage)
                    .setIcon(R.drawable.ic_baseline_error_24)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok_text) { _, _ ->
                        dialog?.dismiss()
                    }
                    .show()
            }
        }

        return binding.root
    }

    private fun checkForm(binding: FragmentAuthorizationBinding): Boolean {
        var isCorrect = true

        if (binding.loginEditText.text.isNullOrBlank()) {
            binding.loginTextInputLayout.error = getString(R.string.enter_your_username)
            isCorrect = false
        }

        if (binding.passwordEditText.text.isNullOrBlank()) {
            binding.passwordTextInputLayout.error = getString(R.string.enter_the_password)
            isCorrect = false
        }

        return isCorrect
    }


}
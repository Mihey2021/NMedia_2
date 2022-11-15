package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.NewPostFragment.Companion.textArg
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.view.MenuState
import ru.netology.nmedia.view.MenuStates
import ru.netology.nmedia.viewmodel.AuthViewModel

class AppActivity : AppCompatActivity(R.layout.activity_app) {

    var menuState = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let {
            if (it.action != Intent.ACTION_SEND) {
                return@let
            }

            val text = it.getStringExtra(Intent.EXTRA_TEXT)
            if (text?.isNotBlank() != true) {
                return@let
            }

            intent.removeExtra(Intent.EXTRA_TEXT)
            findNavController(R.id.nav_host_fragment)
                .navigate(
                    R.id.action_feedFragment_to_newPostFragment,
                    Bundle().apply {
                        textArg = text
                    }
                )
        }

        checkGoogleApiAvailability()

        val authViewModel by viewModels<AuthViewModel>()
        var currentMenuProvider: MenuProvider? = null
        authViewModel.authData.observe(this) {
            //currentMenuProvider?.also { ::removeMenuProvider }
            clearMenuProvider(currentMenuProvider)

            addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_main, menu)
                    val authorized = authViewModel.authorized
                    if (MenuState.getMenuState() == MenuStates.SHOW_STATE) {
                        menu.setGroupVisible(R.id.authenticated, authorized)
                        menu.setGroupVisible(R.id.unauthenticated, !authorized)
                    } else {
                        menu.setGroupVisible(R.id.authenticated, false)
                        menu.setGroupVisible(R.id.unauthenticated, false)
                    }

                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                     when (menuItem.itemId) {
                        R.id.signIn -> {
                            //AppAuth.getInstance().setAuth(5, "x-token")
                            //clearMenuProvider(currentMenuProvider)
                            findNavController(R.id.nav_host_fragment).navigate(R.id.action_feedFragment_to_authFragment)
                            true
                        }
                        R.id.signUp -> {
                            //AppAuth.getInstance().setAuth(5, "x-token")
                            findNavController(R.id.nav_host_fragment).navigate(R.id.action_feedFragment_to_registrationFragment)
                            true
                        }
                        R.id.logout -> {
                            if(MenuState.getMenuState() == MenuStates.SHOW_STATE) {
                                AppAuth.getInstance().removeAuth()
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
            }.apply {
                currentMenuProvider = this
            })
        }
    }

    private fun clearMenuProvider(currentMenuProvider: MenuProvider?) {
        currentMenuProvider?.let { removeMenuProvider(it) }
    }

    private fun checkGoogleApiAvailability() {
        with(GoogleApiAvailability.getInstance()) {
            val code = isGooglePlayServicesAvailable(this@AppActivity)
            if (code == ConnectionResult.SUCCESS) {
                return@with
            }
            if (isUserResolvableError(code)) {
                getErrorDialog(this@AppActivity, code, 9000)?.show()
                return
            }
            Toast.makeText(this@AppActivity, R.string.google_play_unavailable, Toast.LENGTH_LONG)
                .show()
        }

        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            println(it)
        }
    }
}
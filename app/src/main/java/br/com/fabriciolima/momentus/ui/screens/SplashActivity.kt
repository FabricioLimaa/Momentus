package br.com.fabriciolima.momentus.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import br.com.fabriciolima.momentus.ui.viewmodel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashActivity : ComponentActivity() {

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            when (viewModel.checkUserStatus()) {
                SplashViewModel.UserStatus.NOT_LOGGED_IN -> {
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                }
                SplashViewModel.UserStatus.TERMS_NOT_ACCEPTED -> {
                    startActivity(Intent(this@SplashActivity, TermsActivity::class.java))
                }
                SplashViewModel.UserStatus.LOGGED_IN -> {
                    startActivity(Intent(this@SplashActivity, CalendarActivity::class.java))
                }
            }
            finish()
        }
    }
}

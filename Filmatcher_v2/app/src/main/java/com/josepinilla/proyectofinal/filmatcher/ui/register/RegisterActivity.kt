package com.josepinilla.proyectofinal.filmatcher.ui.register

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.josepinilla.proyectofinal.filmatcher.R
import com.josepinilla.proyectofinal.filmatcher.WatchedMoviesApplication
import com.josepinilla.proyectofinal.filmatcher.data.RemoteDataSource
import com.josepinilla.proyectofinal.filmatcher.data.Repository
import com.josepinilla.proyectofinal.filmatcher.databinding.ActivityRegisterBinding
import com.josepinilla.proyectofinal.filmatcher.ui.login.LoginActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var repository: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = (application as WatchedMoviesApplication).db
        repository = Repository(RemoteDataSource(), db)

        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
    }

    private fun registerUser() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        binding.etUsername.error = null
        binding.etPassword.error = null
        binding.etConfirmPassword.error = null

        var hasError = false

        if (username.isEmpty()) {
            binding.etUsername.error = getString(R.string.txt_required_field)
            hasError = true
        }

        if (password.isEmpty()) {
            binding.etPassword.error = getString(R.string.txt_required_field)
            hasError = true
        }

        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.error = getString(R.string.txt_required_field)
            hasError = true
        }

        if (password.length < 6) {
            binding.etPassword.error = getString(R.string.txt_password_length)
            hasError = true
        }

        if (password != confirmPassword) {
            binding.etConfirmPassword.error = getString(R.string.txt_password_match)
            hasError = true
        }

        if (hasError) return

        lifecycleScope.launch {
            val success = repository.registerUser(username, password)

            if (success) {
                Toast.makeText(this@RegisterActivity, getString(R.string.txt_user_stored_success), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                finish()
            } else {
                MaterialAlertDialogBuilder(this@RegisterActivity)
                    .setTitle(getString(R.string.txt_alert_title))
                    .setMessage(getString(R.string.txt_username_exists))
                    .setPositiveButton(getString(R.string.txt_ok), null)
                    .show()
            }
        }
    }
}

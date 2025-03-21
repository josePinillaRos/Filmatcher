package com.josepinilla.proyectofinal.filmatcher.ui.login

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.josepinilla.proyectofinal.filmatcher.R
import com.josepinilla.proyectofinal.filmatcher.WatchedMoviesApplication
import com.josepinilla.proyectofinal.filmatcher.data.RemoteDataSource
import com.josepinilla.proyectofinal.filmatcher.data.Repository
import com.josepinilla.proyectofinal.filmatcher.databinding.ActivityLoginBinding
import com.josepinilla.proyectofinal.filmatcher.ui.register.RegisterActivity
import com.josepinilla.proyectofinal.filmatcher.ui.main.MainActivity
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * LoginActivity
 * Clase que representa la actividad de inicio de sesión
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var repository: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar el Repository
        val watchedMoviesDB = (application as WatchedMoviesApplication).db
        repository = Repository(RemoteDataSource(), watchedMoviesDB)

        // Verificar si el usuario ya está autenticado
        checkUserSession()

        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
    }

    private fun checkUserSession() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", null)

        if (username != null) {
            // Usuario ya autenticado, ir al MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun loginUser() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Limpiar errores previos
        binding.etUsername.error = null
        binding.etPassword.error = null

        var hasError = false

        if (username.isEmpty()) {
            binding.etUsername.error = getString(R.string.txt_required_field)
            hasError = true
        }
        if (password.isEmpty()) {
            binding.etPassword.error = getString(R.string.txt_required_field)
            hasError = true
        }
        if (hasError) return

        lifecycleScope.launch {
            try {
                val querySnapshot = repository.getUserByUsername(username)
                if (querySnapshot == null || querySnapshot.isEmpty) {
                    binding.etUsername.error = getString(R.string.txt_user_not_found)
                    return@launch
                }

                val userDoc = querySnapshot.documents[0]
                val storedPassword = userDoc.getString("password")

                if (storedPassword == hashPassword(password)) {
                    Toast.makeText(this@LoginActivity, getString(R.string.txt_login_success), Toast.LENGTH_SHORT).show()
                    saveUserSession(username)
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    binding.etPassword.error = getString(R.string.txt_wrong_password)
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, getString(R.string.txt_login_failed), Toast.LENGTH_SHORT).show()
                Log.d("FIREBASE_ERROR", "Error al obtener usuario: ${e.message}", e)
            }
        }
    }


    /**
     * hashPassword
     * Método que cifra una contraseña con SHA-256
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * saveUserSession
     * Método que guarda la sesión del usuario en SharedPreferences para mantenerlo autenticado
     * y poder acceder a la información del usuario en otras actividades
     */
    private fun saveUserSession(username: String) {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("username", username)
        editor.apply()
    }
}

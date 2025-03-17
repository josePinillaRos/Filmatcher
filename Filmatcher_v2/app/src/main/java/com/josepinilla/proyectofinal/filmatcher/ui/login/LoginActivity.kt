package com.josepinilla.proyectofinal.filmatcher.ui.login

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.josepinilla.proyectofinal.filmatcher.R
import com.josepinilla.proyectofinal.filmatcher.databinding.ActivityLoginBinding
import com.josepinilla.proyectofinal.filmatcher.ui.register.RegisterActivity
import com.josepinilla.proyectofinal.filmatcher.ui.main.MainActivity
import java.security.MessageDigest

/**
 * LoginActivity
 * Clase que representa la actividad de inicio de sesión
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.txt_empty_fields), Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userDoc = documents.documents[0]
                    val storedPassword = userDoc.getString("password")

                    if (storedPassword == hashPassword(password)) {
                        Toast.makeText(this, getString(R.string.txt_login_success), Toast.LENGTH_SHORT).show()
                        saveUserSession(username)
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, getString(R.string.txt_wrong_password), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, getString(R.string.txt_user_not_found), Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.txt_login_failed), Toast.LENGTH_SHORT).show()
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

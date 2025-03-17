package com.josepinilla.proyectofinal.filmatcher.ui.register

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.josepinilla.proyectofinal.filmatcher.R
import com.josepinilla.proyectofinal.filmatcher.databinding.ActivityRegisterBinding
import com.josepinilla.proyectofinal.filmatcher.ui.login.LoginActivity
import java.security.MessageDigest

/**
 * RegisterActivity
 * Clase que representa la actividad de registro de usuario
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, getString(R.string.txt_compulsory_fields), Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, getString(R.string.txt_password_length), Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, getString(R.string.txt_password_match), Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar si el nombre de usuario ya existe en firestore
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    Toast.makeText(this, getString(R.string.txt_username_exists), Toast.LENGTH_SHORT).show()
                } else {
                    // Guardar el usuario en Firestore con contraseña cifrada
                    saveUserToFirestore(username, password)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.txt_error_register), Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Guarda el usuario en Firestore con la contraseña cifrada
     */
    private fun saveUserToFirestore(username: String, password: String) {
        val hashedPassword = hashPassword(password)
        val user = hashMapOf(
            "username" to username,
            "password" to hashedPassword
        )

        db.collection("users")
            .add(user)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.txt_user_stored_failure), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.txt_error_register), Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Cifra la contraseña con SHA-256
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

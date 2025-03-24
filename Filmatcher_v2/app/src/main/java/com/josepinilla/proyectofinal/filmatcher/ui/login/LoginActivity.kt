package com.josepinilla.proyectofinal.filmatcher.ui.login

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.josepinilla.proyectofinal.filmatcher.R
import com.josepinilla.proyectofinal.filmatcher.WatchedMoviesApplication
import com.josepinilla.proyectofinal.filmatcher.data.RemoteDataSource
import com.josepinilla.proyectofinal.filmatcher.data.Repository
import com.josepinilla.proyectofinal.filmatcher.databinding.ActivityLoginBinding
import com.josepinilla.proyectofinal.filmatcher.ui.main.MainActivity
import com.josepinilla.proyectofinal.filmatcher.ui.register.RegisterActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * LoginActivity
 * Activity para el proceso de login.
 *
 * @autor Jose Pinilla
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar el Repository
        val watchedMoviesDB = (application as WatchedMoviesApplication).db
        val repository = Repository(RemoteDataSource(), watchedMoviesDB)

        // Crear el ViewModel
        val factory = LoginViewModelFactory(repository)
        loginViewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        // Observar cambios en el StateFlow loginState
        observeLoginState()

        // Verificar si el usuario ya está autenticado
        checkUserSession()

        // Configurar boton de login
        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        // Configurar boton de registro
        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Bloquear rotación de pantalla
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
    }

    /**
     * Observa los cambios en el StateFlow loginState.
     * Y actua en consecuencia.
     */
    private fun observeLoginState() {
        lifecycleScope.launch {
            loginViewModel.loginState.collectLatest { state ->
                when (state) { // En funcion del estado actual del login
                    is LoginState.NoProcess -> {
                        binding.progressBar.visibility = View.GONE
                    }
                    is LoginState.Loading -> {
                        // Progress bar que muestra que se está cargando
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is LoginState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@LoginActivity, getString(R.string.txt_login_success), Toast.LENGTH_SHORT).show()

                        // Guardamos la sesión
                        saveUserSession(state.username)

                        // Vamos al MainActivity
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                    is LoginState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        // Muestra error en UI
                        showError(state.msgResId)
                    }
                }
            }
        }
    }

    /**
     * Muestra un mensaje de error en el campo correspondiente.
     */
    private fun showError(msgResId: Int) {
        when(msgResId) {
            R.string.txt_user_not_found -> binding.etUsername.error = getString(msgResId)
            R.string.txt_wrong_password -> binding.etPassword.error = getString(msgResId)
            else -> Toast.makeText(this, getString(msgResId), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Inicia el proceso de login.
     * Obtiene los datos de los campos de texto y los envía al ViewModel.
     * Si hay errores en los campos, los muestra en la UI.
     */
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

        // Iniciamos la lógica de login en el ViewModel
        loginViewModel.loginUser(username, password)
    }

    /**
     * Verifica si el usuario ya está autenticado.
     * Si es así, lo redirige al MainActivity sin pasar por el login.
     */
    private fun checkUserSession() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", null)
        if (username != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    /**
     * Guarda la sesión del usuario en SharedPreferences. Para mantener la sesión activa.
     *
     * @param username Nombre de usuario.
     */
    private fun saveUserSession(username: String) {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("username", username)
        editor.apply()
    }
}

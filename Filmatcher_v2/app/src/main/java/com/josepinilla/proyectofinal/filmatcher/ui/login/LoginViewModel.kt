package com.josepinilla.proyectofinal.filmatcher.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.josepinilla.proyectofinal.filmatcher.R
import com.josepinilla.proyectofinal.filmatcher.data.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * LoginViewModel
 * Contiene la lógica de negocio y acceso a datos para el proceso de login.
 *
 * @param repository Repositorio de datos.
 *
 * @autor Jose Pinilla
 */
class LoginViewModel(private val repository: Repository) : ViewModel() {

    // estado del login
    private val _loginState = MutableStateFlow<LoginState>(LoginState.NoProcess) // Estado inicial
    val loginState: StateFlow<LoginState> = _loginState

    /**
     * Inicia el proceso de login.
     */
    fun loginUser(username: String, password: String) {
        // Se cambia el estado a Loading antes de iniciar
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                // Verificar si el usuario existe
                val querySnapshot = repository.getUserByUsername(username)
                if (querySnapshot == null || querySnapshot.isEmpty) {
                    // Cambiamos el estado a "Error" con un mensaje
                    _loginState.value = LoginState.Error(R.string.txt_user_not_found)
                    return@launch
                }

                // Extraer la contraseña encriptada
                val userDoc = querySnapshot.documents[0]
                val storedPassword = userDoc.getString("password")

                // Validar la contraseña
                if (storedPassword == hashPassword(password)) {
                    // Login exitoso
                    _loginState.value = LoginState.Success(username)
                } else {
                    // Contraseña incorrecta
                    _loginState.value = LoginState.Error(R.string.txt_wrong_password)
                }
            } catch (e: Exception) {
                // Excepción al consultar Firebase
                Log.d("FIREBASE_ERROR", "Error al obtener usuario: ${e.message}", e)
                _loginState.value = LoginState.Error(R.string.txt_login_failed)
            }
        }
    }

    // Cifra la contraseña con SHA-256
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * LoginState
 * Modela los distintos estados en los que puede encontrarse el proceso de login:
 * - NoProcess: Aún no se ha iniciado el proceso de login
 * - Loading: Se está validando el login
 * - Success: Login correcto, guarda el username
 * - Error: Ocurrió un error, con un mensaje (StringRes)
 */
sealed class LoginState {
    object NoProcess : LoginState()
    object Loading : LoginState()
    data class Success(val username: String) : LoginState()
    data class Error(val msgResId: Int) : LoginState()
}

class LoginViewModelFactory(
    private val repository: Repository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

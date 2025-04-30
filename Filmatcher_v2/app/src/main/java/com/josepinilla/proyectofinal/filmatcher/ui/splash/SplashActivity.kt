package com.josepinilla.proyectofinal.filmatcher.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.josepinilla.proyectofinal.filmatcher.R
import com.josepinilla.proyectofinal.filmatcher.ui.login.LoginActivity
import com.josepinilla.proyectofinal.filmatcher.ui.main.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SplashActivity
 * Clase que representa la pantalla de inicio de la aplicación.
 * Muestra el logo de la aplicación y el de la API durante un breve periodo de tiempo
 *
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica un tema sin ActionBar para que sea limpio
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // pausa para que se vea el logo durante 3 segundos
        lifecycleScope.launch {
            delay(3000)
            routeToNextScreen()
        }
    }

    /**
     * Redirige a la siguiente pantalla dependiendo de si el usuario está autenticado o no.
     * Si el usuario está autenticado, se redirige a MainActivity.
     * Si no, se redirige a LoginActivity.
     */
    private fun routeToNextScreen() {
        val next = if (isUserLogged())
            Intent(this, MainActivity::class.java)
        else
            Intent(this, LoginActivity::class.java)

        startActivity(next)
        finish()  // Cierra la actividad actual para que no se pueda volver atrás
    }

    /**
     * Verifica si el usuario está autenticado comprobando si hay un nombre de usuario almacenado en SharedPreferences.
     */
    private fun isUserLogged(): Boolean =
        //Mode_PRIVATE es el modo por defecto de SharedPreferences para que solo la app pueda
        // acceder a los datos almacenados
        getSharedPreferences("UserSession", MODE_PRIVATE)
            .contains("username")
}

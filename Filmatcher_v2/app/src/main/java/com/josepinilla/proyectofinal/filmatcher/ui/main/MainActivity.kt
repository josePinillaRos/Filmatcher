package com.josepinilla.proyectofinal.filmatcher.ui.main

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.josepinilla.proyectofinal.filmatcher.databinding.ActivityMainBinding
import com.josepinilla.proyectofinal.filmatcher.R
import com.josepinilla.proyectofinal.filmatcher.ui.login.LoginActivity
import com.josepinilla.proyectofinal.filmatcher.ui.matches.LikedFilmsActivity
import com.josepinilla.proyectofinal.filmatcher.ui.matches.MatchesActivity
import com.josepinilla.proyectofinal.filmatcher.ui.playmatch.PlayMatchActivity

/**
 * MainActivity
 * Clase que representa la actividad principal
 * Permite al usuario seleccionar un proveedor de streaming y buscar coincidencias con otro usuario
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Nombre de usuario actual almacenado en SharedPreferences
    private val currentUsername by lazy {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        sharedPreferences.getString("username", null) ?: "guest_user"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.subtitle = getString(R.string.txt_username, currentUsername)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR

        // Mapeo de proveedores con sus IDs
        val providerMap = mapOf(
            binding.btnNetflix to 8,
            binding.btnMovistar to 2241,
            binding.btnDisney to 337,
            binding.btnMax to 1899,
            binding.btnAmazon to 119,
            binding.btnAppletv to 350
        )

        // Configurar clics en los botones para ir a PlayMatchActivity en funcion del proveedor
        providerMap.forEach { (button, providerId) ->
            button.setOnClickListener {
                goToPlayMatch(providerId)
            }
        }

        // buscar coincidencias con otro usuario
        binding.btnSearchMatches.setOnClickListener {
            val otherUsername = binding.etSearchOtherUser.text.toString().trim()
            if (otherUsername.isEmpty()) {
                Toast.makeText(this, "Ingresa un nombre de usuario", Toast.LENGTH_SHORT).show()
            } else {
                searchMatches()
            }
        }
    }

    /**
     * goToPlayMatch
     * Navega a PlayMatchActivity con el ID del proveedor seleccionado
     * @param providerId ID del proveedor
     */
    private fun goToPlayMatch(providerId: Int) {
        val intent = Intent(this, PlayMatchActivity::class.java)
        intent.putExtra("EXTRA_PROVIDER_ID", providerId)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                Log.d("MainActivity", "Menú 'Cerrar sesión' seleccionado")
                logoutUser()
                true
            }
            R.id.action_likes -> {
                val intent = Intent(this, LikedFilmsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logoutUser() {
        Log.d("MainActivity", "Ejecutando logoutUser()")
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        Log.d("MainActivity", "SharedPreferences eliminadas")

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        Log.d("MainActivity", "Redirigiendo a LoginActivity")
        finish()
    }



    /**
     * buscarCoincidencias
     * Descarga las películas de currentUsername y de otherUsername,
     * y muestra un Toast con las coincidencias
     */
    private fun searchMatches() {
        val otherUserNAme = binding.etSearchOtherUser.text.toString().trim()
        if (otherUserNAme.isEmpty()) {
            Toast.makeText(this, "Ingresa un nombre de usuario", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, MatchesActivity::class.java)
        intent.putExtra("CURRENT_USER", currentUsername)
        intent.putExtra("OTHER_USER", otherUserNAme)
        startActivity(intent)
    }
}

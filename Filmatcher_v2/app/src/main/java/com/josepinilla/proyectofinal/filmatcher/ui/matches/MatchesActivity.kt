package com.josepinilla.proyectofinal.filmatcher.ui.matches

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.josepinilla.proyectofinal.filmatcher.WatchedMoviesApplication
import com.josepinilla.proyectofinal.filmatcher.adapters.MovieAdapter
import com.josepinilla.proyectofinal.filmatcher.data.RemoteDataSource
import com.josepinilla.proyectofinal.filmatcher.data.Repository
import com.josepinilla.proyectofinal.filmatcher.databinding.ActivityMatchesBinding
import com.josepinilla.proyectofinal.filmatcher.models.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * MatchesActivity
 * Clase que representa la actividad de coincidencias
 * Muestra las películas en común entre dos usuarios
 */
class MatchesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMatchesBinding

    private lateinit var repository: Repository

    private var currentUser: String? = null
    private var otherUser: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatchesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = "Películas en común"

        // Recuperar extras
        currentUser = intent.getStringExtra("CURRENT_USER")
        otherUser = intent.getStringExtra("OTHER_USER")

        if (currentUser.isNullOrEmpty() || otherUser.isNullOrEmpty()) {
            Toast.makeText(this, "Faltan datos de usuario", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Instanciar el repositorio
        val db = (application as WatchedMoviesApplication).db
        repository = Repository(RemoteDataSource(), db)

        // Configurar RecyclerView
        binding.rvMatches.layoutManager = LinearLayoutManager(this)
        binding.rvMatches.adapter = MovieAdapter(emptyList()) { movie ->
            showMovieInfoDialog(movie)
        }

        // Iniciar la búsqueda de coincidencias
        findMatches(currentUser!!, otherUser!!)
    }

    /**
     * findMatches
     * Método que busca las coincidencias entre dos usuarios
     * @param userA Nombre de usuario A
     * @param userB Nombre de usuario B
     * @return Lista de películas en común
     */
    private fun findMatches(userA: String, userB: String) {
        // Lanzar una corrutina para buscar coincidencias
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Se llama al metodo fetchCommonMatches del repositorio
                val commonResults = repository.fetchCommonMatches(userA, userB)
                // Actualizar el adaptador en el hilo principal
                runOnUiThread {
                    if (commonResults.isEmpty()) {
                        Toast.makeText(this@MatchesActivity, "Sin coincidencias", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MatchesActivity, "Coincidencias: ${commonResults.size}", Toast.LENGTH_SHORT).show()
                    }
                    updateAdapter(commonResults)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MatchesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * updateAdapter
     * Método que actualiza el adaptador del RecyclerView
     * @param results Lista de películas en común
     */
    private fun updateAdapter(results: List<Result>) {
        val newAdapter = MovieAdapter(results) { movie ->
            showMovieInfoDialog(movie)
        }
        binding.rvMatches.adapter = newAdapter
    }

    /**
     * showMovieInfoDialog
     * Método que muestra un diálogo con la información de una película
     * @param movie Película a mostrar
     */
    private fun showMovieInfoDialog(movie: Result) {
        val overviewText = movie.overview ?: "Sin sinopsis disponible"
        AlertDialog.Builder(this)
            .setTitle(movie.title ?: "Película")
            .setMessage(overviewText)
            .setPositiveButton("Cerrar", null)
            .show()
    }
}

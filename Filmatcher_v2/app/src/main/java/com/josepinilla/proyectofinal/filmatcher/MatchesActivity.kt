package com.josepinilla.proyectofinal.filmatcher

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.josepinilla.proyectofinal.filmatcher.adapters.MovieAdapter
import com.josepinilla.proyectofinal.filmatcher.databinding.ActivityMatchesBinding
import com.josepinilla.proyectofinal.filmatcher.models.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MatchesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var binding: ActivityMatchesBinding

    private var currentUser: String? = null
    private var otherUser: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatchesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Título de la pantalla
        title = "Películas en común"

        // Recuperar los extras de usuarios
        currentUser = intent.getStringExtra("CURRENT_USER")
        otherUser = intent.getStringExtra("OTHER_USER")

        if (currentUser.isNullOrEmpty() || otherUser.isNullOrEmpty()) {
            Toast.makeText(this, "Faltan datos de usuario", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Configurar RecyclerView con el adaptador y manejo de clics
        binding.rvMatches.layoutManager = LinearLayoutManager(this)
        binding.rvMatches.adapter = MovieAdapter(emptyList()) { movie ->
            showMovieInfoDialog(movie)
        }

        // Iniciar la búsqueda de coincidencias
        findMatches(currentUser!!, otherUser!!)
    }

    /**
     * findMatches
     * Obtiene las películas de ambos usuarios y busca coincidencias.
     */
    private fun findMatches(userA: String, userB: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener películas de userA
                val snapshotA = db.collection("user_movies")
                    .document(userA)
                    .collection("movies")
                    .get()
                    .await()
                val pelisA = snapshotA.documents.mapNotNull { doc ->
                    doc.getLong("movieId")?.toInt()
                }.toSet()

                // Obtener películas de userB
                val snapshotB = db.collection("user_movies")
                    .document(userB)
                    .collection("movies")
                    .get()
                    .await()

                // Filtrar películas en común
                val commonDocs = snapshotB.documents.filter { doc ->
                    val mid = doc.getLong("movieId")?.toInt()
                    mid != null && pelisA.contains(mid)
                }

                // Convertir a objetos Result con validaciones
                val commonResults = commonDocs.mapNotNull { doc ->
                    val mid = doc.getLong("movieId")?.toInt() ?: return@mapNotNull null
                    val title = doc.getString("title") ?: "Desconocido"
                    val posterPath = doc.getString("posterPath") ?: ""
                    val releaseDate = doc.getString("releaseDate") ?: "Desconocido"
                    val overview = doc.getString("overview") ?: "Sin sinopsis disponible"
                    val providerId = doc.getLong("providerId")?.toInt() ?: 0

                    // Firestore almacena números largos (Long), los convertimos correctamente a Int
                    val genreIds = (doc.get("genreIds") as? List<*>)?.mapNotNull { it as? Long }?.map { it.toInt() } ?: emptyList()

                    Result(
                        adult = null,
                        backdropPath = null,
                        genreIds = genreIds,
                        id = mid,
                        originalLanguage = null,
                        originalTitle = null,
                        overview = overview,
                        popularity = null,
                        posterPath = posterPath,
                        releaseDate = releaseDate,
                        title = title,
                        video = null,
                        voteAverage = null,
                        voteCount = null,
                        providerId = providerId
                    )
                }

                // Actualizar la UI en el hilo principal
                runOnUiThread {
                    if (commonResults.isEmpty()) {
                        Toast.makeText(this@MatchesActivity, "Sin coincidencias", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MatchesActivity, "Coincidencias encontradas: ${commonResults.size}", Toast.LENGTH_SHORT).show()
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
     * Actualiza la lista del adaptador y notifica cambios.
     */
    private fun updateAdapter(results: List<Result>) {
        val newAdapter = MovieAdapter(results) { movie ->
            showMovieInfoDialog(movie)
        }
        binding.rvMatches.adapter = newAdapter
    }

    /**
     * showMovieInfoDialog
     * Muestra un diálogo con la sinopsis de la película seleccionada.
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

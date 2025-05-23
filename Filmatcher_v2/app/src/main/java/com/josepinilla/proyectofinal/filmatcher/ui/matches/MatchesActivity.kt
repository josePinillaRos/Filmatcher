package com.josepinilla.proyectofinal.filmatcher.ui.matches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.josepinilla.proyectofinal.filmatcher.R
import com.josepinilla.proyectofinal.filmatcher.WatchedMoviesApplication
import com.josepinilla.proyectofinal.filmatcher.adapters.MovieAdapter
import com.josepinilla.proyectofinal.filmatcher.data.RemoteDataSource
import com.josepinilla.proyectofinal.filmatcher.data.Repository
import com.josepinilla.proyectofinal.filmatcher.databinding.ActivityMatchesBinding
import com.josepinilla.proyectofinal.filmatcher.models.Result
import com.josepinilla.proyectofinal.filmatcher.utils.getGenres
import com.josepinilla.proyectofinal.filmatcher.utils.providerMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

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

    // Provider seleccionado (0 = Todos)
    private var selectedProviderId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatchesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        findViewById<ImageButton>(R.id.btnBackToMain).setOnClickListener {
            finish() // Cierra esta actividad y vuelve a MainActivity
        }

        title = getString(R.string.txt_films_matches)

        // Recuperar extras
        currentUser = intent.getStringExtra("CURRENT_USER")
        otherUser = intent.getStringExtra("OTHER_USER")

        if (currentUser.isNullOrEmpty() || otherUser.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.txt_error_data_user), Toast.LENGTH_SHORT).show()
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
        findMatches(currentUser!!, otherUser!!, selectedProviderId)
    }

    /**
     * onCreateOptionsMenu
     * infla el menú de opciones
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_matches, menu)
        return true
    }

    // 2) Manejamos el clic en el ítem de filtrado
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                showProviderFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * showProviderFilterDialog
     * Muestra un diálogo para que el usuario elija la plataforma.
     */
    private fun showProviderFilterDialog() {
        val providerNames = providerMap.values.toTypedArray()
        val providerIds = providerMap.keys.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.txt_provider_filter))
            .setItems(providerNames) { dialog, which ->
                // 'which' es la posición en el array, no el ID real
                val chosenProviderId = providerIds[which]
                selectedProviderId = chosenProviderId

                // Una vez elegida la plataforma, recargamos matches
                findMatches(currentUser!!, otherUser!!, selectedProviderId)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.txt_cancel), null)
            .show()
    }

    /**
     * findMatches
     * Método que busca las coincidencias entre dos usuarios
     * @param userA Nombre de usuario A
     * @param userB Nombre de usuario B
     * @return Lista de películas en común
     */
    private fun findMatches(userA: String, userB: String, providerId: Int) {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener todas las coincidencias
                val commonResults = repository.fetchCommonMatches(userA, userB)

                // Filtrar según el providerId
                val filteredResults = if (providerId == 0) {
                    commonResults  // 0 = Todos
                } else {
                    commonResults.filter { it.providerId == providerId }
                }

                // Actualizo la UI
                runOnUiThread {
                    if (filteredResults.isEmpty()) {
                        Toast.makeText(this@MatchesActivity, getString(R.string.txt_no_matches), Toast.LENGTH_SHORT).show()
                    } else {
                        val matchesSize = getString(R.string.txt_matches_size, filteredResults.size)
                        Toast.makeText(
                            this@MatchesActivity,
                            matchesSize,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    updateAdapter(filteredResults)
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
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog, null)

        // Referencias a los elementos del layout
        val tvMovieTitle = dialogView.findViewById<TextView>(R.id.tv_movie_title)
        val tvOverviewInfo = dialogView.findViewById<TextView>(R.id.tv_overview_info)
        val tvReleaseDateInfo = dialogView.findViewById<TextView>(R.id.tv_release_date_info)
        val tvGenreInfo = dialogView.findViewById<TextView>(R.id.tv_genre_info)

        // Asignación de valores
        tvMovieTitle.text = movie.title ?: getString(R.string.txt_title)
        tvOverviewInfo.text = if (movie.overview.isNullOrBlank()) getString(R.string.txt_no_sinopsis) else movie.overview
        tvReleaseDateInfo.text = formatReleaseDate(movie.releaseDate)
        tvGenreInfo.text = getGenres(this@MatchesActivity,movie.genreIds)

        // Construcción del Material AlertDialog con bordes redondeados
        val dialog = MaterialAlertDialogBuilder(this, R.style.RoundedMaterialDialog)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.txt_close), null)
            .create()

        dialog.show()
    }

    /**
     * formatReleaseDate
     * Método que formatea la fecha de lanzamiento para que se muestre en el formato dd-MM-yyyy
     */
    private fun formatReleaseDate(dateString: String?): String {
        if (dateString.isNullOrBlank()) {
            return getString(R.string.txt_year_unknown)
        }
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: return getString(R.string.txt_year_unknown))
        } catch (e: Exception) {
            getString(R.string.txt_year_unknown)
        }
    }
}

package com.josepinilla.proyectofinal.filmatcher

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.josepinilla.proyectofinal.filmatcher.data.*
import com.josepinilla.proyectofinal.filmatcher.databinding.ActivityPlayMatchBinding
import com.josepinilla.proyectofinal.filmatcher.models.Result
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class PlayMatchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayMatchBinding

    // Firestore
    private val db = FirebaseFirestore.getInstance()

    // Base de datos local (Room)
    private lateinit var watchedMoviesDB: WatchedMoviesRoomDB

    // Repositorio con acceso a API remota y base de datos local
    private lateinit var repository: Repository

    // Obtener username de SharedPreferences
    private val sharedPrefs by lazy {
        getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    }
    private val username by lazy {
        sharedPrefs.getString("username", "guest_user") ?: "guest_user"
    }

    // Lista local
    private var moviesList = mutableListOf<Result>()
    private var currentMovieIndex = 0

    // Paginación
    private var page = 1
    private var totalPages = 1
    private var isLoading = false
    private var providerId: Int = 1899

    // Variables para el drag
    private var initialX = 0f
    private var initialY = 0f
    private var downX = 0f
    private var downY = 0f

    // Umbrales
    private val swipeThreshold = 350f
    private val swipeDuration = 200L
    private val tapThreshold = 10f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ⚠️ Mover la inicialización aquí dentro de onCreate()
        watchedMoviesDB = (application as WatchedMoviesApplication).db
        repository = Repository(RemoteDataSource(), watchedMoviesDB)

        providerId = intent.getIntExtra("EXTRA_PROVIDER_ID", 1899)

        lifecycleScope.launch {
            try {
                totalPages = repository.getTotalPages(providerId).totalPages ?: 1
                Toast.makeText(this@PlayMatchActivity, "Total de páginas: $totalPages", Toast.LENGTH_SHORT).show()
            }
            catch (e: Exception) {
                Toast.makeText(
                    this@PlayMatchActivity,
                    "Error al obtener el total de páginas: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        fetchPage(1)

        setupButtons()
        setupSwipeCard()
    }

    private fun fetchPage(pageToLoad: Int, showNextImmediately: Boolean = false) {
        // Evitamos que se hagan múltiples llamadas simultáneas
        if (isLoading) return
        isLoading = true

        lifecycleScope.launch {
            try {
                val newMovies = repository.fetchMovies(providerId, pageToLoad)

                // Si newMovies está vacío, significa que esa página no tiene películas nuevas que mostrar
                if (newMovies.isEmpty()) {
                    // Si aún no hemos superado el total de páginas, intentamos la siguiente
                    if (pageToLoad < totalPages) {
                        isLoading = false  // liberamos el flag para que la siguiente llamada pueda ocurrir
                        fetchPage(pageToLoad + 1, showNextImmediately)
                    } else {
                        // Hemos agotado todas las páginas disponibles
                        isLoading = false
                        Toast.makeText(
                            this@PlayMatchActivity,
                            "No hay más películas disponibles",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // Aquí tenemos películas nuevas que no están en la BBDD local
                    val oldSize = moviesList.size
                    moviesList.addAll(newMovies)

                    // Ajustar totalPages si tu API lo proporciona. Por ahora,
                    // mantenemos tu lógica de establecer totalPages = pageToLoad.
                    // Idealmente, deberías ponerlo a la cantidad de páginas totales de la respuesta del servidor.
                    //totalPages = pageToLoad
                    page = pageToLoad

                    // Si es la primera vez que llenamos moviesList, o si pedimos mostrar de inmediato
                    if (oldSize == 0) {
                        currentMovieIndex = 0
                        showMovie(moviesList[currentMovieIndex])
                    } else if (showNextImmediately) {
                        currentMovieIndex = oldSize
                        showMovie(moviesList[currentMovieIndex])
                    }

                    // Liberamos el flag de carga
                    isLoading = false
                }

            } catch (e: Exception) {
                isLoading = false
                Toast.makeText(
                    this@PlayMatchActivity,
                    "Error al obtener películas: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun showMovie(movie: Result) {
        val card = binding.includeItemFilm.root
        card.translationX = 0f
        card.translationY = 0f

        binding.includeItemFilm.tvTitle.text = movie.title ?: "Sin título"
        binding.includeItemFilm.tvYear.text = "Año: ${movie.releaseDate?.take(4) ?: "Desconocido"}"
        binding.includeItemFilm.tvGenere.text = "Géneros: ${getGenres(movie.genreIds)}"

        Glide.with(this)
            .load("https://image.tmdb.org/t/p/w500${movie.posterPath}")
            .into(binding.includeItemFilm.ivImage)

        val providerImage = providerLogos[providerId] ?: R.drawable.netflix
        Glide.with(this)
            .load(providerImage)
            .into(binding.includeItemFilm.ivProvider)
    }

    private fun saveWatchedMovie(movie: Result) {
        lifecycleScope.launch {
            repository.insertWatchedMovie(movie)
        }
    }

    private fun getGenres(genreIds: List<Int?>?): String {
        val genreMap = mapOf(
            28 to "Acción",
            12 to "Aventura",
            16 to "Animación",
            35 to "Comedia",
            80 to "Crimen",
            99 to "Documental",
            18 to "Drama",
            10751 to "Familia",
            14 to "Fantasía",
            36 to "Historia",
            27 to "Terror",
            10402 to "Música",
            9648 to "Misterio",
            10749 to "Romance",
            878 to "Ciencia ficción",
            10770 to "Película de TV",
            53 to "Suspense",
            10752 to "Bélica",
            37 to "Oeste"
        )

        return genreIds?.mapNotNull { genreMap[it ?: 0] }?.joinToString(", ") ?: "Desconocido"
    }

    private fun setupButtons() {
        binding.btnAccept.setOnClickListener {
            val movie = moviesList.getOrNull(currentMovieIndex)
            if (movie != null) {
                saveAcceptedMovie(movie)
                movie.providerId = providerId
                saveWatchedMovie(movie) // Guardar en base de datos local
            }
            nextMovie()
        }
        binding.btnReject.setOnClickListener {
            val movie = moviesList.getOrNull(currentMovieIndex)
            if (movie != null) {
                movie.providerId = providerId
                saveWatchedMovie(movie) // Guardar en base de datos local si es rechazada
            }
            nextMovie()
        }
    }

    private fun nextMovie() {
        currentMovieIndex++

        if (currentMovieIndex < moviesList.size) {
            showMovie(moviesList[currentMovieIndex])
        } else {
            // Si llegamos al final de la lista, verificamos si aún hay páginas disponibles
            if (page < totalPages) {
                fetchPage(page + 1, showNextImmediately = true)
            } else {
                Toast.makeText(this, "No hay más películas disponibles", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun setupSwipeCard() {
        val card = binding.includeItemFilm.root
        card.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = card.translationX
                    initialY = card.translationY
                    downX = event.rawX
                    downY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    card.translationX = initialX + dx
                    card.translationY = initialY + dy
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val finalX = card.translationX
                    val distance = abs(finalX)

                    val totalDx = abs(event.rawX - downX)
                    val totalDy = abs(event.rawY - downY)

                    if (totalDx < tapThreshold && totalDy < tapThreshold) {
                        v.performClick()
                        showMovieInfoDialog()
                        return@setOnTouchListener true
                    }

                    if (distance > swipeThreshold) {
                        val direction = if (finalX > 0) 1 else -1
                        animateCardOut(direction)
                    } else {
                        card.animate()
                            .translationX(0f)
                            .translationY(0f)
                            .setDuration(swipeDuration)
                            .start()
                    }
                }
            }
            true
        }
    }

    private fun showMovieInfoDialog() {
        val movie = moviesList.getOrNull(currentMovieIndex) ?: return

        val overviewText = movie.overview ?: "Sin sinopsis disponible"

        AlertDialog.Builder(this)
            .setTitle(movie.title ?: "Película")
            .setMessage("Sinopsis: $overviewText")
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun animateCardOut(direction: Int) {
        val card = binding.includeItemFilm.root
        val exitX = direction * 2000f

        card.animate()
            .translationX(exitX)
            .setDuration(swipeDuration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    card.translationX = 0f
                    card.translationY = 0f

                    val movie = moviesList.getOrNull(currentMovieIndex)
                    if (movie != null) {
                        movie.providerId = providerId
                        saveWatchedMovie(movie) // Guardar en la base de datos local si se mueve la tarjeta

                        if (direction == 1) { // Si se desliza a la derecha (aceptada)
                            saveAcceptedMovie(movie)
                        }
                    }

                    nextMovie()
                }
            })
            .start()
    }

    private fun saveAcceptedMovie(movie: Result) {
        lifecycleScope.launch {
            try {
                repository.saveAcceptedMovie(username, movie, providerId)
                Toast.makeText(this@PlayMatchActivity, "Película '${movie.title}' guardada", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@PlayMatchActivity, "Error al guardar '${movie.title}'", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val providerLogos = mapOf(
        8 to R.drawable.netflix,
        2241 to R.drawable.movistar,
        337 to R.drawable.disney,
        1899 to R.drawable.max,
        119 to R.drawable.amazon
    )
}

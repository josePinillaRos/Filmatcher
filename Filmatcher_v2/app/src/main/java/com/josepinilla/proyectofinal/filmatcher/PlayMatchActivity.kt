package com.josepinilla.proyectofinal.filmatcher

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.josepinilla.proyectofinal.filmatcher.data.MoviesAPI
import com.josepinilla.proyectofinal.filmatcher.data.RemoteDataSource
import com.josepinilla.proyectofinal.filmatcher.data.Repository
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

    private val repository by lazy { Repository(RemoteDataSource()) }


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



        providerId = intent.getIntExtra("EXTRA_PROVIDER_ID", 1899)

        fetchPage(1)

        setupButtons()
        setupSwipeCard()
    }

    private fun fetchPage(pageToLoad: Int, showNextImmediately: Boolean = false) {
        if (isLoading) return
        isLoading = true

        val api = MoviesAPI.getRetrofit2Api()
        lifecycleScope.launch {
            try {
                val response = api.getMoviesByProvider(
                    watchProvider = providerId,
                    page = pageToLoad
                )
                totalPages = response.totalPages ?: totalPages

                val newMovies = response.results?.filterNotNull() ?: emptyList()

                val oldSize = moviesList.size
                moviesList.addAll(newMovies)

                page = pageToLoad

                if (pageToLoad == 1 && oldSize == 0 && moviesList.isNotEmpty()) {
                    currentMovieIndex = 0
                    showMovie(moviesList[currentMovieIndex])
                }

                if (showNextImmediately && newMovies.isNotEmpty()) {
                    currentMovieIndex = oldSize
                    showMovie(moviesList[currentMovieIndex])
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@PlayMatchActivity,
                    "Error al obtener películas: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isLoading = false
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

    /**
     * Mapea IDs de géneros a texto
     */
    private fun getGenres(genreIds: List<Int?>?): String {
        val genreMap = mapOf(
            28 to "Acción",
            12 to "Aventura",
            16 to "Animación",
            35 to "Comedia",
            80 to "Crimen",
            99 to "Documental",
            18 to "Drama",
            10751 to "Familiar",
            14 to "Fantasía",
            36 to "Historia",
            27 to "Terror",
            10402 to "Música",
            9648 to "Misterio",
            10749 to "Romance",
            878 to "Ciencia Ficción",
            10770 to "Película de TV",
            53 to "Suspenso",
            10752 to "Bélica",
            37 to "Western"
        )
        return genreIds?.mapNotNull { genreMap[it] }?.joinToString(", ") ?: "Desconocido"
    }
    private fun setupButtons() {
        binding.btnAccept.setOnClickListener {
            val movie = moviesList.getOrNull(currentMovieIndex)
            if (movie != null) {
                saveAcceptedMovie(movie)
            }
            nextMovie()
        }
        binding.btnReject.setOnClickListener {
            nextMovie()
        }
    }

    private fun nextMovie() {
        currentMovieIndex++
        if (currentMovieIndex < moviesList.size) {
            showMovie(moviesList[currentMovieIndex])
        } else {
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

    /**
     * Muestra un dialog con la sinopsis (overview) de la película actual.
     */
    private fun showMovieInfoDialog() {
        // Verifica que tengamos una película válida
        if (moviesList.isEmpty() || currentMovieIndex >= moviesList.size) return

        val movie = moviesList[currentMovieIndex]
        val overviewText = movie.overview ?: "Sin sinopsis disponible"

        AlertDialog.Builder(this)
            .setTitle(movie.title ?: "Película")
            .setMessage(overviewText)
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

                    if (direction == 1 && currentMovieIndex < moviesList.size) {
                        val movie = moviesList[currentMovieIndex]
                        saveAcceptedMovie(movie)
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



    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private val providerLogos = mapOf(
        8 to R.drawable.netflix,
        2241 to R.drawable.movistar,
        337 to R.drawable.disney,
        1899 to R.drawable.max,
        119 to R.drawable.amazon
    )

}

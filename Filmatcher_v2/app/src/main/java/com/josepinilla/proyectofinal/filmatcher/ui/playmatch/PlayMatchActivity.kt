package com.josepinilla.proyectofinal.filmatcher.ui.playmatch

import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.josepinilla.proyectofinal.filmatcher.R
import com.josepinilla.proyectofinal.filmatcher.data.RemoteDataSource
import com.josepinilla.proyectofinal.filmatcher.data.Repository
import com.josepinilla.proyectofinal.filmatcher.data.RepositoryFirebase
import com.josepinilla.proyectofinal.filmatcher.WatchedMoviesApplication
import com.josepinilla.proyectofinal.filmatcher.databinding.ActivityPlayMatchBinding
import com.josepinilla.proyectofinal.filmatcher.models.Result
import com.josepinilla.proyectofinal.filmatcher.utils.getGenres
import com.josepinilla.proyectofinal.filmatcher.utils.providerLogos
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * PlayMatchActivity
 * Clase que maneja la interfaz de "swipe" de películas y usa el PlayMatchViewModel.
 * Muestra una película a la vez y permite aceptar o rechazar.
 * También permite filtrar por género.
 */
class PlayMatchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayMatchBinding
    private lateinit var viewModel: PlayMatchViewModel

    // Para manejar el arrastre / swipe
    private var initialX = 0f
    private var initialY = 0f
    private var downX = 0f
    private var downY = 0f

    // Umbrales
    private val swipeDuration = 200L
    private val tapThreshold = 10f

    // Username de SharedPreferences
    private val sharedPrefs by lazy {
        getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    }

    private val username by lazy {
        sharedPrefs.getString("username", "guest_user") ?: "guest_user"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtenemos el providerId que llega por Intent
        val providerId = intent.getIntExtra("EXTRA_PROVIDER_ID", 1899)

        // Inicializo el Repository
        val watchedMoviesDB = (application as WatchedMoviesApplication).db
        val repository = Repository(RemoteDataSource(), watchedMoviesDB)
        val firestore = RepositoryFirebase()

        // Instancio el ViewModel
        val factory = PlayMatchViewModelFactory(repository, username, providerId)
        viewModel = ViewModelProvider(this, factory)[PlayMatchViewModel::class.java]

        // Configurar el Spinner de géneros
        setupGenreFilterSpinner()

        // Configurar los botones de Aceptar/Rechazar
        setupButtons()

        // Configurar swipe manual
        setupSwipeCard()

        // Observar la película actual y mostrarla en pantalla
        observeCurrentMovie()

        // Cargar el total de páginas y la primera página
        initData()
    }

    /**
     * Llama al ViewModel para obtener el total de páginas y cargar la página 1.
     */
    private fun initData() {
        // Cargar el total de páginas
        viewModel.loadTotalPages(
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            },
            onSuccess = { total ->
                Toast.makeText(this, "Total de páginas: $total", Toast.LENGTH_SHORT).show()
            }
        )
        // Cargar la primera página
        viewModel.fetchPage(
            pageToLoad = 1,
            showNextImmediately = false,
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    /**
     * Observa el StateFlow del ViewModel y muestra la película actual cuando cambia.
     */
    private fun observeCurrentMovie() {
        lifecycleScope.launch {
            viewModel.currentMovie.collectLatest { movie ->
                movie?.let {
                    showMovie(it)
                }
            }
        }
    }

    /**
     * Muestra la película en pantalla.
     */
    private fun showMovie(movie: Result) {
        // Reiniciar posición de la tarjeta para el swipe
        val card = binding.includeItemFilm.root
        card.translationX = 0f
        card.translationY = 0f

        binding.includeItemFilm.tvTitle.text = movie.title ?: "Sin título"
        binding.includeItemFilm.tvYear.text =
            "Año: ${movie.releaseDate?.take(4) ?: "Desconocido"}"
        binding.includeItemFilm.tvGenere.text =
            "Géneros: ${getGenres(movie.genreIds)}"

        // Cargar poster con Glide
        Glide.with(this)
            .load("https://image.tmdb.org/t/p/w500${movie.posterPath}")
            .into(binding.includeItemFilm.ivImage)

        // Logotipo del proveedor
        val providerImage = providerLogos[movie.providerId] ?: R.drawable.netflix
        Glide.with(this)
            .load(providerImage)
            .into(binding.includeItemFilm.ivProvider)
    }

    /**
     * Configurar botones Aceptar / Rechazar
     */
    private fun setupButtons() {
        binding.btnAccept.setOnClickListener {
            val current = viewModel.currentMovie.value ?: return@setOnClickListener
            current.providerId = intent.getIntExtra("EXTRA_PROVIDER_ID", 1899)

            // Guarda en BD local y Firestore
            viewModel.saveWatchedMovie(current)
            viewModel.saveAcceptedMovie(
                current,
                onSuccess = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() },
                onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
            )
            viewModel.nextMovie {
                Toast.makeText(this, "No hay más películas disponibles", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnReject.setOnClickListener {
            val current = viewModel.currentMovie.value ?: return@setOnClickListener
            current.providerId = intent.getIntExtra("EXTRA_PROVIDER_ID", 1899)

            // Solo guarda en BD local y avanza
            viewModel.saveWatchedMovie(current)
            viewModel.nextMovie {
                Toast.makeText(this, "No hay más películas disponibles", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Spinner para filtrar por género.
     */
    private fun setupGenreFilterSpinner() {
        val genreMap = mapOf(
            0 to "Todos",
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

        val genreNames = genreMap.values.toList()
        val genreIds = genreMap.keys.toList()

        // Adaptador para el Spinner
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            genreNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerFilter.adapter = adapter

        // Listener para el Spinner
        binding.spinnerFilter.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedGenre = genreIds[position]
                // Informamos al ViewModel que cambió el género
                viewModel.setSelectedGenre(selectedGenre)

                // Volver a cargar la página 1 con el nuevo filtro
                viewModel.fetchPage(
                    pageToLoad = 1,
                    showNextImmediately = false,
                    onError = { message ->
                        Toast.makeText(this@PlayMatchActivity, message, Toast.LENGTH_SHORT).show()
                    }
                )
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    /**
     * Configurar swipe manual en la card.
     */
    //TODO: Arreglar los warnings
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
                    val distanceX = abs(finalX)

                    val totalDx = abs(event.rawX - downX)
                    val totalDy = abs(event.rawY - downY)

                    // Detectar tap
                    if (totalDx < tapThreshold && totalDy < tapThreshold) {
                        v.performClick()
                        showMovieInfoDialog()
                        return@setOnTouchListener true
                    }

                    // Detectar swipe
                    if (viewModel.isSwipe(distanceX)) {
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
     * Muestra diálogo con la sinopsis.
     */
    private fun showMovieInfoDialog() {
        val movie = viewModel.currentMovie.value ?: return
        val overviewText = movie.overview ?: "Sin sinopsis disponible"

        AlertDialog.Builder(this)
            .setTitle(movie.title ?: "Película")
            .setMessage("Sinopsis: $overviewText")
            .setPositiveButton("Cerrar", null)
            .show()
    }

    /**
     * Animación al arrastrar la tarjeta fuera de pantalla.
     */
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

                    val current = viewModel.currentMovie.value
                    if (current != null) {
                        // Marcamos en BD local
                        current.providerId = intent.getIntExtra("EXTRA_PROVIDER_ID", 1899)
                        viewModel.saveWatchedMovie(current)

                        // Swipe derecha => aceptada
                        if (direction == 1) {
                            viewModel.saveAcceptedMovie(
                                current,
                                onSuccess = { msg -> Toast.makeText(this@PlayMatchActivity, msg, Toast.LENGTH_SHORT).show() },
                                onError = { msg -> Toast.makeText(this@PlayMatchActivity, msg, Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }

                    viewModel.nextMovie {
                        Toast.makeText(this@PlayMatchActivity, "No hay más películas disponibles", Toast.LENGTH_SHORT).show()
                    }
                }
            })
            .start()
    }
}

package com.josepinilla.proyectofinal.filmatcher.ui.playmatch

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.josepinilla.proyectofinal.filmatcher.R
import com.josepinilla.proyectofinal.filmatcher.WatchedMoviesApplication
import com.josepinilla.proyectofinal.filmatcher.data.RemoteDataSource
import com.josepinilla.proyectofinal.filmatcher.data.Repository
import com.josepinilla.proyectofinal.filmatcher.databinding.ActivityPlayMatchBinding
import com.josepinilla.proyectofinal.filmatcher.models.Result
import com.josepinilla.proyectofinal.filmatcher.utils.getGenres
import com.josepinilla.proyectofinal.filmatcher.utils.providerLogos
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * PlayMatchActivity
 * Muestra una película a la vez y permite aceptar o rechazar.
 * También permite filtrar por género.
 */
class PlayMatchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayMatchBinding
    private lateinit var viewModel: PlayMatchViewModel

    // Para manejar el arrastre / swipe
    private var downX = 0f
    private var downY = 0f

    // Umbrales
    private val swipeDuration = 200L
    private val tapThreshold = 20f  // un poco más alto que 10f para evitar falsos taps

    // Username de SharedPreferences
    private val sharedPrefs by lazy {
        getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    }

    private val username by lazy {
        sharedPrefs.getString("username", "guest_user") ?: "guest_user"
    }

    private var providerId: Int = 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViewById<ImageButton>(R.id.btnBackToMain).setOnClickListener {
            finish() // Cierra esta actividad y vuelve a MainActivity
        }

        // Obtenemos el providerId que llega por Intent
        providerId = intent.getIntExtra("EXTRA_PROVIDER_ID", 1899)

        // Inicializo el Repository
        val watchedMoviesDB = (application as WatchedMoviesApplication).db
        val repository = Repository(RemoteDataSource(), watchedMoviesDB)

        // Instancio el ViewModel
        val factory = PlayMatchViewModelFactory(repository, username, providerId)
        viewModel = ViewModelProvider(this, factory)[PlayMatchViewModel::class.java]

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR

        //Configurar el SwipeRefreshLayout
        setupSwipeRefreshLayout()

        // Configurar la toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.subtitle = getString(R.string.txt_username, username)

        // Configurar botones Aceptar/Rechazar
        setupButtons()

        // Configurar swipe manual
        setupSwipeCard()

        // Observar la película actual y mostrarla en pantalla
        observeCurrentMovie()

        // Cargar el total de páginas y la primera página
        initData()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_play_match, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                showGenreFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupSwipeRefreshLayout() {
        binding.swipeRefresh.setOnRefreshListener {
            showResetDialog()
        }
    }

    private fun showResetDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reiniciar películas")
            .setMessage("¿Estás seguro de que quieres reiniciar?")
            .setPositiveButton("Sí") { _, _ ->
                resetMovies()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                binding.swipeRefresh.isRefreshing = false
            }
            .show()
    }

    private fun resetMovies() {
        lifecycleScope.launch {
            viewModel.resetMoviesByUser()
            binding.swipeRefresh.isRefreshing = false
            Toast.makeText(this@PlayMatchActivity, "Películas reiniciadas", Toast.LENGTH_SHORT).show()

            // Cargar la primera página
            viewModel.fetchPage(
                pageToLoad = 1,
                showNextImmediately = true,
                onError = { message ->
                    Toast.makeText(this@PlayMatchActivity, message, Toast.LENGTH_SHORT).show()
                }
            )
        }
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
        val providerImage = providerLogos[providerId] ?: R.drawable.netflix
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
            current.userName = username

            // Guarda en BD local y Firestore
            viewModel.saveWatchedMovie(current)
            viewModel.saveAcceptedMovie(
                current,
                onSuccess = { msg -> showShorterToast(msg) },
                onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
            )
            viewModel.nextMovie {
                Toast.makeText(this, "No hay más películas disponibles", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnReject.setOnClickListener {
            val current = viewModel.currentMovie.value ?: return@setOnClickListener
            current.providerId = intent.getIntExtra("EXTRA_PROVIDER_ID", 1899)
            current.userName = username

            // guarda en BD local y elimina de Firestore
            viewModel.saveWatchedMovie(current)
            viewModel.deleteMovie(
                current,
                onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
            )
            viewModel.nextMovie {
                Toast.makeText(this, "No hay más películas disponibles", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showShorterToast(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.show()

        // Cancela el toast después de 1 segundo (1000 ms)
        android.os.Handler().postDelayed({
            toast.cancel()
        }, 500)
    }

    /**
     * Spinner para filtrar por género.
     */
    private fun showGenreFilterDialog() {
        // Esto es lo que había en setupGenreFilterSpinner(), para la lista de géneros:
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

        // Mostramos un diálogo con las opciones
        AlertDialog.Builder(this)
            .setTitle("Elige un género")
            .setItems(genreNames.toTypedArray()) { dialog, which ->
                // Al hacer clic en un género
                val selectedGenreId = genreIds[which]
                viewModel.setSelectedGenre(selectedGenreId)

                // Recargar la página 1 con el nuevo género
                viewModel.fetchPage(
                    pageToLoad = 1,
                    showNextImmediately = false,
                    onError = { message ->
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                )
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Configurar swipe manual en la card.
     */
    private fun setupSwipeCard() {
        val card = binding.includeItemFilm.root
        card.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Desactiva el SwipeRefresh para no interferir
                    binding.swipeRefresh.isEnabled = false
                    // Pide al padre que no intercepte el gesto
                    card.parent.requestDisallowInterceptTouchEvent(true)

                    // Guarda las coordenadas donde el usuario pone el dedo
                    downX = event.rawX
                    downY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    card.parent.requestDisallowInterceptTouchEvent(true)

                    // Desplazamiento relativo al punto donde tocó DOWN
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY

                    // La tarjeta se mueve “en directo”
                    card.translationX = dx
                    card.translationY = dy
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Permite al padre interceptar de nuevo
                    card.parent.requestDisallowInterceptTouchEvent(false)
                    binding.swipeRefresh.isEnabled = true

                    // Calcula el arrastre final
                    val dx = card.translationX
                    val dy = card.translationY

                    // Detectar “tap” (cuando se mueve muy poco)
                    if (abs(dx) < tapThreshold && abs(dy) < tapThreshold) {
                        v.performClick()
                        showMovieInfoDialog()
                        return@setOnTouchListener true
                    }

                    // Si el arrastre supera cierto umbral => swipe out
                    val swipeThreshold = card.width * 0.5f
                    if (abs(dx) > swipeThreshold) {
                        val direction = if (dx > 0) 1 else -1
                        animateCardOut(direction)
                    } else {
                        // Vuelve la tarjeta al centro
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
        val overviewText = if (movie.overview.isNullOrBlank()) "Sin sinopsis disponible" else movie.overview

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
        val exitX = if (direction > 0) 2000f else -2000f

        // Animación de salida
        card.animate()
            .translationX(exitX)
            .setDuration(swipeDuration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 1) Resetea la tarjeta al centro para la próxima película
                    card.translationX = 0f
                    card.translationY = 0f

                    // 2) Manejamos la lógica de Aceptar/Rechazar
                    val current = viewModel.currentMovie.value ?: return
                    current.providerId = providerId
                    current.userName = username

                    if (direction > 0) {
                        // Swipe derecha => Aceptar
                        viewModel.saveWatchedMovie(current)
                        viewModel.saveAcceptedMovie(
                            current,
                            onSuccess = {msg -> showShorterToast(msg)
                            },
                            onError = { msg ->
                                Toast.makeText(this@PlayMatchActivity, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        // Swipe izquierda => Rechazar
                        viewModel.saveWatchedMovie(current)
                        viewModel.deleteMovie(
                            current,
                            onError = { msg ->
                                Toast.makeText(this@PlayMatchActivity, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // 3) Pasamos a la siguiente película
                    viewModel.nextMovie {
                        Toast.makeText(
                            this@PlayMatchActivity,
                            "No hay más películas disponibles",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
            .start()
    }
}

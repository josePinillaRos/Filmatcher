package com.josepinilla.proyectofinal.filmatcher.ui.playmatch

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.josepinilla.proyectofinal.filmatcher.R
import com.josepinilla.proyectofinal.filmatcher.WatchedMoviesApplication
import com.josepinilla.proyectofinal.filmatcher.adapters.MovieAdapter
import com.josepinilla.proyectofinal.filmatcher.data.RemoteDataSource
import com.josepinilla.proyectofinal.filmatcher.data.Repository
import com.josepinilla.proyectofinal.filmatcher.databinding.ActivityPlayMatchBinding
import com.josepinilla.proyectofinal.filmatcher.models.Result
import com.josepinilla.proyectofinal.filmatcher.utils.getGenres
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.text.SimpleDateFormat
import java.util.Locale

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
            .setTitle(getString(R.string.txt_reset_movies))
            .setMessage(getString(R.string.txt_ask_reset_movies))
            .setPositiveButton(getString(R.string.txt_yes)) { _, _ ->
                resetMovies()
            }
            .setNegativeButton(getString(R.string.txt_no)) { dialog, _ ->
                dialog.dismiss()
                binding.swipeRefresh.isRefreshing = false
            }
            .show()
    }

    private fun resetMovies() {
        lifecycleScope.launch {
            viewModel.resetMoviesByUser()
            binding.swipeRefresh.isRefreshing = false
            Toast.makeText(this@PlayMatchActivity, getString(R.string.txt_films_reseted), Toast.LENGTH_SHORT).show()

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
                val totalPages = getString(R.string.txt_pages_size, total)
                Toast.makeText(this, totalPages, Toast.LENGTH_SHORT).show()
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

        // Asignamos el providerId al objeto movie para que el adapter muestre el logo correcto
        movie.providerId = providerId

        // Usamos el adapter con una sola película
        val singleMovieAdapter = MovieAdapter(listOf(movie)) {
            // Acción al hacer clic en la película
            showMovieInfoDialog()
        }

        // Creamos el ViewHolder usando la vista incluida (item_film)
        val viewHolder = MovieAdapter.MovieViewHolder(binding.includeItemFilm.root)
        // Pasamos la posición 0 (único elemento) para que el adapter "pinche" los datos
        singleMovieAdapter.onBindViewHolder(viewHolder, 0)
    }

    /**
     * Configurar botones Aceptar / Rechazar
     */
    private fun setupButtons() {
        binding.btnAccept.setOnClickListener {
            val current = viewModel.currentMovie.value ?: return@setOnClickListener
            current.providerId = intent.getIntExtra("EXTRA_PROVIDER_ID", 1899)
            current.userName = username
            if(current.overview == null || current.overview == ""){
                current.overview = getString(R.string.txt_no_sinopsis)
            }

            // Guarda en BD local y Firestore
            viewModel.saveWatchedMovie(current)
            viewModel.saveAcceptedMovie(
                current,
                onSuccess = { msg -> showShorterToast(msg) },
                onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
            )
            viewModel.nextMovie {
                Toast.makeText(this, getString(R.string.txt_no_films_available), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, getString(R.string.txt_no_films_available), Toast.LENGTH_SHORT).show()
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
        val genreMap = mapOf(
            28 to getString(R.string.genre_action),
            12    to getString(R.string.genre_adventure),
            16    to getString(R.string.genre_animation),
            35    to getString(R.string.genre_comedy),
            80    to getString(R.string.genre_crime),
            99    to getString(R.string.genre_documentary),
            18    to getString(R.string.genre_drama),
            10751 to getString(R.string.genre_family),
            14    to getString(R.string.genre_fantasy),
            36    to getString(R.string.genre_history),
            27    to getString(R.string.genre_horror),
            10402 to getString(R.string.genre_music),
            9648  to getString(R.string.genre_mystery),
            10749 to getString(R.string.genre_romance),
            878   to getString(R.string.genre_scifi),
            10770 to getString(R.string.genre_tv_movie),
            53    to getString(R.string.genre_thriller),
            10752 to getString(R.string.genre_war),
            37    to getString(R.string.genre_western)
        )

        val genreNames = genreMap.values.toList()
        val genreIds = genreMap.keys.toList()

        // Mostramos un diálogo con las opciones
        MaterialAlertDialogBuilder(this, R.style.RoundedMaterialDialog)
            .setTitle(getString(R.string.txt_search_film_by_genre))
            .setItems(genreNames.toTypedArray()) { dialog, which ->
                val selectedGenreId = genreIds[which]
                viewModel.setSelectedGenre(selectedGenreId)

                viewModel.fetchPage(
                    pageToLoad = 1,
                    showNextImmediately = false,
                    onError = { message ->
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                )
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.txt_cancel), null)
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
                        //showMovieInfoDialog()
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
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog, null)

        // Referencias a los elementos del layout
        val tvMovieTitle = dialogView.findViewById<TextView>(R.id.tv_movie_title)
        val tvOverviewInfo = dialogView.findViewById<TextView>(R.id.tv_overview_info)
        val tvReleaseDateInfo = dialogView.findViewById<TextView>(R.id.tv_release_date_info)
        val tvGenreInfo = dialogView.findViewById<TextView>(R.id.tv_genre_info)

        // Asignación de valores
        tvMovieTitle.text = movie.title ?: getString(R.string.txt_title)
        tvOverviewInfo.text = if (movie.overview.isNullOrBlank()) getString(R.string.txt_no_sinopsis) else movie.overview

        // **Corrección de la fecha al formato dd-MM-yyyy**
        tvReleaseDateInfo.text = formatReleaseDate(movie.releaseDate)

        // **Corrección de los géneros para mostrar nombres en lugar de ID**
        tvGenreInfo.text = getGenres(this@PlayMatchActivity,movie.genreIds)

        // Construcción del AlertDialog
       val dialog = MaterialAlertDialogBuilder(this, R.style.RoundedMaterialDialog)
           .setView(dialogView)
           .setPositiveButton(getString(R.string.txt_close), null)
           .create()

       dialog.show()
    }

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
                    if(current.overview == null || current.overview == ""){
                        current.overview = getString(R.string.txt_no_sinopsis)
                    }

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
                            getString(R.string.txt_no_films_available),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
            .start()
    }
}

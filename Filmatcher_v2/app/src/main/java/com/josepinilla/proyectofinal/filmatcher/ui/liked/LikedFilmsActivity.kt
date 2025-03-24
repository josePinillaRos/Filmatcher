package com.josepinilla.proyectofinal.filmatcher.ui.liked

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.josepinilla.proyectofinal.filmatcher.R
import com.josepinilla.proyectofinal.filmatcher.WatchedMoviesApplication
import com.josepinilla.proyectofinal.filmatcher.adapters.LikedFilmsAdapter
import com.josepinilla.proyectofinal.filmatcher.data.RemoteDataSource
import com.josepinilla.proyectofinal.filmatcher.data.Repository
import com.josepinilla.proyectofinal.filmatcher.databinding.ActivityLikedFilmsBinding
import com.josepinilla.proyectofinal.filmatcher.models.Result
import com.josepinilla.proyectofinal.filmatcher.utils.getGenres
import com.josepinilla.proyectofinal.filmatcher.utils.providerMap
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * LikedFilmsActivity
 * Muestra las películas guardadas por el usuario autenticado.
 *
 * @author Jose Pinilla
 */
class LikedFilmsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLikedFilmsBinding
    private lateinit var viewModel: LikedFilmsViewModel
    private lateinit var adapter: LikedFilmsAdapter

    // Obtener el username desde SharedPreferences
    private val sharedPrefs by lazy {
        getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    }

    private val username by lazy {
        sharedPrefs.getString("username", "guest_user") ?: "guest_user"
    }

    // Provider seleccionado (0 = Todos), se inicializa por defecto
    private var selectedProviderId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLikedFilmsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura la Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.subtitle = getString(R.string.txt_stored_films)

        // Botón para volver a la actividad principal
        findViewById<ImageButton>(R.id.btnBackToMain).setOnClickListener {
            finish() // Cierra esta actividad y vuelve a MainActivity
        }

        // Bloquear rotación de pantalla
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR

        // Inicializar repositorio y ViewModel
        val repository = Repository(RemoteDataSource(), (application as WatchedMoviesApplication).db)
        val factory = LikedFilmsViewModelFactory(repository, username)
        viewModel = ViewModelProvider(this, factory)[LikedFilmsViewModel::class.java]

        // Configurar RecyclerView
        setupRecyclerView()

        // Cargar las películas guardadas
        loadLikedMovies()
    }

    /**
     * Configura el RecyclerView y el adaptador.
     */
    private fun setupRecyclerView() {
        // Inicializar el adaptador
        adapter = LikedFilmsAdapter(emptyList()) { movie ->
            showMovieInfoDialog(movie)
        }
        binding.rvLikedFilms.layoutManager = LinearLayoutManager(this)
        binding.rvLikedFilms.adapter = adapter

        /**
         * ItemTouchHelper para deslizar a la derecha y eliminar una película. Mediante un swipe.
         * Se muestra un diálogo de confirmación antes de eliminar.
         * Se restaura la película si el usuario cancela.
         * Se elimina la película si el usuario confirma.
         * Se actualiza la lista de películas en Firestore.
         */
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                adapter.notifyItemMoved(fromPosition, toPosition)
                return true
            }

            // Al deslizar a la derecha, se elimina la película
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val movie = adapter.movies[position]
                val deleteConfirmation = getString(R.string.delete_confirmation, movie.title)


                // Restaurar la película temporalmente hasta que el usuario confirme
                adapter.notifyItemChanged(position)

                // Mostrar diálogo de confirmación
                MaterialAlertDialogBuilder(this@LikedFilmsActivity)
                    .setTitle(R.string.txt_delete_movie)
                    .setMessage(deleteConfirmation)
                    .setPositiveButton(R.string.txt_delete) { _, _ ->
                        // Si el usuario confirma, se elimina la película
                        val currentMovies = adapter.movies.toMutableList()
                        currentMovies.removeAt(position)
                        adapter.updateMovies(currentMovies)

                        // Eliminar de Firestore
                        lifecycleScope.launch {
                            viewModel.deleteUserMovie(username, movie, movie.providerId)
                            Toast.makeText(this@LikedFilmsActivity, R.string.txt_film_deleted, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton(R.string.txt_cancel) { dialog, _ ->
                        // Si cancela, se mantiene en la lista
                        dialog.dismiss()
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }

            // Dibujar el fondo y el icono de eliminación
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

                // Se comprueba que el itemView se está moviendo hacia la derecha
                if (dX > 0) {
                    val itemView = viewHolder.itemView
                    val itemHeight = itemView.bottom - itemView.top

                    // Configurar Paint para el fondo rojo con esquinas redondeadas
                    val backgroundPaint = Paint().apply {
                        color = Color.RED
                        isAntiAlias = true
                    }

                    // backgroundMargin para hacer el fondo más delgado
                    val backgroundMargin = 40f  // margen para la altura
                    val cornerRadius = 40f  // Mantiene las esquinas redondeadas

                    // Definir los bordes del rectángulo de fondo
                    val backgroundRect = RectF(
                        itemView.left.toFloat(),
                        itemView.top + backgroundMargin,
                        itemView.left + dX + 40f,  // Pinta según se desliza la X
                        itemView.bottom - backgroundMargin
                    )

                    // Dibujar el fondo redondeado con menor altura
                    c.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, backgroundPaint)

                    // Obtener el icono de eliminación
                    val iconTrash: Drawable? = AppCompatResources.getDrawable(
                        recyclerView.context, R.drawable.papelera
                    ) ?: return

                    // Reducir el tamaño del icono
                    val scaleFactor = 0.7f
                    val iconWidth = (iconTrash!!.intrinsicWidth * scaleFactor).toInt()
                    val iconHeight = (iconTrash.intrinsicHeight * scaleFactor).toInt()

                    // Calcular margen izquierdo para centrar el icono verticalmente
                    val leftMargin = (itemHeight - iconHeight) / 2

                    // Posiciones del icono
                    val iconTop = itemView.top + leftMargin
                    val iconBottom = iconTop + iconHeight
                    val iconLeft = itemView.left + leftMargin
                    val iconRight = iconLeft + iconWidth

                    // Asignar las medidas al icono con el nuevo tamaño reducido
                    iconTrash.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                    // Dibujar icono en el canvas
                    iconTrash.draw(c)
                }
            }

        }).attachToRecyclerView(binding.rvLikedFilms)
    }


    /**
     * Carga las películas guardadas por el usuario.
     */
    private fun loadLikedMovies() {
        lifecycleScope.launch {
            val movies = viewModel.getUserMovies()

            // Filtramos según el providerId
            val filteredMovies = if (selectedProviderId == 0) {
                movies  // 0 = "Todos"
            } else {
                movies.filter { it.providerId == selectedProviderId }
            }

            if (filteredMovies.isEmpty()) {
                Toast.makeText(this@LikedFilmsActivity, R.string.txt_no_stored_films, Toast.LENGTH_SHORT).show()
            }
            adapter.updateMovies(filteredMovies)
        }
    }

    /**
     * Muestra un diálogo con la sinopsis de la película seleccionada.
     */
    private fun showMovieInfoDialog(movie: Result) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog, null)

        // elementos del layout
        val tvMovieTitle = dialogView.findViewById<TextView>(R.id.tv_movie_title)
        val tvOverviewInfo = dialogView.findViewById<TextView>(R.id.tv_overview_info)
        val tvReleaseDateInfo = dialogView.findViewById<TextView>(R.id.tv_release_date_info)
        val tvGenreInfo = dialogView.findViewById<TextView>(R.id.tv_genre_info)

        // Asignación de valores
        tvMovieTitle.text = movie.title ?: getString(R.string.txt_title)
        tvOverviewInfo.text = if (movie.overview.isNullOrBlank()) getString(R.string.txt_no_sinopsis) else movie.overview
        tvReleaseDateInfo.text = formatReleaseDate(movie.releaseDate)
        tvGenreInfo.text = getGenres(this@LikedFilmsActivity,movie.genreIds)

        // Construcción del MaterialAlertDialog con bordes redondeados
        val dialog = MaterialAlertDialogBuilder(this, R.style.RoundedMaterialDialog)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.txt_close), null)
            .create()

        dialog.show()
    }

    /**
     * Formatea la fecha de lanzamiento de la película.
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

    /**
     * onCreateOptionsMenu
     * Infla el menú de opciones.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_liked_films, menu)
        return true
    }

    /**
     * onOptionsItemSelected
     * Maneja las acciones de los elementos del menú.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                showProviderFilterDialog() // Muestra el diálogo de filtro por plataforma
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
            .setItems(providerNames) { dialog, which -> // Posición del itmem
                val chosenProviderId = providerIds[which]
                selectedProviderId = chosenProviderId
                // Se vuelven a cargar las películas con el filtro seleccionado
                loadLikedMovies()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.txt_cancel, null)
            .show()
    }
}

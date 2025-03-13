package com.josepinilla.proyectofinal.filmatcher.ui.liked

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.josepinilla.proyectofinal.filmatcher.R
import com.josepinilla.proyectofinal.filmatcher.WatchedMoviesApplication
import com.josepinilla.proyectofinal.filmatcher.adapters.LikedFilmsAdapter
import com.josepinilla.proyectofinal.filmatcher.data.RemoteDataSource
import com.josepinilla.proyectofinal.filmatcher.data.Repository
import com.josepinilla.proyectofinal.filmatcher.databinding.ActivityLikedFilmsBinding
import com.josepinilla.proyectofinal.filmatcher.models.Result
import com.josepinilla.proyectofinal.filmatcher.utils.providerMap
import kotlinx.coroutines.launch

/**
 * LikedFilmsActivity
 * Muestra las películas guardadas por el usuario autenticado.
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

    // Provider seleccionado (0 = Todos)
    private var selectedProviderId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLikedFilmsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.subtitle = "Películas guardadas"
        findViewById<ImageButton>(R.id.btnBackToMain).setOnClickListener {
            finish() // Cierra esta actividad y vuelve a MainActivity
        }

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
        adapter = LikedFilmsAdapter(emptyList()) { movie ->
            showMovieInfoDialog(movie)
        }
        binding.rvLikedFilms.layoutManager = LinearLayoutManager(this)
        binding.rvLikedFilms.adapter = adapter

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

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val movie = adapter.movies[position]

                // Restaurar la película temporalmente hasta que el usuario confirme
                adapter.notifyItemChanged(position)

                // Mostrar diálogo de confirmación
                AlertDialog.Builder(this@LikedFilmsActivity)
                    .setTitle("Eliminar película")
                    .setMessage("¿Estás seguro de que quieres eliminar '${movie.title}'?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        // Si el usuario confirma, se elimina la película
                        val currentMovies = adapter.movies.toMutableList()
                        currentMovies.removeAt(position)
                        adapter.updateMovies(currentMovies)

                        // Eliminar de Firestore
                        lifecycleScope.launch {
                            viewModel.deleteUserMovie(username, movie, movie.providerId)
                            Toast.makeText(this@LikedFilmsActivity, "Película eliminada", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancelar") { dialog, _ ->
                        // Si cancela, se mantiene en la lista
                        dialog.dismiss()
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }

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

                    // **Ajuste**: Incrementar backgroundMargin para hacer el fondo más delgado
                    val backgroundMargin = 40f  // margen para la altura
                    val cornerRadius = 40f  // Mantiene las esquinas redondeadas

                    // Definir los bordes del rectángulo de fondo
                    val backgroundRect = RectF(
                        itemView.left.toFloat(),
                        itemView.top + backgroundMargin,
                        itemView.left + dX + 20f,  // Pinta según se desliza la X
                        itemView.bottom - backgroundMargin
                    )

                    // Dibujar el fondo redondeado con menor altura
                    c.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, backgroundPaint)

                    // Obtener el icono de eliminación
                    val iconTrash: Drawable? = AppCompatResources.getDrawable(
                        recyclerView.context, R.drawable.papelera
                    ) ?: return

                    // Reducir el tamaño del icono (Ej: 70% del tamaño original)
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
                Toast.makeText(this@LikedFilmsActivity, "No tienes películas guardadas", Toast.LENGTH_SHORT).show()
            }
            adapter.updateMovies(filteredMovies)
        }
    }

    /**
     * Muestra un diálogo con la sinopsis de la película seleccionada.
     */
    private fun showMovieInfoDialog(movie: Result) {
        val overviewText = if (movie.overview.isNullOrBlank()) "Sin sinopsis disponible" else movie.overview

        AlertDialog.Builder(this)
            .setTitle(movie.title ?: "Película")
            .setMessage("Sinopsis: $overviewText")
            .setPositiveButton("Cerrar", null)
            .show()
    }

    // Inflamos el menú
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_liked_films, menu)
        return true
    }

    // Manejamos el clic en el ítem de filtrado
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

        AlertDialog.Builder(this)
            .setTitle("Filtrar por plataforma")
            .setItems(providerNames) { dialog, which ->
                // 'which' es la posición en el array, no el ID real
                val chosenProviderId = providerIds[which]
                selectedProviderId = chosenProviderId
                loadLikedMovies()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

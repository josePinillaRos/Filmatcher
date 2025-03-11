package com.josepinilla.proyectofinal.filmatcher.ui.matches

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
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
import com.google.android.material.snackbar.Snackbar
import com.josepinilla.proyectofinal.filmatcher.R
import com.josepinilla.proyectofinal.filmatcher.WatchedMoviesApplication
import com.josepinilla.proyectofinal.filmatcher.adapters.LikedFilmsAdapter
import com.josepinilla.proyectofinal.filmatcher.data.RemoteDataSource
import com.josepinilla.proyectofinal.filmatcher.data.Repository
import com.josepinilla.proyectofinal.filmatcher.databinding.ActivityLikedFilmsBinding
import com.josepinilla.proyectofinal.filmatcher.models.Result
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLikedFilmsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.subtitle = "Películas guardadas"
        findViewById<ImageButton>(R.id.btnBackToMain).setOnClickListener {
            finish() // Cierra esta actividad y vuelve a MainActivity
        }

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
                    // Obtener el icono de eliminación
                    val iconTrash: Drawable? = AppCompatResources.getDrawable(
                        recyclerView.context, R.drawable.papelera
                    ) ?: return

                    // Reducir el tamaño del icono (Ej: 70% del tamaño original)
                    val scaleFactor = 0.7f
                    val iconWidth = (iconTrash!!.intrinsicWidth * scaleFactor).toInt()
                    val iconHeight = (iconTrash.intrinsicHeight * scaleFactor).toInt()

                    // Calcular margen izquierdo para centrar el icono verticalmente
                    val leftMargin = (viewHolder.itemView.height - iconHeight) / 2

                    // Posiciones del icono
                    val iconTop = viewHolder.itemView.top + leftMargin
                    val iconBottom = iconTop + iconHeight
                    val iconLeft = viewHolder.itemView.left + leftMargin
                    val iconRight = iconLeft + iconWidth

                    // Asignar las medidas al icono con el nuevo tamaño reducido
                    iconTrash.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                    // Crear el fondo rojo y establecer sus límites
                    val background = ColorDrawable(Color.RED)
                    val backgroundMargin = 32 // Reduce la altura superior e inferior
                    background.setBounds(
                        viewHolder.itemView.left,
                        viewHolder.itemView.top + backgroundMargin,
                        viewHolder.itemView.left + dX.toInt() + 20, // Pinta según se desplaza la X
                        viewHolder.itemView.bottom - backgroundMargin
                    )

                    // Dibujar fondo y luego icono en el canvas
                    background.draw(c)
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
            if (movies.isEmpty()) {
                Toast.makeText(this@LikedFilmsActivity, "No tienes películas guardadas", Toast.LENGTH_SHORT).show()
            }
            adapter.updateMovies(movies)
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
}

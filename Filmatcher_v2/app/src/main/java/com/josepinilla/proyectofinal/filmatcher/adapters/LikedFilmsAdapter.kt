package com.josepinilla.proyectofinal.filmatcher.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.josepinilla.proyectofinal.filmatcher.R
import com.josepinilla.proyectofinal.filmatcher.models.Result
import com.josepinilla.proyectofinal.filmatcher.utils.getGenres
import com.josepinilla.proyectofinal.filmatcher.utils.providerLogos

/**
 * MatchesAdapter
 * Adaptador para mostrar las películas en común entre dos usuarios.
 *
 * @param movies Lista de películas en común
 * @param onItemClick Callback al hacer clic en una película
 */
class LikedFilmsAdapter(
    var movies: List<Result>,
    private val onItemClick: (Result) -> Unit
) : RecyclerView.Adapter<LikedFilmsAdapter.LikedFilmsViewHolder>() {

    // ViewHolder que contiene las referencias a los elementos de la vista
    class LikedFilmsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProvider: ImageView = itemView.findViewById(R.id.ivProvider)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val ivImage: ImageView = itemView.findViewById(R.id.ivImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LikedFilmsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_liked_films, parent, false)
        return LikedFilmsViewHolder(view)
    }

    override fun onBindViewHolder(holder: LikedFilmsViewHolder, position: Int) {
        val movie = movies[position]

        // Título
        holder.tvTitle.text = movie.title ?: "Sin título"

        // Cargar imagen de la película con Glide
        Glide.with(holder.itemView.context)
            .load("https://image.tmdb.org/t/p/w500${movie.posterPath}")
            .placeholder(R.drawable.netflix) // Imagen por defecto si no carga
            .error(R.drawable.netflix) // Imagen en caso de error
            .into(holder.ivImage)

        // Cargar imagen del proveedor (Netflix, Disney, etc.)
        val providerImage = providerLogos[movie.providerId] ?: R.drawable.netflix
        Glide.with(holder.itemView.context)
            .load(providerImage)
            .placeholder(R.drawable.netflix)
            .error(R.drawable.netflix)
            .into(holder.ivProvider)

        // Acción al hacer click en un item
        holder.itemView.setOnClickListener {
            onItemClick(movie)
        }
    }

    override fun getItemCount(): Int = movies.size

    // Método para actualizar la lista de películas dinámicamente
    fun updateMovies(newMovies: List<Result>) {
        movies = newMovies
        notifyDataSetChanged()
    }
}

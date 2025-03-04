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
 * MovieAdapter
 * Clase que representa el adaptador de la lista de películas
 *
 * @param movies lista de películas
 * @param onItemClick función que se ejecuta al hacer clic en una película
 * @constructor Crea un adaptador de películas
 */
class MovieAdapter(
    var movies: List<Result>,
    private val onItemClick: (Result) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    // Se instancian los elementos de la vista del item
    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProvider: ImageView = itemView.findViewById(R.id.ivProvider)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val ivImage: ImageView = itemView.findViewById(R.id.ivImage)
        val tvGenre: TextView = itemView.findViewById(R.id.tvGenere)
        val tvYear: TextView = itemView.findViewById(R.id.tvYear)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_film, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]

        // Asignamos los valores
        holder.tvTitle.text = movie.title ?: "Sin título"
        holder.tvGenre.text = "Género(s): ${getGenres(movie.genreIds)}"
        holder.tvYear.text = "Año: ${movie.releaseDate?.substring(0, 4) ?: "Desconocido"}"

        // Cargar la imagen con Glide
        Glide.with(holder.itemView.context)
            .load("https://image.tmdb.org/t/p/w500${movie.posterPath}")
            .placeholder(R.drawable.netflix) // Imagen por defecto si no carga
            .error(R.drawable.netflix) // Imagen en caso de error
            .into(holder.ivImage)

        Glide.with(holder.itemView.context)
            .load(R.drawable.netflix)
            .placeholder(R.drawable.netflix)
            .into(holder.ivProvider)

        val providerImage = providerLogos[movie.providerId] ?: R.drawable.netflix
        Glide.with(holder.itemView.context)
            .load(providerImage)
            .into(holder.ivProvider)

        // Acción al hacer click en un item
        holder.itemView.setOnClickListener {
            onItemClick(movie)
        }
    }

    override fun getItemCount(): Int = movies.size
}

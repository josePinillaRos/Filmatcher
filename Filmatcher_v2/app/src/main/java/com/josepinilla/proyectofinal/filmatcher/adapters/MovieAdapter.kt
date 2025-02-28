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

class MovieAdapter(
    var movies: List<Result>,
    private val onItemClick: (Result) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProvider: ImageView = itemView.findViewById(R.id.ivProvider)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val ivImage: ImageView = itemView.findViewById(R.id.ivImage)
        val tvGenere: TextView = itemView.findViewById(R.id.tvGenere)
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
        holder.tvGenere.text = "Género(s): ${getGenres(movie.genreIds)}"
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

    /**
     * Convierte una lista de IDs de géneros en un string legible.
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

    private val providerLogos = mapOf(
        8 to R.drawable.netflix,
        2241 to R.drawable.movistar,
        337 to R.drawable.disney,
        1899 to R.drawable.max,
        119 to R.drawable.amazon
    )

}

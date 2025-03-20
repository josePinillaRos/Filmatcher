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
        val context = holder.itemView.context
        val imageUrl = "https://image.tmdb.org/t/p/w500${movie.posterPath}"


        // Asignamos los valores
        holder.tvTitle.text = movie.title ?: R.string.txt_no_title.toString()
        holder.tvGenre.text = context.getString(R.string.txt_genre, getGenres(context,movie.genreIds))
        holder.tvYear.text = context.getString(R.string.txt_year, movie.releaseDate?.substring(0, 4)
            ?: R.string.txt_year_unknown.toString())

        // Cargar la imagen con Glide
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .error(R.drawable.notfound) // Imagen en caso de error
            .into(holder.ivImage)


        // Cargar imagen del proveedor (Netflix, Disney, etc.)
        val providerImage = providerLogos[movie.providerId] ?: R.drawable.notfound
        Glide.with(holder.itemView.context)
            .load(providerImage)
            .placeholder(R.drawable.notfound)
            .error(R.drawable.notfound)
            .into(holder.ivProvider)

        // Acción al hacer click en un item
        holder.itemView.setOnClickListener {
            onItemClick(movie)
        }
    }

    override fun getItemCount(): Int = movies.size
}

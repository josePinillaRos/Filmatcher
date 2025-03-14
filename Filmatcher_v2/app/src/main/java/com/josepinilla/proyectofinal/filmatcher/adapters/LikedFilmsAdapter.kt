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
import com.josepinilla.proyectofinal.filmatcher.utils.providerLogos

/**
 * MatchesAdapter
 * Adaptador para mostrar las películas de un usuario.
 *
 * @param movies Lista de películas
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
        val context = holder.itemView.context
        val imageUrl = context.getString(R.string.base_image_url, movie.posterPath ?: "")


        // Título
        holder.tvTitle.text = movie.title ?: R.string.txt_no_title.toString()

        // Cargar imagen de la película con Glide
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.notfound) // Imagen por defecto si no carga
            .error(R.drawable.notfound) // Imagen en caso de error
            .into(holder.ivImage)

        // Cargar imagen del proveedor (Netflix, Disney, etc.)
        val providerImage = providerLogos[movie.providerId] ?: R.drawable.netflix
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

    // Método para actualizar la lista de películas dinámicamente
    fun updateMovies(newMovies: List<Result>) {
        movies = newMovies
        notifyDataSetChanged()
    }
}

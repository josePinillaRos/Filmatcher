package com.josepinilla.proyectofinal.filmatcher.data

import com.google.firebase.firestore.FirebaseFirestore
import com.josepinilla.proyectofinal.filmatcher.models.Result
import kotlinx.coroutines.tasks.await

class RepositoryFirebase {
    private val db = FirebaseFirestore.getInstance()

    /**
     * saveAcceptedMovie
     * Guarda una película aceptada en Firestore: user_movies/username/movies/movieId
     */
     fun saveAcceptedMovie(username: String, movie: Result, providerId: Int) {
        val docId = "${movie.id}_$providerId"
        val docData = hashMapOf(
            "movieId" to movie.id,
            "title" to (movie.title ?: "Desconocido"),
            "posterPath" to (movie.posterPath ?: ""),
            "releaseDate" to (movie.releaseDate ?: "Desconocido"), // Año de lanzamiento
            "genreIds" to (movie.genreIds ?: emptyList()), // Lista de géneros
            "overview" to (movie.overview ?: "Sin sinopsis disponible"), // Sinopsis
            "providerId" to providerId, // Guardar ID de la plataforma
            "timestamp" to System.currentTimeMillis()
        )

        // Guardar en Firestore
        db.collection("user_movies")
            .document(username)
            .collection("movies")
            .document(docId)
            .set(docData)
    }

    /**
     * deleteMoviesByUser
     * Elimina las películas de un usuario de una plataforma en Firestore
     */
    fun deleteMovie(username: String, movie: Result, providerId: Int) {
        val docId = "${movie.id}_$providerId"
        db.collection("user_movies")
            .document(username)
            .collection("movies")
            .document(docId)
            .delete()
    }

    /**
     * fetchCommonMovies
     * Retorna la lista de IDs en común entre dos usuarios
     */
    suspend fun fetchCommonResults(userA: String, userB: String): List<Result> {
        // Recoge las películas de userA
        val queryUserA = db.collection("user_movies")
            .document(userA)
            .collection("movies")
            .get()
            .await()
        val filmsUserA = queryUserA.documents.mapNotNull { it.getLong("movieId")?.toInt() }.toSet()

        // Peliculas de userB
        val queryUserB = db.collection("user_movies")
            .document(userB)
            .collection("movies")
            .get()
            .await()

        // filtrar las películas de userB que también tiene userA
        val commonDocs = queryUserB.documents.filter { doc ->
            val movieId = doc.getLong("movieId")?.toInt()
            movieId != null && filmsUserA.contains(movieId)
        }

        // 3) Convertir Firestore docs a Result
        return commonDocs.mapNotNull { doc ->
            val movieId = doc.getLong("movieId")?.toInt() ?: return@mapNotNull null
            val title = doc.getString("title") ?: "Desconocido"
            val posterPath = doc.getString("posterPath") ?: ""
            val releaseDate = doc.getString("releaseDate") ?: "Desconocido"
            val overview = doc.getString("overview") ?: "Sin sinopsis disponible"
            val providerId = doc.getLong("providerId")?.toInt() ?: 0
            val genreIds = (doc.get("genreIds") as? List<*>)?.mapNotNull { it as? Long }?.map { it.toInt() } ?: emptyList()

            Result(
                id = movieId,
                providerId = providerId,
                title = title,
                posterPath = posterPath,
                releaseDate = releaseDate,
                overview = overview,
                genreIds = genreIds
            )
        }
    }

}
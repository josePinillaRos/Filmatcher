package com.josepinilla.proyectofinal.filmatcher.data

import com.google.firebase.firestore.FirebaseFirestore
import com.josepinilla.proyectofinal.filmatcher.models.MoviesByProviders
import com.josepinilla.proyectofinal.filmatcher.models.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class Repository(
    private val remoteDataSource: RemoteDataSource
) {
    private val db = FirebaseFirestore.getInstance()

    /**
     * fetchMovies
     * Llama a la API TMDb para obtener las películas de un proveedor y página
     */
    suspend fun fetchMovies(watchProvider: Int, page: Int): List<Result> {
        val response: MoviesByProviders =
            remoteDataSource.getMoviesByProvider(watchProvider, page)
        return response.results?.filterNotNull() ?: emptyList()
    }

    /**
     * saveAcceptedMovie
     * Guarda una película aceptada en Firestore: user_movies/username/movies/movieId
     */
    suspend fun saveAcceptedMovie(username: String, movie: Result, providerId: Int) {
        val movieId = movie.id ?: return
        val docData = hashMapOf(
            "movieId" to movieId,
            "title" to (movie.title ?: "Desconocido"),
            "posterPath" to (movie.posterPath ?: ""),
            "releaseDate" to (movie.releaseDate ?: "Desconocido"), // Año de lanzamiento
            "genreIds" to (movie.genreIds ?: emptyList()), // Lista de géneros
            "overview" to (movie.overview ?: "Sin sinopsis disponible"), // Sinopsis
            "providerId" to providerId, // Guardar ID de la plataforma
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("user_movies")
            .document(username)
            .collection("movies")
            .document(movieId.toString())
            .set(docData)
            .await()
    }


    /**
     * fetchCommonMovies
     * Retorna la lista de IDs en común entre dos usuarios
     */
    suspend fun fetchCommonMovies(userA: String, userB: String): List<Int> {
        // Pelis de userA
        val moviesA = db.collection("user_movies")
            .document(userA)
            .collection("movies")
            .get()
            .await()
            .documents
            .mapNotNull { it.getLong("movieId")?.toInt() }
            .toSet()

        // Pelis de userB
        val moviesB = db.collection("user_movies")
            .document(userB)
            .collection("movies")
            .get()
            .await()
            .documents
            .mapNotNull { it.getLong("movieId")?.toInt() }

        return moviesB.filter { it in moviesA }
    }

    /**
     * fetchAcceptedMoviesOfUser
     * Devuelve en tiempo real (Flow) las películas de un usuario
     */
    fun fetchAcceptedMoviesOfUser(username: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = db.collection("user_movies")
            .document(username)
            .collection("movies")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { it.data ?: emptyMap() } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }
}


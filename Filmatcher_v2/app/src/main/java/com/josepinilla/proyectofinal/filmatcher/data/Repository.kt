package com.josepinilla.proyectofinal.filmatcher.data

import com.josepinilla.proyectofinal.filmatcher.models.MoviesByProviders
import com.josepinilla.proyectofinal.filmatcher.models.Result

/**
 * Repository
 * Clase que maneja la lógica de negocio y la comunicación entre las fuentes de datos.
 * @param remoteDataSource fuente de datos remota
 * @param db base de datos de Room
 */
class Repository(
    val remoteDataSource: RemoteDataSource,
    db: WatchedMoviesRoomDB
) {


    private val localDataSource = LocalDataSource(db.watchedMoviesDao())

    private val fbRepository = RepositoryFirebase()

    /**
     * saveAcceptedMovie
     * Guarda una película aceptada en Firestore: user_movies/username/movies/movieId
     */
    fun saveAcceptedMovie(username: String, movie: Result, providerId: Int) {
        fbRepository.saveAcceptedMovie(username, movie, providerId)
    }


    /**
     * fetchMovies
     * Llama a la API TMDb para obtener las películas de un proveedor y página
     */
    suspend fun fetchMovies(watchProvider: Int, page: Int, genreFilter: Int = 0): List<Result> {
        val response: MoviesByProviders = remoteDataSource.getMoviesByProvider(watchProvider, page)

        val allMovies = response.results?.filterNotNull() ?: emptyList()

        val filteredMovies = allMovies.filter { movie ->
            val isWatched = localDataSource.getWatchedMoviesById(movie.id, watchProvider).isNotEmpty()

            // Verificamos si la película contiene el género seleccionado,
            // a menos que genreFilter sea 0 que es todos los géneros
            val belongsToGenre = (genreFilter == 0) || (movie.genreIds?.contains(genreFilter) == true)

            // Aceptamos la película solo si no está en la bbdd local y si cumple el filtro de género
            // asi obtenemos solo las películas que no hemos visto y que pertenecen al género seleccionado
            (!isWatched) && belongsToGenre
        }

        return filteredMovies
    }

    /**
     * insertWatchedMovie
     * Inserta una película vista en la base de datos local
     */
    suspend fun insertWatchedMovie(movie: Result) {
        localDataSource.insertWatchedMovie(movie)
    }

    /**
     * getTotalPages
     * Obtiene el número total de páginas de películas de un proveedor de streaming
     */
    suspend fun getTotalPages(providerID: Int): MoviesByProviders {
        return remoteDataSource.getTotalPages(providerID)
    }

    /**
     * fetchCommonMatches
     * Retorna la lista de IDs en común entre dos usuarios
     */
    suspend fun fetchCommonMatches(userA: String, userB: String): List<Result> {
        return fbRepository.fetchCommonResults(userA, userB)
    }

}


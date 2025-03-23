package com.josepinilla.proyectofinal.filmatcher.data

import com.josepinilla.proyectofinal.filmatcher.models.MoviesByProviders
import com.josepinilla.proyectofinal.filmatcher.models.Result
import java.security.MessageDigest

/**
 * Repository
 * Clase que maneja la lógica de negocio y la comunicación entre las fuentes de datos.
 * @param remoteDataSource fuente de datos remota
 * @param db base de datos de Room
 *
 * @author Jose Pinilla
 */
class Repository(
    private val remoteDataSource: RemoteDataSource,
    db: WatchedMoviesRoomDB
) {
    private val localDataSource = LocalDataSource(db.watchedMoviesDao())

    private val fbRepository = RepositoryFirebase()

    /**
     * ================ Métodos de FireBase =================
     */

    /**
     * saveAcceptedMovie
     * Guarda una película aceptada en Firestore: user_movies/username/movies/movieId
     */
    fun saveAcceptedMovie(username: String, movie: Result, providerId: Int) {
        fbRepository.saveAcceptedMovie(username, movie, providerId)
    }

    /**
     * deleteMovie
     * Elimina las películas de un usuario de una plataforma en Firestore
     */
    fun deleteMovie(username: String, movie: Result, providerId: Int) {
        fbRepository.deleteMovie(username, movie, providerId)
    }

    /**
     * fetchCommonMatches
     * Retorna la lista de películas en común entre dos usuarios
     */
    suspend fun fetchCommonMatches(userA: String, userB: String): List<Result> {
        return fbRepository.fetchCommonResults(userA, userB)
    }

    /**
     * fetchUserMovies
     * Retorna la lista de películas de un usuario
     */
    suspend fun fetchUserMovies(username: String): List<Result> {
        return fbRepository.fetchUserMovies(username)
    }

    /**
     * getUserByUsername
     * Retorna un usuario por su nombre de usuario
     */
    suspend fun getUserByUsername(username: String) =
        fbRepository.getUserByUsername(username)

    /**
     * registerUser
     * Registra un usuario en Firebase y retorna true si se ha registrado correctamente
     */
    suspend fun registerUser(username: String, password: String): Boolean {
        val hashedPassword = hashPassword(password)
        return fbRepository.registerUser(username, hashedPassword)
    }


    /**
     * ================ Métodos base de datos local =================
     */

    /**
     * insertWatchedMovie
     * Inserta una película vista en la base de datos local
     */
    suspend fun insertWatchedMovie(movie: Result) {
        localDataSource.insertWatchedMovie(movie)
    }

    /**
     * resetMoviesByUser
     * Elimina las películas vistas de un usuario de una plataforma en la base de datos local
     */
    suspend fun resetMoviesByUser(username: String, providerId: Int) {
        localDataSource.resetMoviesByUser(username, providerId)
    }

    /**
     * getTotalPages
     * Obtiene el número total de páginas de películas de un proveedor de streaming
     */
    suspend fun getTotalPages(providerID: Int): Int {
        return remoteDataSource.getTotalPages(providerID)
    }


    /**
     * ================ Métodos de la API TMDb y filtrado con local =================
     */


    /**
     * fetchMovies
     * Llama a la API TMDb para obtener las películas de un proveedor y página
     */
    suspend fun fetchMovies(watchProvider: Int, page: Int, genreFilter: Int = 0, username: String): List<Result> {
        val response: MoviesByProviders = remoteDataSource.getMoviesByProvider(watchProvider, page)

        // Recibimos todas las películas de la API
        val allMovies = response.results?.filterNotNull() ?: emptyList()

        // Filtramos las películas que ya hemos visto, que estén en la bbdd local
        val filteredMovies = allMovies.filter { movie ->
            val isWatched = localDataSource.getWatchedMoviesById(movie.id, watchProvider, username).isNotEmpty()

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
     * ================ Métodos de autenticación =================
     */

    /**
     * hashPassword
     * Cifra la contraseña con SHA-256
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}


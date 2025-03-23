package com.josepinilla.proyectofinal.filmatcher.data

import com.josepinilla.proyectofinal.filmatcher.models.Result

/**
 * LocalDataSource
 * Clase que representa el origen de datos local
 * @param watchedMoviesDao DAO de películas vistas
 *
 * @author Jose Pinilla
 */
class LocalDataSource(private val watchedMoviesDao: WatchedMoviesDao) {

    // Inserta una película vista en la base de datos (Ya sea aceptada o rechazada)
    suspend fun insertWatchedMovie(movie: Result) {
        watchedMoviesDao.insertWatchedMovie(movie)
    }

    // Obtiene las películas vistas por un usuario
    suspend fun getWatchedMoviesById(id: Int, providerId: Int, userName: String): List<Result> {
        return watchedMoviesDao.getWatchedMoviesById(id, providerId, userName)
    }

    // Borra las películas vistas por un usuario
    suspend fun resetMoviesByUser(username: String, providerId: Int) {
        watchedMoviesDao.resetMoviesByUser(username, providerId)
    }
}
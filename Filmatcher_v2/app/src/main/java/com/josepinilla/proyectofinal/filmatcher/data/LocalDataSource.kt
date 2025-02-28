package com.josepinilla.proyectofinal.filmatcher.data

import com.josepinilla.proyectofinal.filmatcher.models.Result

class LocalDataSource(private val watchedMoviesDao: WatchedMoviesDao) {

    suspend fun insertWatchedMovie(movie: Result) {
        watchedMoviesDao.insertWatchedMovie(movie)
    }

    suspend fun getWatchedMoviesById(id: Int, providerId: Int): List<Result> {
        return watchedMoviesDao.getWatchedMoviesById(id, providerId)
    }
}
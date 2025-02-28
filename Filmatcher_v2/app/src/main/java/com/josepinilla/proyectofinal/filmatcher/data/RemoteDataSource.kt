package com.josepinilla.proyectofinal.filmatcher.data

import com.josepinilla.proyectofinal.filmatcher.models.MoviesByProviders

class RemoteDataSource {
    private val api = MoviesAPI.getRetrofit2Api()

    suspend fun getMoviesByProvider(providerId: Int,page: Int): MoviesByProviders {
        return api.getMoviesByProvider(watchProvider = providerId, page = page)
    }
}

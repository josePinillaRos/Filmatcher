package com.josepinilla.proyectofinal.filmatcher.data

import com.josepinilla.proyectofinal.filmatcher.models.MoviesByProviders

/**
 * RemoteDataSource
 * Clase que representa el origen de datos remoto
 * @param api API de películas
 *
 * @author Jose Pinilla
 */
class RemoteDataSource {
    private val api = MoviesAPI.getRetrofit2Api()

    /**
     * Obtiene las películas de un proveedor de streaming por página
     */
    suspend fun getMoviesByProvider(providerId: Int,page: Int): MoviesByProviders {
        return api.getMoviesByProvider(watchProvider = providerId, page = page)
    }

    /**
     * Obtiene el número total de páginas de películas de un proveedor de streaming
     */
    suspend fun getTotalPages(providerID: Int): Int {
        val response = api.getMoviesByProvider(watchProvider = providerID)
        return response.totalPages ?: 1
    }
}

package com.josepinilla.proyectofinal.filmatcher.data

import com.josepinilla.proyectofinal.filmatcher.BuildConfig
import com.josepinilla.proyectofinal.filmatcher.models.MoviesByProviders
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

/**
 * MoviesAPI
 * Llamada a la API de The Movie Database (TMDb)
 *
 * @author Jose Pinilla
 */
class MoviesAPI {
    companion object {
        // URL base de la API de TMDb
        private const val BASE_URL = "https://api.themoviedb.org/3/"

        // Método para obtener la interfaz de la API de TMDb
        fun getRetrofit2Api(): MoviesAPIInterface {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MoviesAPIInterface::class.java)
        }
    }
}

/**
 * Interface MoviesAPIInterface
 * Métodos para recolectar películas de TMDb con las anotaciones de Retrofit
 *
 * @author Jose Pinilla
 */
interface MoviesAPIInterface {
    // Token de la API via gradle.properties
    @Headers("Authorization: Bearer ${BuildConfig.API_KEY_TMDB}")
    @GET("discover/movie")
    // Método para obtener películas en idioma español, región española y
    // por proveedor de streaming (watchProvider)
    suspend fun getMoviesByProvider(
        @Query("language") language: String = "es-ES",
        @Query("page") page: Int = 1,
        @Query("region") region: String = "ES",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_watch_providers") watchProvider: Int,
        @Query("watch_region") watchRegion: String = "ES"
    ): MoviesByProviders
}


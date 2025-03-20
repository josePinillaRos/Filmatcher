package com.josepinilla.proyectofinal.filmatcher.utils

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.josepinilla.proyectofinal.filmatcher.R

/**
 * providerLogos
 * Mapa que contiene los logos de los proveedores de streaming segun la API
 */
val providerLogos = mapOf(
    8 to R.drawable.netflix,
    2241 to R.drawable.movistar,
    337 to R.drawable.disney,
    1899 to R.drawable.max,
    119 to R.drawable.amazon,
    350 to R.drawable.appletv
)

/**
 * providerMap
 * Mapa que contiene los nombres de los proveedores de streaming segun la API
 */
val providerMap = mapOf(
    0 to "Todos",
    8 to "Netflix",
    2241 to "Movistar",
    337 to "Disney",
    1899 to "Max",
    119 to "Prime Video",
    350 to "Apple TV"
)

/**
 * getGenres
 * Función que recibe una lista de IDs de géneros y devuelve un String con los nombres de los géneros
 * segun la API
 */
fun getGenres(context: Context, genreIds: List<Int?>?): String {

    val genreMap = mapOf(
        28 to R.string.genre_action,
        12    to R.string.genre_adventure,
        16    to R.string.genre_animation,
        35    to R.string.genre_comedy,
        80    to R.string.genre_crime,
        99    to R.string.genre_documentary,
        18    to R.string.genre_drama,
        10751 to R.string.genre_family,
        14    to R.string.genre_fantasy,
        36    to R.string.genre_history,
        27    to R.string.genre_horror,
        10402 to R.string.genre_music,
        9648  to R.string.genre_mystery,
        10749 to R.string.genre_romance,
        878   to R.string.genre_scifi,
        10770 to R.string.genre_tv_movie,
        53    to R.string.genre_thriller,
        10752 to R.string.genre_war,
        37    to R.string.genre_western
    )

    return genreIds
        ?.mapNotNull { id ->
            genreMap[id ?: 0]?.let { resId ->
                context.getString(resId as Int)
            }
        }
        ?.joinToString(", ")
        ?: context.getString(R.string.txt_cancel)
    //return genreIds?.mapNotNull { genreMap[it ?: 0] }?.joinToString(", ") ?: "Desconocido"
}


fun checkConnection(context: Context): Boolean {
    val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = cm.activeNetwork
    if (networkInfo != null) {
        val activeNetwork = cm.getNetworkCapabilities(networkInfo)
        if (activeNetwork != null)
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
    }
    return false
}

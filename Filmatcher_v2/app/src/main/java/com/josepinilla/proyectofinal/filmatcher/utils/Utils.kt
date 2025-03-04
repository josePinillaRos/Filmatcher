package com.josepinilla.proyectofinal.filmatcher.utils

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
    119 to R.drawable.amazon
)

/**
 * getGenres
 * Función que recibe una lista de IDs de géneros y devuelve un String con los nombres de los géneros
 * segun la API
 */
fun getGenres(genreIds: List<Int?>?): String {
    val genreMap = mapOf(
        28 to "Acción",
        12 to "Aventura",
        16 to "Animación",
        35 to "Comedia",
        80 to "Crimen",
        99 to "Documental",
        18 to "Drama",
        10751 to "Familia",
        14 to "Fantasía",
        36 to "Historia",
        27 to "Terror",
        10402 to "Música",
        9648 to "Misterio",
        10749 to "Romance",
        878 to "Ciencia ficción",
        10770 to "Película de TV",
        53 to "Suspense",
        10752 to "Bélica",
        37 to "Oeste"
    )

    return genreIds?.mapNotNull { genreMap[it ?: 0] }?.joinToString(", ") ?: "Desconocido"
}
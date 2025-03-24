package com.josepinilla.proyectofinal.filmatcher.models


import com.google.gson.annotations.SerializedName

/**
 * MoviesByProviders
 * Clase que representa la respuesta de la API de The Movie Database
 */
data class MoviesByProviders(
    @SerializedName("page")
    val page: Int?,
    @SerializedName("results")
    val results: List<Result?>?,
    @SerializedName("total_pages")
    val totalPages: Int?,
    @SerializedName("total_results")
    val totalResults: Int?
)
package com.josepinilla.proyectofinal.filmatcher.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "watched_movies",
    primaryKeys = ["id", "providerId", "userName"] // Clave compuesta
)
data class Result(
    @SerializedName("id")
    val id: Int,
    var providerId: Int,
    var userName: String = "",

    @Ignore
    @SerializedName("adult")
    val adult: Boolean? = null,

    @Ignore
    @SerializedName("backdrop_path")
    val backdropPath: String? = null,

    @Ignore
    @SerializedName("genre_ids")
    val genreIds: List<Int?>? = null,

    @Ignore
    @SerializedName("original_language")
    val originalLanguage: String? = null,

    @Ignore
    @SerializedName("original_title")
    val originalTitle: String? = null,

    @Ignore
    @SerializedName("overview")
    val overview: String? = null,

    @Ignore
    @SerializedName("popularity")
    val popularity: Double? = null,

    @Ignore
    @SerializedName("poster_path")
    val posterPath: String? = null,

    @Ignore
    @SerializedName("release_date")
    val releaseDate: String? = null,

    @Ignore
    @SerializedName("title")
    val title: String? = null,

    @Ignore
    @SerializedName("video")
    val video: Boolean? = null,

    @Ignore
    @SerializedName("vote_average")
    val voteAverage: Double? = null,

    @Ignore
    @SerializedName("vote_count")
    val voteCount: Int? = null

) : Parcelable {
    // Constructor secundario simple para Room
    constructor(id: Int, providerId: Int) : this(
        id = id,
        providerId = providerId,
        adult = null,
        backdropPath = null,
        genreIds = null,
        originalLanguage = null,
        originalTitle = null,
        overview = null,
        popularity = null,
        posterPath = null,
        releaseDate = null,
        title = null,
        video = null,
        voteAverage = null,
        voteCount = null
    )
}

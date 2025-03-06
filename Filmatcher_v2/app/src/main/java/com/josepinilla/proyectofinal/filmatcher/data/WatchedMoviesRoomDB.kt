package com.josepinilla.proyectofinal.filmatcher.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.josepinilla.proyectofinal.filmatcher.models.Result

/**
 * WatchedMoviesRoomDB
 * Clase que representa la base de datos de Room
 */
@Database(entities = [Result::class], version = 1)
abstract class WatchedMoviesRoomDB: RoomDatabase() {
    abstract fun watchedMoviesDao(): WatchedMoviesDao
}

@Dao
interface WatchedMoviesDao {
    /**
     * insertWatchedMovie
     * Inserta una película vista en la base de datos local
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchedMovie(movie: Result)

    /**
     * getWatchedMoviesById
     * Obtiene las películas vistas por un usuario por id y providerId
     */
    @Query("SELECT * FROM watched_movies WHERE id = :id AND providerId = :providerId AND userName = :userName")
    suspend fun getWatchedMoviesById(id: Int, providerId: Int, userName: String): List<Result>

    @Query("DELETE FROM watched_movies WHERE userName = :username AND providerId = :providerId")
    suspend fun resetMoviesByUser(username: String, providerId: Int)
}
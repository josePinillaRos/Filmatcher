package com.josepinilla.proyectofinal.filmatcher.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.josepinilla.proyectofinal.filmatcher.models.Result

@Database(entities = [Result::class], version = 1)
abstract class WatchedMoviesRoomDB: RoomDatabase() {
    abstract fun watchedMoviesDao(): WatchedMoviesDao
}

@Dao
interface WatchedMoviesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchedMovie(movie: Result)



    @Query("SELECT * FROM watched_movies WHERE id = :id AND providerId = :providerId")
    suspend fun getWatchedMoviesById(id: Int, providerId: Int): List<Result>
}
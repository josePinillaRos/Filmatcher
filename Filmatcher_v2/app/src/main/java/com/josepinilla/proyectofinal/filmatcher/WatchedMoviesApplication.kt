package com.josepinilla.proyectofinal.filmatcher

import android.app.Application
import androidx.room.Room
import com.josepinilla.proyectofinal.filmatcher.data.WatchedMoviesRoomDB

/**
 * WatchedMoviesApplication
 * Clase que representa la aplicación de la base de datos de Room
 */
class WatchedMoviesApplication : Application() {
    lateinit var db: WatchedMoviesRoomDB
        private set
    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(
            this,
            WatchedMoviesRoomDB::class.java, "watched_movies.db"
        ).build()
    }
}
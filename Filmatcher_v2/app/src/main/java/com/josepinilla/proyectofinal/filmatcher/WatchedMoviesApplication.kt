package com.josepinilla.proyectofinal.filmatcher

import android.app.Application
import androidx.room.Room
import com.josepinilla.proyectofinal.filmatcher.data.WatchedMoviesRoomDB

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
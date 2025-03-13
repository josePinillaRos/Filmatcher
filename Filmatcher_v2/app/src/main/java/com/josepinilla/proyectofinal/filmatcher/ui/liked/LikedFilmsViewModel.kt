package com.josepinilla.proyectofinal.filmatcher.ui.liked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.josepinilla.proyectofinal.filmatcher.data.Repository
import com.josepinilla.proyectofinal.filmatcher.models.Result

/**
 * ViewModel para gestionar las películas guardadas por el usuario.
 */
class LikedFilmsViewModel(
    private val repository: Repository,
    private val username: String
) : ViewModel() {

    /**
     * Obtiene las películas guardadas del usuario.
     */
    suspend fun getUserMovies(): List<Result> {
        return repository.fetchUserMovies(username)
    }

    fun deleteUserMovie(username: String, movie: Result, providerId: Int) {
        repository.deleteMovie(username, movie, providerId)
    }
}

/**
 * Factoría para instanciar el ViewModel con sus dependencias.
 */
@Suppress("UNCHECKED_CAST")
class LikedFilmsViewModelFactory(
    private val repository: Repository,
    private val username: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LikedFilmsViewModel(repository, username) as T
    }
}

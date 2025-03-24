package com.josepinilla.proyectofinal.filmatcher.ui.liked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.josepinilla.proyectofinal.filmatcher.data.Repository
import com.josepinilla.proyectofinal.filmatcher.models.Result

/**
 * LikedFilmsViewModel
 * ViewModel para gestionar las películas guardadas por el usuario.
 *
 * @param repository Repositorio de datos.
 * @param username Nombre de usuario.
 *
 * @author Jose Pinilla
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

    /**
     * Elimina una película guardada por el usuario.
     *
     * @param username Nombre de usuario.
     * @param movie Película a eliminar.
     * @param providerId Identificador del proveedor de la película.
     */
    fun deleteUserMovie(username: String, movie: Result, providerId: Int) {
        repository.deleteMovie(username, movie, providerId)
    }
}

/**
 * Factory para instanciar el ViewModel con sus dependencias.
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

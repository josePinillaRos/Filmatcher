package com.josepinilla.proyectofinal.filmatcher.ui.playmatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.josepinilla.proyectofinal.filmatcher.data.Repository
import com.josepinilla.proyectofinal.filmatcher.models.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * PlayMatchViewModel
 * Lógica de negocio y de acceso a datos para PlayMatchActivity.
 *
 * @author Jose Pinilla
 */
class PlayMatchViewModel(
    private val repository: Repository,
    private val username: String,
    private var providerId: Int
) : ViewModel() {

    // Lista de películas en memoria y el índice actual
    private var moviesList = mutableListOf<Result>()
    private var currentMovieIndex = 0

    // Género seleccionado
    private var selectedGenre: Int = 0

    // Paginación
    private var page = 1
    private var totalPages = 1
    private var isLoading = false

    // StateFlow que expone la película actual para la Activity
    private val _currentMovie = MutableStateFlow<Result?>(null)
    val currentMovie: StateFlow<Result?> = _currentMovie

    /**
     * Llamado desde la Activity cuando se inicia o se cambia el providerId.
     * Se obtiene el total de páginas y se notifica a la Activity en caso de error.
     */
    fun loadTotalPages(
        onError: (String) -> Unit,
        onSuccess: (Int) -> Unit
    ) {
        viewModelScope.launch {
            try {
                totalPages = repository.getTotalPages(providerId).totalPages ?: 1
                onSuccess(totalPages)
            } catch (e: Exception) {
                onError("Error al obtener el total de páginas: ${e.message}")
            }
        }
    }

    /**
     * Descarga una página de películas y las añade a la lista local.
     * Llamado desde la Activity cuando se quiere fetch la página 1 o siguiente.
     */
    fun fetchPage(pageToLoad: Int, showNextImmediately: Boolean = false, onError: (String) -> Unit) {
        if (isLoading) return
        isLoading = true

        viewModelScope.launch {
            try {
                // Obtener películas de la página
                val newMovies = repository.fetchMovies(providerId, pageToLoad, selectedGenre, username)

                if (newMovies.isEmpty()) {
                    // Si no llegan películas en esta página y quedan más páginas, intentamos la siguiente
                    if (pageToLoad < totalPages) {
                        isLoading = false
                        fetchPage(pageToLoad + 1, showNextImmediately, onError)
                    } else {
                        isLoading = false
                        onError("No hay más películas disponibles.")
                    }
                } else {
                    // Añadir películas nuevas a la lista
                    val oldSize = moviesList.size
                    moviesList.addAll(newMovies)
                    page = pageToLoad

                    // Si es la primera vez que se cargan películas
                    if (oldSize == 0) {
                        currentMovieIndex = 0
                        _currentMovie.value = moviesList[currentMovieIndex]
                    } else if (showNextImmediately) {
                        currentMovieIndex = oldSize
                        _currentMovie.value = moviesList[currentMovieIndex]
                    }

                    isLoading = false
                }
            } catch (e: Exception) {
                isLoading = false
                onError("Error al obtener películas: ${e.message}")
            }
        }
    }

    /**
     * Llamado cuando el usuario cambia de género en el spinner.
     */
    fun setSelectedGenre(genreId: Int) {
        selectedGenre = genreId
        // Reseteo de la lista de películas y paginación
        moviesList.clear()
        currentMovieIndex = 0
        page = 1
    }

    /**
     * Para pasar a la siguiente película.
     */
    fun nextMovie(onNoMoreMovies: () -> Unit) {
        currentMovieIndex++
        if (currentMovieIndex < moviesList.size) {
            _currentMovie.value = moviesList[currentMovieIndex]
        } else {
            // Si se llega al final y aún quedan páginas
            if (page < totalPages) {
                // fetchPage con showNextImmediately = true para mostrar la siguiente película
                fetchPage(page + 1, true) {
                    onNoMoreMovies()
                }
            } else {
                onNoMoreMovies()
            }
        }
    }

    /**
     * Guarda la película en la BD local como "vista".
     */
    fun saveWatchedMovie(movie: Result) {
        viewModelScope.launch {
            repository.insertWatchedMovie(movie)
        }
    }

    /**
     * Guarda la película aceptada en Firestore.
     */
    fun saveAcceptedMovie(movie: Result, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.saveAcceptedMovie(username, movie, providerId)
                onSuccess("Película '${movie.title}' guardada")
            } catch (e: Exception) {
                onError("Error al guardar '${movie.title}'")
            }
        }
    }

    /**
     * Elimina la película rechazada de Firestore.
     */
    fun deleteMovie(movie: Result, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteMovie(username, movie, providerId)
            } catch (e: Exception) {
                onError("Error al eliminar '${movie.title}'")
            }
        }
    }


    fun resetMoviesByUser() {
        viewModelScope.launch {
            repository.resetMoviesByUser(username, providerId)
        }
    }
}

/**
 * PlayMatchViewModelFactory
 * Factoría para instanciar el ViewModel con sus dependencias.
 */
@Suppress("UNCHECKED_CAST")
class PlayMatchViewModelFactory(
    private val repository: Repository,
    private val username: String,
    private val providerId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlayMatchViewModel(repository, username, providerId) as T
    }
}

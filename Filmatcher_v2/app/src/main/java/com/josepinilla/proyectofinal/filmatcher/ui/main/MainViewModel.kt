package com.josepinilla.proyectofinal.filmatcher.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.josepinilla.proyectofinal.filmatcher.data.Repository
import com.josepinilla.proyectofinal.filmatcher.models.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

//TODO no se si se usa aun
class MainViewModel(private val repository: Repository) : ViewModel() {

    // Estado para almacenar la lista de películas (Result) que llegan de la API
    private val _moviesFromApi = MutableStateFlow<List<Result>>(emptyList())
    val moviesFromApi = _moviesFromApi.asStateFlow()

    /**
     * cargarPeliculas
     * llama al repository para obtener pelis de un watchProvider/página
     */
    fun cargarPeliculas(watchProvider: Int, page: Int) {
        viewModelScope.launch {
            val results = repository.fetchMovies(watchProvider, page)
            _moviesFromApi.value = results
        }
    }
}

/**
 * MainViewModelFactory
 */
class MainViewModelFactory(
    private val repository: Repository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository) as T
    }
}

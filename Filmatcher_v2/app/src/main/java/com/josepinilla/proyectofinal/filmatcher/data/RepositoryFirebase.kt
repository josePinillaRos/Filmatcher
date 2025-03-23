package com.josepinilla.proyectofinal.filmatcher.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.josepinilla.proyectofinal.filmatcher.R
import com.josepinilla.proyectofinal.filmatcher.models.Result
import kotlinx.coroutines.tasks.await

/**
 * RepositoryFirebase
 * Clase que se encarga de interactuar con Firestore
 *
 * @property db Instancia de Firestore
 *
 * @author Jose Pinilla
 */
class RepositoryFirebase {
    private val db = FirebaseFirestore.getInstance()

    /**
     * saveAcceptedMovie
     * Guarda una película aceptada en Firestore: user_movies/username/movies/movieId
     */
     fun saveAcceptedMovie(username: String, movie: Result, providerId: Int) {
         // la clave del documento es la concatenación de movieId y providerId para así poder guardar
         // la misma película si es aceptada en diferentes plataformas
        val docId = "${movie.id}_$providerId"
        val docData = hashMapOf(
            "movieId" to movie.id,
            "title" to (movie.title ?: R.string.txt_no_title.toString()),
            "posterPath" to (movie.posterPath ?: ""),
            "releaseDate" to (movie.releaseDate ?: R.string.txt_year_unknown.toString()),
            "genreIds" to (movie.genreIds ?: emptyList()),
            "overview" to (movie.overview ?: R.string.txt_no_sinopsis.toString()),
            "providerId" to providerId,
            "timestamp" to System.currentTimeMillis()
        )

        // Guardar en Firestore
        db.collection("user_movies")
            .document(username)
            .collection("movies")
            .document(docId)
            .set(docData)
    }

    /**
     * deleteMovie
     * Elimina las películas de un usuario de una plataforma en Firestore
     */
    fun deleteMovie(username: String, movie: Result, providerId: Int) {
        // se busca por clave primaria
        val docId = "${movie.id}_$providerId"
        // Eliminar de Firestore
        db.collection("user_movies")
            .document(username)
            .collection("movies")
            .document(docId)
            .delete()
    }

    /**
     * fetchCommonMovies
     * Retorna la lista de de películas en común entre dos usuarios
     */
    suspend fun fetchCommonResults(userA: String, userB: String): List<Result> {
        // Recoge las películas de userA
        val queryUserA = db.collection("user_movies")
            .document(userA)
            .collection("movies")
            .get()
            .await()
        val filmsUserA = queryUserA.documents.mapNotNull { it.getLong("movieId")?.toInt() }.toSet()

        // Peliculas de userB
        val queryUserB = db.collection("user_movies")
            .document(userB)
            .collection("movies")
            .get()
            .await()

        // filtrar las películas de userB que también tiene userA
        val commonDocs = queryUserB.documents.filter { doc ->
            val movieId = doc.getLong("movieId")?.toInt()
            movieId != null && filmsUserA.contains(movieId)
        }

        // 3) Convertir Firestore docs a Result
        return commonDocs.mapNotNull { doc ->
            mapFirestoreToResult(doc)
        }
    }

    /**
     * fetchUserMovies
     * Retorna la lista de películas de un usuario
     */
    suspend fun fetchUserMovies(username: String): List<Result> {
        return try {
            val querySnapshot = db.collection("user_movies")
                .document(username)
                .collection("movies")
                .get()
                .await()

            querySnapshot.documents.mapNotNull { doc -> mapFirestoreToResult(doc) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * mapFirestoreToResult
     * Convierte un documento Firestore en un objeto Result
     */
    private fun mapFirestoreToResult(doc: com.google.firebase.firestore.DocumentSnapshot): Result? {
        val movieId = doc.getLong("movieId")?.toInt() ?: return null
        val title = doc.getString("title") ?: R.string.txt_no_title.toString()
        val posterPath = doc.getString("posterPath") ?: ""
        val releaseDate = doc.getString("releaseDate") ?: R.string.txt_year_unknown.toString()
        val overview = doc.getString("overview") ?: R.string.txt_no_sinopsis.toString()
        val providerId = doc.getLong("providerId")?.toInt() ?: 0

        val genreIds = (doc.get("genreIds") as? List<*>) // Recuperar la lista de géneros sin tipo definido
            ?.mapNotNull { it as? Long } // Filtrar solo los elementos que sean Long
            ?.map { it.toInt() } // Convertir cada Long a Int
            ?: emptyList() // Si el campo no existe, retornar lista vacía

        return Result(
            id = movieId,
            providerId = providerId,
            title = title,
            posterPath = posterPath,
            releaseDate = releaseDate,
            overview = overview,
            genreIds = genreIds
        )
    }

    /**
     * getUserByUsername
     * Obtiene un usuario por su nombre de usuario
     * Incluye el campo "username" y la password en el documento
     */
    suspend fun getUserByUsername(username: String): QuerySnapshot? {
        return try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()
        } catch (e: Exception) {
            Log.e("FIREBASE_ERROR", "Error al obtener usuario: ${e.message}", e)
            null
        }
    }

    /**
     * registerUser
     * Registra un usuario en Firestore
     */
    suspend fun registerUser(username: String, hashedPassword: String): Boolean {
        return try {
            // Verificar si el usuario ya existe
            val existingUser = getUserByUsername(username)
            // Si el usuario ya existe, retornar false
            if (existingUser != null && !existingUser.isEmpty) {
                return false // Usuario ya existe
            }

            // Crear un nuevo usuario (username, password)
            val user = hashMapOf(
                "username" to username,
                "password" to hashedPassword
            )

            // Guardar en Firestore
            db.collection("users")
                .add(user)
                .await()

            true // Registro exitoso
        } catch (e: Exception) {
            Log.e("FIREBASE_ERROR", "Error al registrar usuario: ${e.message}", e)
            false
        }
    }
}
package com.example.gymtrackerviews // Asegúrate que es tu paquete

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow // Importante para observar cambios

// @Dao marca esta interfaz para que Room sepa que aquí definimos operaciones de base de datos.
@Dao
interface WorkoutDao {

    // @Insert define una función para insertar datos.
    // 'suspend' significa que debe llamarse desde una corutina (para no bloquear la app).
    // Devuelve el ID del nuevo workout insertado.
    @Insert
    suspend fun insertWorkout(workout: Workout): Long

    // @Query define una consulta SQL personalizada para leer datos.
    // Esta obtiene un workout específico por su ID.
    // Devuelve un 'Flow', que permite observar los datos: si cambian en la BD, el Flow lo notificará automáticamente.
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun getWorkoutById(workoutId: Long): Flow<Workout?> // Puede ser null si no existe

    // Esta consulta obtiene todos los workouts de la tabla.
    // 'ORDER BY start_time DESC' los ordena del más nuevo al más viejo.
    // También devuelve un Flow para observar la lista completa.
    @Query("SELECT * FROM workouts ORDER BY start_time DESC")
    fun getAllWorkouts(): Flow<List<Workout>>

    // Podríamos añadir funciones con @Update o @Delete si las necesitáramos más adelante.
}
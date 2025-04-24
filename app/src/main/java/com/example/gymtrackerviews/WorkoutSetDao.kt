package com.example.gymtrackerviews // Tu paquete

import androidx.room.Dao
import androidx.room.Delete // Importamos @Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetDao {

    @Insert
    suspend fun insertSet(set: WorkoutSet): Long

    @Query("SELECT * FROM workout_sets WHERE workout_id = :workoutId ORDER BY timestamp ASC")
    fun getSetsForWorkout(workoutId: Long): Flow<List<WorkoutSet>>

    // 👇 --- FUNCIÓN AÑADIDA --- 👇
    // Room generará el código para borrar el objeto WorkoutSet que le pases.
    @Delete
    suspend fun deleteSet(set: WorkoutSet)
    // 👆 --- FIN FUNCIÓN AÑADIDA --- 👇

    // Podríamos añadir @Update aquí más tarde
}
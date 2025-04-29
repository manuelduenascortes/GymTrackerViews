package com.example.gymtrackerviews // Tu paquete

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction // Importar Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insertWorkout(workout: Workout): Long

    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun getWorkoutFlowById(workoutId: Long): Flow<Workout?> // Sin cambios aquí

    // 👇 --- Consulta MODIFICADA para devolver WorkoutSummary --- 👇
    @Transaction // Buena práctica para consultas que pueden tocar múltiples tablas o relaciones
    @Query("""
        SELECT workouts.*, COUNT(workout_sets.id) as setCount
        FROM workouts
        LEFT JOIN workout_sets ON workouts.id = workout_sets.workout_id
        GROUP BY workouts.id
        ORDER BY workouts.start_time DESC
    """)
    fun getAllWorkoutSummaries(): Flow<List<WorkoutSummary>>
    // 👆 --- FIN Consulta MODIFICADA --- 👆

    @Delete
    suspend fun deleteWorkout(workout: Workout) // Sin cambios aquí

    @Update
    suspend fun updateWorkout(workout: Workout) // Sin cambios aquí
}

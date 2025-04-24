package com.example.gymtrackerviews // Tu paquete

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insertWorkout(workout: Workout): Long

    // Función para obtener un Workout específico por su ID como un Flow
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun getWorkoutFlowById(workoutId: Long): Flow<Workout?> // Devuelve Flow (puede ser null)

    // Función para obtener todos los workouts (ya la teníamos)
    @Query("SELECT * FROM workouts ORDER BY start_time DESC")
    fun getAllWorkouts(): Flow<List<Workout>>

}
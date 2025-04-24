package com.example.gymtrackerviews // Tu paquete

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insertWorkout(workout: Workout): Long

    // Añadimos una función para obtener un Workout por ID (la usaremos en el DetailFragment)
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun getWorkoutFlowById(workoutId: Long): Flow<Workout?> // Devuelve Flow

    @Query("SELECT * FROM workouts ORDER BY start_time DESC")
    fun getAllWorkouts(): Flow<List<Workout>>

}
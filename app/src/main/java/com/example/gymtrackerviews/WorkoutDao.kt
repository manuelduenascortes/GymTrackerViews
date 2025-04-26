package com.example.gymtrackerviews // Tu paquete

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insertWorkout(workout: Workout): Long

    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun getWorkoutFlowById(workoutId: Long): Flow<Workout?>

    @Query("SELECT * FROM workouts ORDER BY start_time DESC")
    fun getAllWorkouts(): Flow<List<Workout>>

    @Delete
    suspend fun deleteWorkout(workout: Workout)
}
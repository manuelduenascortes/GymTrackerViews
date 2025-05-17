package com.example.gymtrackerviews // Tu paquete

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetDao {

    @Insert
    suspend fun insertSet(set: WorkoutSet): Long

    // Este método devuelve un Flow, útil para WorkoutDetailViewModel
    @Query("SELECT * FROM workout_sets WHERE workout_id = :workoutId ORDER BY timestamp ASC")
    fun getSetsForWorkout(workoutId: Long): Flow<List<WorkoutSet>>

    // NUEVO MÉTODO SUSPEND PARA ESTADÍSTICAS: Obtener lista de series para un workout
    @Query("SELECT * FROM workout_sets WHERE workout_id = :workoutId")
    suspend fun getSetListForWorkout(workoutId: Long): List<WorkoutSet>

    @Delete
    suspend fun deleteSet(set: WorkoutSet)

    @Update
    suspend fun updateSet(set: WorkoutSet)
}

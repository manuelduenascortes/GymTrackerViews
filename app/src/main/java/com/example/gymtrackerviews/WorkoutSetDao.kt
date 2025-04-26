package com.example.gymtrackerviews // Tu paquete

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update // Importamos @Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetDao {

    @Insert
    suspend fun insertSet(set: WorkoutSet): Long

    @Query("SELECT * FROM workout_sets WHERE workout_id = :workoutId ORDER BY timestamp ASC")
    fun getSetsForWorkout(workoutId: Long): Flow<List<WorkoutSet>>

    @Delete
    suspend fun deleteSet(set: WorkoutSet)

    // 👇 --- FUNCIÓN AÑADIDA --- 👇
    // Room actualizará la fila que tenga el mismo PrimaryKey que el objeto 'set' que le pases.
    @Update
    suspend fun updateSet(set: WorkoutSet)
    // 👆 --- FIN FUNCIÓN AÑADIDA --- 👆
}
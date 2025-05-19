package com.example.gymtrackerviews // O tu paquete de DAOs

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// TODO: Asegúrate de importar tu entidad Exercise si está en otro paquete
// import com.example.gymtrackerviews.model.Exercise

@Dao
interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ignorar si el ejercicio ya existe (basado en nombre, podríamos añadir un índice único)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllExercises(exercises: List<Exercise>)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    fun getExerciseById(exerciseId: Long): Flow<Exercise?>

    @Query("SELECT * FROM exercises WHERE name LIKE :query ORDER BY name ASC")
    fun searchExercisesByName(query: String): Flow<List<Exercise>>

    // Podríamos añadir más consultas, como obtener ejercicios por grupo muscular
    @Query("SELECT * FROM exercises WHERE muscle_group = :muscleGroup ORDER BY name ASC")
    fun getExercisesByMuscleGroup(muscleGroup: String): Flow<List<Exercise>>

    @Query("SELECT DISTINCT muscle_group FROM exercises WHERE muscle_group IS NOT NULL ORDER BY muscle_group ASC")
    fun getAllMuscleGroups(): Flow<List<String>>
}

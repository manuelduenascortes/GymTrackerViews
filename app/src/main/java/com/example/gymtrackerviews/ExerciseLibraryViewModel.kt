package com.example.gymtrackerviews.ui.library // Asegúrate que este es el paquete correcto

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtrackerviews.Exercise // TODO: Ajusta el import si tu entidad Exercise está en otro paquete (ej. com.example.gymtrackerviews.model.Exercise)
import com.example.gymtrackerviews.ExerciseDao // TODO: Ajusta el import si tu ExerciseDao está en otro paquete (ej. com.example.gymtrackerviews.data.ExerciseDao)
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExerciseLibraryViewModel(private val exerciseDao: ExerciseDao) : ViewModel() {

    val allExercises: StateFlow<List<Exercise>> = exerciseDao.getAllExercises()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun insertExercise(exerciseName: String, muscleGroup: String?, description: String?) {
        if (exerciseName.isBlank()) {
            Log.w("ExerciseLibraryVM", "Exercise name cannot be blank.")
            return
        }
        val newExercise = Exercise(
            name = exerciseName.trim(),
            muscleGroup = if (muscleGroup.isNullOrBlank()) null else muscleGroup.trim(),
            description = if (description.isNullOrBlank()) null else description.trim()
        )
        viewModelScope.launch {
            try {
                exerciseDao.insertExercise(newExercise)
                Log.d("ExerciseLibraryVM", "Ejercicio insertado: ${newExercise.name}")
            } catch (e: Exception) {
                Log.e("ExerciseLibraryVM", "Error al insertar ejercicio: ${newExercise.name}", e)
            }
        }
    }

    fun updateExercise(exercise: Exercise) {
        if (exercise.name.isBlank()) {
            Log.w("ExerciseLibraryVM", "Exercise name cannot be blank for update.")
            return
        }
        viewModelScope.launch {
            try {
                exerciseDao.updateExercise(exercise)
                Log.d("ExerciseLibraryVM", "Ejercicio actualizado: ${exercise.name}")
            } catch (e: Exception) {
                Log.e("ExerciseLibraryVM", "Error al actualizar ejercicio: ${exercise.name}", e)
            }
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            try {
                exerciseDao.deleteExercise(exercise)
                Log.d("ExerciseLibraryVM", "Ejercicio borrado: ${exercise.name}")
            } catch (e: Exception) {
                Log.e("ExerciseLibraryVM", "Error al borrar ejercicio: ${exercise.name}", e)
            }
        }
    }
}

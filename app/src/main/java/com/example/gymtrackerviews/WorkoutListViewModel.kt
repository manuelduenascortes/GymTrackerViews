package com.example.gymtrackerviews // Tu paquete

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

// TODO: Asegúrate de que Workout, WorkoutSummary y WorkoutDao están importados correctamente
// import com.example.gymtrackerviews.model.Workout
// import com.example.gymtrackerviews.model.WorkoutSummary
// import com.example.gymtrackerviews.data.WorkoutDao

class WorkoutListViewModel(private val workoutDao: WorkoutDao) : ViewModel() {

    val allWorkoutSummaries: StateFlow<List<WorkoutSummary>> = workoutDao.getAllWorkoutSummaries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    /**
     * Inserta un nuevo workout con un nombre opcional y devuelve su ID.
     * Esta es una función 'suspend' porque la inserción en la base de datos es asíncrona.
     * Devuelve el ID del workout insertado, o -1L si hubo un error.
     */
    suspend fun insertNewWorkoutWithNameAndGetId(workoutName: String?): Long {
        // Si el nombre está vacío, lo tratamos como nulo para la base de datos.
        val nameToSave = if (workoutName.isNullOrBlank()) null else workoutName

        val newWorkout = Workout(
            name = nameToSave, // Usamos el nombre proporcionado
            startTime = Date()
        )
        var newWorkoutId = -1L
        try {
            newWorkoutId = workoutDao.insertWorkout(newWorkout)
            Log.d("WorkoutListViewModel", "Nuevo workout insertado con ID: $newWorkoutId, Nombre: $nameToSave")
        } catch (e: Exception) {
            Log.e("WorkoutListViewModel", "Error inserting workout with name", e)
        }
        return newWorkoutId
    }

    // Mantenemos el insertNewWorkout original por si lo usas en otro sitio,
    // aunque ahora el FAB debería usar el nuevo método con nombre.
    fun insertNewWorkout() {
        viewModelScope.launch {
            val newWorkout = Workout(startTime = Date()) // Crea un workout sin nombre
            try {
                workoutDao.insertWorkout(newWorkout)
            } catch (e: Exception) {
                Log.e("WorkoutListViewModel", "Error inserting workout (sin devolver ID y sin nombre)", e)
            }
        }
    }

    fun deleteWorkout(workoutSummary: WorkoutSummary) {
        viewModelScope.launch {
            try {
                workoutDao.deleteWorkout(workoutSummary.workout)
            } catch (e: Exception) {
                Log.e("WorkoutListViewModel", "Error deleting workout", e)
            }
        }
    }
}

class WorkoutListViewModelFactory(private val workoutDao: WorkoutDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutListViewModel(workoutDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

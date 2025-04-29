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

class WorkoutListViewModel(private val workoutDao: WorkoutDao) : ViewModel() {

    // 👇 --- StateFlow ahora contiene List<WorkoutSummary> --- 👇
    val allWorkoutSummaries: StateFlow<List<WorkoutSummary>> = workoutDao.getAllWorkoutSummaries() // Llama a la nueva función del DAO
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList() // Valor inicial sigue siendo lista vacía
        )
    // 👆 --- FIN StateFlow MODIFICADO --- 👆

    // --- Funciones insert y delete usan el objeto Workout interno ---
    fun insertNewWorkout() {
        viewModelScope.launch {
            val newWorkout = Workout(startTime = Date())
            try {
                workoutDao.insertWorkout(newWorkout)
            } catch (e: Exception) {
                Log.e("WorkoutListViewModel", "Error inserting workout", e)
            }
        }
    }

    // Recibe WorkoutSummary pero solo necesita el Workout para borrar
    fun deleteWorkout(workoutSummary: WorkoutSummary) {
        viewModelScope.launch {
            try {
                // Pasamos solo el objeto workout interno al DAO
                workoutDao.deleteWorkout(workoutSummary.workout)
            } catch (e: Exception) {
                Log.e("WorkoutListViewModel", "Error deleting workout", e)
            }
        }
    }
    // --- Fin Funciones ---
} // Fin Clase ViewModel

// --- Factory (sin cambios) ---
class WorkoutListViewModelFactory(private val workoutDao: WorkoutDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutListViewModel(workoutDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

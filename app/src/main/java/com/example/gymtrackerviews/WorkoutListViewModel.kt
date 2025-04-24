package com.example.gymtrackerviews // Tu paquete

import android.util.Log // Asegúrate que está importado Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

class WorkoutListViewModel(private val workoutDao: WorkoutDao) : ViewModel() {

    val allWorkouts: StateFlow<List<Workout>> = workoutDao.getAllWorkouts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

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

    // 👇 --- FUNCIÓN AÑADIDA --- 👇
    fun deleteWorkout(workout: Workout) {
        viewModelScope.launch {
            try {
                workoutDao.deleteWorkout(workout)
            } catch (e: Exception) {
                Log.e("WorkoutListViewModel", "Error deleting workout", e)
                // Podríamos exponer un estado de error si quisiéramos
            }
        }
    }
    // 👆 --- FIN FUNCIÓN AÑADIDA --- 👆
}

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
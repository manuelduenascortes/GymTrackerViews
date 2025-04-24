package com.example.gymtrackerviews // Tu paquete

import android.util.Log // <--- ¡¡IMPORT AÑADIDO!! ---
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

class WorkoutDetailViewModel(
    private val workoutDao: WorkoutDao,
    private val workoutSetDao: WorkoutSetDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val workoutId: Long = savedStateHandle.get<Long>("workoutId") ?: 0L

    val workoutDetails: StateFlow<Workout?> = workoutDao.getWorkoutFlowById(workoutId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    val workoutSets: StateFlow<List<WorkoutSet>> = workoutSetDao.getSetsForWorkout(workoutId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun insertSet(exerciseName: String, reps: Int, weight: Double) {
        val newSet = WorkoutSet(
            workoutId = workoutId,
            exerciseName = exerciseName,
            repetitions = reps,
            weight = weight
        )
        viewModelScope.launch {
            try {
                workoutSetDao.insertSet(newSet)
            } catch (e: Exception) {
                // Ahora sí reconoce Log.e
                Log.e("WorkoutDetailViewModel", "Error inserting set", e)
            }
        }
    }

    fun deleteSet(set: WorkoutSet) {
        viewModelScope.launch {
            try {
                workoutSetDao.deleteSet(set)
            } catch (e: Exception) {
                // Ahora sí reconoce Log.e
                Log.e("WorkoutDetailViewModel", "Error deleting set", e)
            }
        }
    }

    companion object {
        fun Factory(workoutDao: WorkoutDao, workoutSetDao: WorkoutSetDao): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras
                ): T {
                    if (modelClass.isAssignableFrom(WorkoutDetailViewModel::class.java)) {
                        val savedStateHandle = extras.createSavedStateHandle()
                        return WorkoutDetailViewModel(workoutDao, workoutSetDao, savedStateHandle) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
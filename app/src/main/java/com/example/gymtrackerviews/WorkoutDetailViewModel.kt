package com.example.gymtrackerviews // Tu paquete

import android.util.Log
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

// --- INICIO CLASE VIEWMODEL ---
class WorkoutDetailViewModel(
    private val workoutDao: WorkoutDao,
    private val workoutSetDao: WorkoutSetDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // --- Propiedades ---
    val workoutId: Long = savedStateHandle.get<Long>("workoutId") ?: 0L

    // StateFlow para los detalles del Workout actual
    val workoutDetails: StateFlow<Workout?> = workoutDao.getWorkoutFlowById(workoutId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    // StateFlow con el Mapa Agrupado de Series
    val groupedWorkoutSets: StateFlow<Map<String, List<WorkoutSet>>> = workoutSetDao.getSetsForWorkout(workoutId)
        .map { setsList ->
            setsList.groupBy { it.exerciseName } // Agrupa por nombre
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyMap() // Mapa vacío inicialmente
        )

    // StateFlow para saber si el workout ha terminado
    val isWorkoutFinished: StateFlow<Boolean> = workoutDetails
        .map { currentWorkout -> currentWorkout?.endTime != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = false
        )

    // --- Funciones ---
    fun insertSet(exerciseName: String, reps: Int, weight: Double) {
        val newSet = WorkoutSet(
            workoutId = workoutId, exerciseName = exerciseName, repetitions = reps, weight = weight
        )
        viewModelScope.launch {
            try { workoutSetDao.insertSet(newSet) }
            catch (e: Exception) { Log.e("WorkoutDetailViewModel", "Error inserting set", e) }
        }
    }

    fun deleteSet(set: WorkoutSet) {
        viewModelScope.launch {
            try { workoutSetDao.deleteSet(set) }
            catch (e: Exception) { Log.e("WorkoutDetailViewModel", "Error deleting set", e) }
        }
    }

    fun updateSet(set: WorkoutSet) {
        viewModelScope.launch {
            try {
                if (set.workoutId == workoutId) { workoutSetDao.updateSet(set) }
                else { Log.e("WorkoutDetailViewModel", "Attempted to update set with wrong workoutId!") }
            } catch (e: Exception) { Log.e("WorkoutDetailViewModel", "Error updating set", e) }
        }
    }

    // Función finishWorkout que ACEPTA las notas
    fun finishWorkout(currentNotes: String?) {
        viewModelScope.launch {
            val currentWorkout = workoutDetails.value
            if (currentWorkout != null && currentWorkout.endTime == null) {
                val finishedWorkout = currentWorkout.copy(
                    endTime = Date(),
                    notes = currentNotes?.trim() // Guarda notas y hora de fin
                )
                try {
                    workoutDao.updateWorkout(finishedWorkout) // Una sola actualización
                    Log.d("WorkoutDetailViewModel", "Workout finished and notes updated.")
                } catch (e: Exception) {
                    Log.e("WorkoutDetailViewModel", "Error finishing workout", e)
                }
            } else {
                Log.w("WorkoutDetailViewModel", "Workout already finished or not loaded.")
            }
        }
    }

    // Función saveNotes (llamada desde onPause)
    fun saveNotes(notes: String?) {
        val currentWorkout = workoutDetails.value
        if (currentWorkout != null && currentWorkout.endTime == null) {
            if (currentWorkout.notes != notes?.trim()) {
                val updatedWorkout = currentWorkout.copy(notes = notes?.trim())
                viewModelScope.launch {
                    try {
                        workoutDao.updateWorkout(updatedWorkout)
                        Log.d("WorkoutDetailViewModel", "Notes saved via saveNotes for workout ID: ${currentWorkout.id}")
                    } catch (e: Exception) {
                        Log.e("WorkoutDetailViewModel", "Error saving notes via saveNotes", e)
                    }
                }
            } else {
                Log.d("WorkoutDetailViewModel", "Notes unchanged in saveNotes, skipping update.")
            }
        } else {
            Log.w("WorkoutDetailViewModel", "Cannot save notes via saveNotes, workout finished or null.")
        }
    }

    // --- Factory ---
    companion object {
        fun Factory(workoutDao: WorkoutDao, workoutSetDao: WorkoutSetDao): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    if (modelClass.isAssignableFrom(WorkoutDetailViewModel::class.java)) {
                        val savedStateHandle = extras.createSavedStateHandle()
                        return WorkoutDetailViewModel(workoutDao, workoutSetDao, savedStateHandle) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    } // --- Fin companion object ---

} // --- FIN CLASE VIEWMODEL ---

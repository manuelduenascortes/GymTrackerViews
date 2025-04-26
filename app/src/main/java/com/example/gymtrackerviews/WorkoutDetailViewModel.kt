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
import kotlinx.coroutines.flow.map // Import map si no estaba
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
            initialValue = null // Inicia como null hasta que se cargue
        )

    // StateFlow para la lista de series de este workout
    val workoutSets: StateFlow<List<WorkoutSet>> = workoutSetDao.getSetsForWorkout(workoutId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList() // Inicia como lista vacía
        )

    // --- StateFlow para saber si el workout ha terminado ---
    // 👇 ASEGÚRATE DE QUE ESTA PROPIEDAD ESTÁ DEFINIDA CORRECTAMENTE 👇
    val isWorkoutFinished: StateFlow<Boolean> = workoutDetails // Se basa en workoutDetails
        .map { currentWorkout ->
            // Emite true si el workout existe y su endTime NO es null
            currentWorkout?.endTime != null
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = false // Asumimos que no está terminado al principio
        )
    // 👆 --- FIN isWorkoutFinished --- 👆


    // --- Funciones (Insert, Delete, Update Set, Finish Workout) ---
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

    fun finishWorkout() {
        viewModelScope.launch {
            val currentWorkout = workoutDetails.value
            if (currentWorkout != null && currentWorkout.endTime == null) {
                val finishedWorkout = currentWorkout.copy(endTime = Date())
                try { workoutDao.updateWorkout(finishedWorkout) }
                catch (e: Exception) { Log.e("WorkoutDetailViewModel", "Error finishing workout", e) }
            } else { Log.w("WorkoutDetailViewModel", "Workout already finished or not loaded.") }
        }
    }

    // --- Factory dentro del companion object ---
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

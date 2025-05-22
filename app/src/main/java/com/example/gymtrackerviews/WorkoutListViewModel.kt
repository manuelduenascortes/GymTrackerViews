package com.example.gymtrackerviews // Tu paquete

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat // Para formatear la fecha si se usa como nombre por defecto
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutListViewModel(private val workoutDao: WorkoutDao) : ViewModel() {

    private val _muscleGroupFilter = MutableStateFlow<String?>(null)
    val muscleGroupFilter: StateFlow<String?> = _muscleGroupFilter.asStateFlow()

    val allWorkoutSummaries: StateFlow<List<WorkoutSummary>> = _muscleGroupFilter
        .flatMapLatest { filter ->
            if (filter.isNullOrBlank()) {
                Log.d("WorkoutListVM", "Filtro nulo o vacío, obteniendo todos los summaries.")
                workoutDao.getAllWorkoutSummaries()
            } else {
                Log.d("WorkoutListVM", "Filtrando summaries por grupo muscular: $filter")
                workoutDao.getWorkoutSummariesByMuscleGroup(filter)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun setMuscleGroupFilter(muscleGroup: String?) {
        _muscleGroupFilter.value = if (muscleGroup.isNullOrBlank()) null else muscleGroup
        Log.d("WorkoutListVM", "Filtro establecido a: ${_muscleGroupFilter.value}")
    }

    suspend fun insertNewWorkoutWithNameAndGetId(workoutNameFromUser: String?, mainMuscleGroup: String?): Long {
        val finalWorkoutName: String?
        val muscleGroupToSave = if (mainMuscleGroup.isNullOrBlank()) null else mainMuscleGroup

        // <<< LÓGICA MODIFICADA PARA EL NOMBRE DEL WORKOUT >>>
        if (workoutNameFromUser.isNullOrBlank()) {
            // Si el usuario no introdujo un nombre
            finalWorkoutName = if (!muscleGroupToSave.isNullOrBlank()) {
                // Usar los grupos musculares como nombre si están definidos
                muscleGroupToSave
            } else {
                // Si tampoco hay grupos musculares, el nombre será null
                // (La UI mostrará "Entrenamiento sin nombre" o similar)
                // Opcionalmente, podrías poner un nombre por defecto basado en la fecha aquí:
                // val sdf = SimpleDateFormat("dd MMM yy", Locale.getDefault())
                // "Entrenamiento del ${sdf.format(Date())}"
                null
            }
        } else {
            // El usuario introdujo un nombre, usar ese
            finalWorkoutName = workoutNameFromUser.trim()
        }
        // <<< FIN DE LA LÓGICA MODIFICADA >>>

        val newWorkout = Workout(
            name = finalWorkoutName,
            mainMuscleGroup = muscleGroupToSave,
            startTime = Date()
        )
        var newWorkoutId = -1L
        try {
            newWorkoutId = workoutDao.insertWorkout(newWorkout)
            Log.d("WorkoutListViewModel", "Nuevo workout insertado con ID: $newWorkoutId, Nombre: $finalWorkoutName, Grupo: $muscleGroupToSave")
        } catch (e: Exception) {
            Log.e("WorkoutListViewModel", "Error inserting workout with name and muscle group", e)
        }
        return newWorkoutId
    }

    // Esta función ya no se usa activamente desde el FAB, pero se mantiene por si acaso.
    fun insertNewWorkout() {
        viewModelScope.launch {
            val newWorkout = Workout(startTime = Date()) // Se creará sin nombre y sin grupo muscular
            try {
                workoutDao.insertWorkout(newWorkout)
            } catch (e: Exception) {
                Log.e("WorkoutListViewModel", "Error inserting workout (sin devolver ID, nombre, ni grupo)", e)
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

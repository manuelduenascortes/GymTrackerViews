package com.example.gymtrackerviews // Tu paquete

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi // Necesario para flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest // Necesario para cambiar el Flow basado en otro
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

// TODO: Asegúrate de que Workout, WorkoutSummary y WorkoutDao están importados correctamente
// import com.example.gymtrackerviews.model.Workout
// import com.example.gymtrackerviews.model.WorkoutSummary
// import com.example.gymtrackerviews.data.WorkoutDao

@OptIn(ExperimentalCoroutinesApi::class) // Necesario para flatMapLatest
class WorkoutListViewModel(private val workoutDao: WorkoutDao) : ViewModel() {

    // StateFlow para el filtro de grupo muscular actual.
    // Null significa que no hay filtro (mostrar todos).
    private val _muscleGroupFilter = MutableStateFlow<String?>(null)
    val muscleGroupFilter: StateFlow<String?> = _muscleGroupFilter.asStateFlow()

    // allWorkoutSummaries ahora reacciona al _muscleGroupFilter
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

    /**
     * Establece el filtro de grupo muscular.
     * Pasa null o una cadena vacía para quitar el filtro.
     */
    fun setMuscleGroupFilter(muscleGroup: String?) {
        _muscleGroupFilter.value = if (muscleGroup.isNullOrBlank()) null else muscleGroup
        Log.d("WorkoutListVM", "Filtro establecido a: ${_muscleGroupFilter.value}")
    }


    suspend fun insertNewWorkoutWithNameAndGetId(workoutName: String?, mainMuscleGroup: String?): Long {
        val nameToSave = if (workoutName.isNullOrBlank()) null else workoutName
        val muscleGroupToSave = if (mainMuscleGroup.isNullOrBlank()) null else mainMuscleGroup

        val newWorkout = Workout(
            name = nameToSave,
            mainMuscleGroup = muscleGroupToSave,
            startTime = Date()
        )
        var newWorkoutId = -1L
        try {
            newWorkoutId = workoutDao.insertWorkout(newWorkout)
            Log.d("WorkoutListViewModel", "Nuevo workout insertado con ID: $newWorkoutId, Nombre: $nameToSave, Grupo: $muscleGroupToSave")
        } catch (e: Exception) {
            Log.e("WorkoutListViewModel", "Error inserting workout with name and muscle group", e)
        }
        return newWorkoutId
    }

    fun insertNewWorkout() { // Este método ya no se usa desde el FAB, pero lo mantenemos por si acaso
        viewModelScope.launch {
            val newWorkout = Workout(startTime = Date())
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

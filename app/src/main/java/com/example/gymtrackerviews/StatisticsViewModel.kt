package com.example.gymtrackerviews.ui.statistics

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtrackerviews.ExerciseDao
import com.example.gymtrackerviews.Workout
import com.example.gymtrackerviews.WorkoutDao
import com.example.gymtrackerviews.WorkoutSetDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Se mantiene la misma data class para los puntos del gráfico
data class ChartEntryData(val date: Date, val value: Float, val labelDetail: String? = null)

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(
    private val workoutDao: WorkoutDao,
    private val workoutSetDao: WorkoutSetDao,
    private val exerciseDao: ExerciseDao // <<< AÑADIDO
) : ViewModel() {

    // Para el gráfico de entrenamientos por semana
    private val _workoutsPerWeek = MutableStateFlow<List<ChartEntryData>>(emptyList())
    val workoutsPerWeek: StateFlow<List<ChartEntryData>> = _workoutsPerWeek.asStateFlow()

    // Para el desplegable de ejercicios
    private val _allExerciseNames = MutableStateFlow<List<String>>(emptyList())
    val allExerciseNames: StateFlow<List<String>> = _allExerciseNames.asStateFlow()

    // Para el ejercicio seleccionado en el desplegable
    private val _selectedExerciseName = MutableStateFlow<String?>(null)
    // val selectedExerciseName: StateFlow<String?> = _selectedExerciseName.asStateFlow() // No es necesario exponerlo si solo se usa internamente para el flatMapLatest

    // Para el gráfico de progreso del ejercicio seleccionado
    val selectedExerciseProgress: StateFlow<List<ChartEntryData>> = _selectedExerciseName
        .flatMapLatest { exerciseName ->
            if (exerciseName == null) {
                flowOf(emptyList()) // Lista vacía si no hay ejercicio seleccionado
            } else {
                // Obtener todos los workouts completados
                workoutDao.getAllWorkoutsForStats().map { allWorkouts ->
                    val completedWorkouts = allWorkouts.filter { it.endTime != null }
                    val progressData = mutableListOf<ChartEntryData>()

                    completedWorkouts.forEach { workout ->
                        val setsForWorkout = workoutSetDao.getSetListForWorkout(workout.id)
                        val setsForSelectedExercise = setsForWorkout.filter { it.exerciseName.equals(exerciseName, ignoreCase = true) }

                        if (setsForSelectedExercise.isNotEmpty()) {
                            // Calcular el peso máximo levantado para este ejercicio en este workout
                            val maxWeightInWorkout = setsForSelectedExercise.maxOfOrNull { it.weight } ?: 0.0
                            if (maxWeightInWorkout > 0) {
                                progressData.add(ChartEntryData(workout.startTime, maxWeightInWorkout.toFloat(), workout.name ?: ""))
                            }
                        }
                    }
                    progressData.sortedBy { it.date } // Ordenar por fecha
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    init {
        Log.d("StatisticsViewModel", "ViewModel initialized")
        loadInitialData()
        loadAllExerciseNamesForPicker()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            workoutDao.getAllWorkoutsForStats().collectLatest { workouts ->
                Log.d("StatisticsViewModel", "Total workouts fetched for weekly stats: ${workouts.size}")
                val completedWorkouts = workouts.filter { it.endTime != null }
                Log.d("StatisticsViewModel", "Completed workouts for weekly stats: ${completedWorkouts.size}")

                if (completedWorkouts.isNotEmpty()) {
                    calculateWorkoutsPerWeek(completedWorkouts)
                } else {
                    _workoutsPerWeek.value = emptyList()
                }
            }
        }
    }

    private fun calculateWorkoutsPerWeek(completedWorkouts: List<Workout>) {
        val calendar = Calendar.getInstance()
        val groupedByWeek = completedWorkouts.groupBy { workout ->
            calendar.time = workout.startTime // Usamos startTime para agrupar
            val year = calendar.get(Calendar.YEAR)
            val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
            // Crear una clave única para el año y la semana
            // Ajustar la semana para que tenga dos dígitos para un ordenamiento correcto si es necesario
            String.format(Locale.US, "%d-%02d", year, weekOfYear)
        }

        val chartData = groupedByWeek.map { (weekYearKey, workoutsInWeek) ->
            // Para la fecha del punto en el gráfico, tomamos el inicio de la semana
            // o la fecha del primer entrenamiento de esa semana.
            // Para simplificar, usamos la fecha del primer entrenamiento de la semana.
            calendar.time = workoutsInWeek.first().startTime
            // Ajustar al primer día de esa semana (Lunes)
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)

            ChartEntryData(calendar.time, workoutsInWeek.size.toFloat(), "Semana $weekYearKey")
        }.sortedBy { it.date }

        _workoutsPerWeek.value = chartData
        Log.d("StatisticsViewModel", "Workouts per week calculated. Count: ${chartData.size}")
    }

    private fun loadAllExerciseNamesForPicker() {
        viewModelScope.launch {
            exerciseDao.getAllExercises()
                .map { exercises -> exercises.map { it.name }.distinct().sorted() }
                .catch { e -> Log.e("StatisticsViewModel", "Error loading exercise names", e) }
                .collectLatest { names ->
                    _allExerciseNames.value = names
                    Log.d("StatisticsViewModel", "All exercise names loaded for picker: ${names.size}")
                }
        }
    }

    fun setSelectedExercise(exerciseName: String?) {
        Log.d("StatisticsViewModel", "Exercise selected: $exerciseName")
        _selectedExerciseName.value = exerciseName
    }
}

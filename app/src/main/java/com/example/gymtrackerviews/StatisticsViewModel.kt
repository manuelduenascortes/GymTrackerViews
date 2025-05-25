package com.example.gymtrackerviews.ui.statistics

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtrackerviews.ExerciseDao
import com.example.gymtrackerviews.Workout
import com.example.gymtrackerviews.WorkoutDao
import com.example.gymtrackerviews.WorkoutSet
import com.example.gymtrackerviews.WorkoutSetDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ChartEntryData(val date: Date, val value: Float, val labelDetail: String? = null)

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(
    private val workoutDao: WorkoutDao,
    private val workoutSetDao: WorkoutSetDao,
    private val exerciseDao: ExerciseDao
) : ViewModel() {

    private val _workoutsPerWeek = MutableStateFlow<List<ChartEntryData>>(emptyList())
    val workoutsPerWeek: StateFlow<List<ChartEntryData>> = _workoutsPerWeek.asStateFlow()

    private val _allExerciseNames = MutableStateFlow<List<String>>(emptyList())
    val allExerciseNames: StateFlow<List<String>> = _allExerciseNames.asStateFlow()

    private val _selectedExerciseName = MutableStateFlow<String?>(null)

    val selectedExerciseProgress: StateFlow<List<ChartEntryData>> = _selectedExerciseName
        .flatMapLatest { exerciseName ->
            if (exerciseName == null) {
                flowOf(emptyList())
            } else {
                workoutDao.getAllWorkoutsForStats().map { allWorkouts ->
                    val completedWorkouts = allWorkouts.filter { it.endTime != null }
                    val progressData = mutableListOf<ChartEntryData>()
                    completedWorkouts.forEach { workout ->
                        val setsForWorkout = workoutSetDao.getSetListForWorkout(workout.id)
                        val setsForSelectedExercise = setsForWorkout.filter { it.exerciseName.equals(exerciseName, ignoreCase = true) }
                        if (setsForSelectedExercise.isNotEmpty()) {
                            val maxWeightInWorkout = setsForSelectedExercise.maxOfOrNull { it.weight } ?: 0.0
                            if (maxWeightInWorkout > 0) {
                                progressData.add(ChartEntryData(workout.startTime, maxWeightInWorkout.toFloat(), workout.name ?: ""))
                            }
                        }
                    }
                    progressData.sortedBy { it.date }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _personalRecords = MutableStateFlow<List<PersonalRecordItem>>(emptyList())
    val personalRecords: StateFlow<List<PersonalRecordItem>> = _personalRecords.asStateFlow()

    init {
        Log.d("StatisticsViewModel", "ViewModel initialized")
        loadInitialData()
        loadAllExerciseNamesForPicker()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            workoutDao.getAllWorkoutsForStats().collectLatest { workouts ->
                Log.d("StatisticsViewModel", "Total workouts fetched: ${workouts.size}")
                val completedWorkouts = workouts.filter { it.endTime != null }
                Log.d("StatisticsViewModel", "Completed workouts: ${completedWorkouts.size}")

                if (completedWorkouts.isNotEmpty()) {
                    calculateWorkoutsPerWeek(completedWorkouts)
                    calculatePersonalRecords(completedWorkouts)
                } else {
                    _workoutsPerWeek.value = emptyList()
                    _personalRecords.value = emptyList()
                }
            }
        }
    }

    private fun calculateWorkoutsPerWeek(completedWorkouts: List<Workout>) {
        val calendar = Calendar.getInstance()
        val groupedByWeek = completedWorkouts.groupBy { workout ->
            calendar.time = workout.startTime
            val year = calendar.get(Calendar.YEAR)
            val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
            String.format(Locale.US, "%d-%02d", year, weekOfYear)
        }
        val chartData = groupedByWeek.map { (weekYearKey, workoutsInWeek) ->
            calendar.time = workoutsInWeek.first().startTime
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            ChartEntryData(calendar.time, workoutsInWeek.size.toFloat(), "Semana $weekYearKey")
        }.sortedBy { it.date }
        _workoutsPerWeek.value = chartData
        Log.d("StatisticsViewModel", "Workouts per week calculated. Count: ${chartData.size}")
    }

    private suspend fun calculatePersonalRecords(completedWorkouts: List<Workout>) {
        val allSetsFromCompletedWorkouts = mutableListOf<Pair<WorkoutSet, Workout>>()
        completedWorkouts.forEach { workout ->
            workoutSetDao.getSetListForWorkout(workout.id).forEach { set ->
                allSetsFromCompletedWorkouts.add(Pair(set, workout))
            }
        }

        if (allSetsFromCompletedWorkouts.isEmpty()) {
            _personalRecords.value = emptyList()
            Log.d("StatisticsViewModel", "No sets found to calculate personal records.")
            return
        }

        val records = allSetsFromCompletedWorkouts
            .groupBy { it.first.exerciseName }
            .mapNotNull { (exerciseName, setsWithWorkoutForExercise) ->
                var bestSetOverall: WorkoutSet? = null
                var workoutOfBestSet: Workout? = null

                setsWithWorkoutForExercise.forEach { pair ->
                    val currentSet = pair.first
                    val currentWorkout = pair.second

                    if (bestSetOverall == null || currentSet.weight > bestSetOverall!!.weight) {
                        bestSetOverall = currentSet
                        workoutOfBestSet = currentWorkout
                    } else if (currentSet.weight == bestSetOverall!!.weight) {
                        if (currentSet.repetitions > bestSetOverall!!.repetitions) {
                            bestSetOverall = currentSet
                            workoutOfBestSet = currentWorkout
                        }
                    }
                }

                if (bestSetOverall != null && workoutOfBestSet != null) {
                    PersonalRecordItem(
                        exerciseName = exerciseName,
                        maxWeight = bestSetOverall!!.weight,
                        repsAtMaxWeight = bestSetOverall!!.repetitions,
                        dateOfRecord = workoutOfBestSet!!.startTime,
                        workoutName = workoutOfBestSet!!.name
                    )
                } else {
                    null
                }
            }
            .sortedBy { it.exerciseName }

        _personalRecords.value = records
        Log.d("StatisticsViewModel", "Personal records calculated. Count: ${records.size}")
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
        Log.d("StatisticsViewModel", "Exercise selected for progress: $exerciseName")
        _selectedExerciseName.value = exerciseName
    }
}

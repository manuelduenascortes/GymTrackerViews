package com.example.gymtrackerviews.ui.statistics // O tu paquete de ViewModels

import android.util.Log // Asegúrate de tener este import
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtrackerviews.Workout
import com.example.gymtrackerviews.WorkoutDao
import com.example.gymtrackerviews.WorkoutSetDao
// TODO: Asegúrate de que el import para Workout es correcto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

// Data class para los puntos del gráfico
data class ChartEntryData(val date: Date, val value: Float, val workoutName: String? = null)

class StatisticsViewModel(
    private val workoutDao: WorkoutDao,
    private val workoutSetDao: WorkoutSetDao
) : ViewModel() {

    private val _seriesPerWorkout = MutableStateFlow<List<ChartEntryData>>(emptyList())
    val seriesPerWorkout: StateFlow<List<ChartEntryData>> = _seriesPerWorkout.asStateFlow()

    private val _volumePerWorkout = MutableStateFlow<List<ChartEntryData>>(emptyList())
    val volumePerWorkout: StateFlow<List<ChartEntryData>> = _volumePerWorkout.asStateFlow()

    init {
        Log.d("StatisticsViewModel", "ViewModel initialized")
        loadStatisticsData()
    }

    private fun loadStatisticsData() {
        viewModelScope.launch {
            workoutDao.getAllWorkoutsForStats().collectLatest { workouts ->
                Log.d("StatisticsViewModel", "Total workouts fetched: ${workouts.size}")
                val seriesDataList = mutableListOf<ChartEntryData>()
                val volumeDataList = mutableListOf<ChartEntryData>()

                // Procesar solo workouts completados (con endTime)
                val completedWorkouts = workouts.filter { it.endTime != null }
                Log.d("StatisticsViewModel", "Completed workouts found: ${completedWorkouts.size}")

                for (workout in completedWorkouts) {
                    val sets = workoutSetDao.getSetListForWorkout(workout.id) // suspend fun
                    val setCount = sets.size.toFloat()
                    var totalVolume = 0.0

                    Log.d("StatisticsViewModel", "Processing Workout ID: ${workout.id}, Name: ${workout.name ?: "N/A"}, StartTime: ${workout.startTime}, SetCount: $setCount")

                    sets.forEach { set ->
                        totalVolume += (set.repetitions * set.weight)
                        Log.d("StatisticsViewModel", "  Set: ${set.exerciseName}, Reps: ${set.repetitions}, Weight: ${set.weight}, Current TotalVolume: $totalVolume")
                    }

                    if (setCount > 0) {
                        seriesDataList.add(ChartEntryData(workout.startTime, setCount, workout.name))
                    }
                    if (totalVolume > 0) {
                        volumeDataList.add(ChartEntryData(workout.startTime, totalVolume.toFloat(), workout.name))
                        Log.d("StatisticsViewModel", "Added to volumeDataList - Workout ID: ${workout.id}, Volume: ${totalVolume.toFloat()}")
                    } else {
                        Log.d("StatisticsViewModel", "Volume for Workout ID ${workout.id} is 0 or less, not adding to chart.")
                    }
                }
                _seriesPerWorkout.value = seriesDataList
                _volumePerWorkout.value = volumeDataList
                Log.d("StatisticsViewModel", "Final seriesDataList size: ${seriesDataList.size}")
                Log.d("StatisticsViewModel", "Final volumeDataList size: ${volumeDataList.size}")
            }
        }
    }
}

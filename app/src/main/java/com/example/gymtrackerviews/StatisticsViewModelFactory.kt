package com.example.gymtrackerviews.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gymtrackerviews.ExerciseDao // Asegúrate que el import es correcto
import com.example.gymtrackerviews.WorkoutDao
import com.example.gymtrackerviews.WorkoutSetDao

class StatisticsViewModelFactory(
    private val workoutDao: WorkoutDao,
    private val workoutSetDao: WorkoutSetDao,
    private val exerciseDao: ExerciseDao // <<< AÑADIDO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatisticsViewModel(workoutDao, workoutSetDao, exerciseDao) as T // <<< AÑADIDO exerciseDao
        }
        throw IllegalArgumentException("Unknown ViewModel class for Statistics")
    }
}

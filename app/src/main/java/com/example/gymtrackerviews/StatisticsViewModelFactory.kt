package com.example.gymtrackerviews.ui.statistics // O el paquete donde esté tu factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gymtrackerviews.ExerciseDao
import com.example.gymtrackerviews.WorkoutDao
import com.example.gymtrackerviews.WorkoutSetDao

// Asegúrate de que el nombre de la clase es StatisticsViewModelFactory
class StatisticsViewModelFactory(
    private val workoutDao: WorkoutDao,
    private val workoutSetDao: WorkoutSetDao,
    private val exerciseDao: ExerciseDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatisticsViewModel(workoutDao, workoutSetDao, exerciseDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for Statistics")
    }
}

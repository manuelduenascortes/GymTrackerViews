package com.example.gymtrackerviews.ui.statistics // TODO: Asegúrate que este paquete coincide con StatisticsViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gymtrackerviews.WorkoutDao // TODO: Ajusta el import si es necesario
import com.example.gymtrackerviews.WorkoutSetDao // TODO: Ajusta el import si es necesario
// Si StatisticsViewModel está en este mismo paquete, no necesitas importarlo explícitamente.
// Si está en otro, necesitarías: import com.example.gymtrackerviews.paquete.del.viewmodel.StatisticsViewModel

class StatisticsViewModelFactory(
    private val workoutDao: WorkoutDao,
    private val workoutSetDao: WorkoutSetDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Aquí se referencia StatisticsViewModel. Debe poder encontrarla.
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatisticsViewModel(workoutDao, workoutSetDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for Statistics")
    }
}

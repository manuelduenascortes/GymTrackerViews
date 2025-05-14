package com.example.gymtrackerviews // O el paquete donde pongas tus ViewModels y Factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// TODO: Asegúrate de que los imports para WorkoutDao y WorkoutSetDao son correctos
// Ejemplo:
// import com.example.gymtrackerviews.data.WorkoutDao
// import com.example.gymtrackerviews.data.WorkoutSetDao
// import com.example.gymtrackerviews.WorkoutDetailViewModel // Asegúrate que este import es correcto si no está en el mismo paquete

class WorkoutDetailViewModelFactory(
    private val workoutId: Long,
    private val workoutDao: WorkoutDao,
    private val workoutSetDao: WorkoutSetDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutDetailViewModel(workoutId, workoutDao, workoutSetDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

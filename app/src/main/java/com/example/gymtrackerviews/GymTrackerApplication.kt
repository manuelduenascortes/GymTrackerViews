package com.example.gymtrackerviews // O tu paquete principal

import android.app.Application

// TODO: Asegúrate de que AppDatabase está importado correctamente
// import com.example.gymtrackerviews.data.AppDatabase // o donde esté tu AppDatabase

class GymTrackerApplication : Application() {
    // Usamos 'lazy' para que la base de datos y el DAO solo se creen cuando se necesiten por primera vez.
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    // Podrías exponer el DAO directamente si lo prefieres, aunque a veces es mejor
    // pasar la instancia de la base de datos y que cada ViewModel obtenga su DAO.
    // val workoutDao: WorkoutDao by lazy { database.workoutDao() }
}

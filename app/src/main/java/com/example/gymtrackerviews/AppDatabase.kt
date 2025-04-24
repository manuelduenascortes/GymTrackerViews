package com.example.gymtrackerviews // Asegúrate que es tu paquete

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.Date

// --- Convertidor de Tipos para Date ---
// Room no sabe guardar objetos 'Date' directamente, solo tipos básicos (números, texto...).
// Este convertidor le enseña a guardar un 'Date' como un número largo (Long) y viceversa.
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        // Convierte Long (milisegundos desde 1970) a Date
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        // Convierte Date a Long (milisegundos desde 1970)
        return date?.time
    }
}

// --- Clase Principal de la Base de Datos ---
// @Database define la clase como una base de datos Room.
// 'entities' lista todas las tablas (Entidades) que tendrá. Por ahora solo Workout.
// 'version' es importante para migraciones. Empezamos en 1.
// 'exportSchema = false' evita un warning de compilación por ahora.
@Database(entities = [Workout::class], version = 1, exportSchema = false)
// @TypeConverters le dice a Room que use nuestro DateConverter para manejar los tipos Date.
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    // Función abstracta que Room implementará para darnos acceso al DAO.
    abstract fun workoutDao(): WorkoutDao

    // --- Singleton Pattern ---
    // Esto asegura que solo exista UNA instancia de la base de datos en toda la app,
    // lo cual es importante para el rendimiento y evitar problemas.
    companion object {
        // '@Volatile' asegura que el valor de INSTANCE esté siempre actualizado para todos los hilos.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Función para obtener la instancia única de la base de datos.
        fun getDatabase(context: Context): AppDatabase {
            // Si ya existe una instancia, la devuelve.
            return INSTANCE ?: synchronized(this) {
                // Si no existe, la crea de forma segura (synchronized).
                val instance = Room.databaseBuilder(
                    context.applicationContext, // Usamos el contexto de la aplicación
                    AppDatabase::class.java,    // La clase de nuestra base de datos
                    "gym_tracker_database"      // Nombre del archivo de la base de datos en el dispositivo
                )
                    // Aquí añadiríamos .addMigrations(...) si cambiamos la versión en el futuro.
                    .build()
                INSTANCE = instance // Guarda la instancia creada
                instance // Devuelve la instancia
            }
        }
    }
}
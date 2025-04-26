package com.example.gymtrackerviews // Asegúrate que coincide con tu paquete

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.Date

// Convertidor (sin cambios)
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? { return value?.let { Date(it) } }
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? { return date?.time }
}

// Clase Database - Verifica la lista 'entities'
@Database(entities = [Workout::class, WorkoutSet::class], version = 2, exportSchema = false) // <-- ¿Está WorkoutSet::class aquí?
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutSetDao(): WorkoutSetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gym_tracker_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
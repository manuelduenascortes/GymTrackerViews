package com.example.gymtrackerviews // Tu paquete

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration // Importar Migration
import androidx.sqlite.db.SupportSQLiteDatabase // Importar SupportSQLiteDatabase
import java.util.Date

// --- Type Converter (sin cambios) ---
class DateConverter {
    @TypeConverter fun fromTimestamp(value: Long?): Date? { return value?.let { Date(it) } }
    @TypeConverter fun dateToTimestamp(date: Date?): Long? { return date?.time }
}

// --- Clase Principal de la Base de Datos (Actualizada) ---
// 👇 Incrementamos version a 3
@Database(entities = [Workout::class, WorkoutSet::class], version = 3, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutSetDao(): WorkoutSetDao

    companion object {
        // 👇 --- MIGRACIÓN AÑADIDA (de v2 a v3) --- 👇
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Le decimos a SQLite que añada la nueva columna a la tabla existente.
                // Es INTEGER porque guardamos las Dates como Long (timestamp).
                // DEFAULT NULL permite que las filas antiguas no den error.
                db.execSQL("ALTER TABLE workouts ADD COLUMN end_time INTEGER DEFAULT NULL")
            }
        }
        // 👆 --- FIN MIGRACIÓN --- 👆


        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gym_tracker_database"
                )
                    // 👇 Quitamos fallbackToDestructiveMigration si estaba
                    // .fallbackToDestructiveMigration()
                    // 👇 Añadimos nuestra migración específica
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
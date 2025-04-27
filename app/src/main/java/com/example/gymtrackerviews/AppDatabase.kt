package com.example.gymtrackerviews // Tu paquete

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Date

// --- Type Converter (sin cambios) ---
class DateConverter {
    @TypeConverter fun fromTimestamp(value: Long?): Date? { return value?.let { Date(it) } }
    @TypeConverter fun dateToTimestamp(date: Date?): Long? { return date?.time }
}

// --- Clase Principal de la Base de Datos (Actualizada a v4) ---
// 👇 Incrementamos version a 4
@Database(entities = [Workout::class, WorkoutSet::class], version = 4, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutSetDao(): WorkoutSetDao

    companion object {
        // --- Migración de v2 a v3 (la que ya teníamos) ---
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workouts ADD COLUMN end_time INTEGER DEFAULT NULL")
            }
        }

        // 👇 --- NUEVA MIGRACIÓN AÑADIDA (de v3 a v4) --- 👇
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Añadimos la columna 'notes' de tipo TEXT (para String) a la tabla 'workouts'
                // Permitimos que sea NULL para las filas antiguas
                db.execSQL("ALTER TABLE workouts ADD COLUMN notes TEXT DEFAULT NULL")
            }
        }
        // 👆 --- FIN NUEVA MIGRACIÓN --- 👆


        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gym_tracker_database"
                )
                    // 👇 Añadimos AMBAS migraciones en orden
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

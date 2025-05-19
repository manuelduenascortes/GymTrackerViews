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

// --- Clase Principal de la Base de Datos ---
// 👇 Incrementamos version a 7 y mantenemos Exercise::class
@Database(entities = [Workout::class, WorkoutSet::class, Exercise::class], version = 7, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutSetDao(): WorkoutSetDao
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workouts ADD COLUMN end_time INTEGER DEFAULT NULL")
            }
        }
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workouts ADD COLUMN notes TEXT DEFAULT NULL")
            }
        }
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workouts ADD COLUMN name TEXT DEFAULT NULL")
            }
        }
        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `exercises` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `muscle_group` TEXT,
                        `description` TEXT
                    )
                """)
            }
        }

        // 👇 --- NUEVA MIGRACIÓN AÑADIDA (de v6 a v7 para 'main_muscle_group' en 'workouts') --- 👇
        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Añadimos la columna 'main_muscle_group' de tipo TEXT a la tabla 'workouts'
                // Permitimos que sea NULL para las filas antiguas.
                db.execSQL("ALTER TABLE workouts ADD COLUMN main_muscle_group TEXT DEFAULT NULL")
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
                    // 👇 Añadimos TODAS las migraciones en orden, incluyendo la nueva MIGRATION_6_7
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigrationOnDowngrade() // Considera una estrategia diferente para producción
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

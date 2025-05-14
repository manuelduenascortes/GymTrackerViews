package com.example.gymtrackerviews // Tu paquete

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter // Asegúrate de tener este import si no está ya
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
// 👇 Incrementamos version a 5
@Database(entities = [Workout::class, WorkoutSet::class], version = 5, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutSetDao(): WorkoutSetDao // Asumo que tienes este DAO

    companion object {
        // Migración de v2 a v3 (la que ya tenías)
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workouts ADD COLUMN end_time INTEGER DEFAULT NULL")
            }
        }

        // Migración de v3 a v4 (la que ya tenías para 'notes')
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workouts ADD COLUMN notes TEXT DEFAULT NULL")
            }
        }

        // 👇 --- NUEVA MIGRACIÓN AÑADIDA (de v4 a v5 para 'name') --- 👇
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Añadimos la columna 'name' de tipo TEXT a la tabla 'workouts'
                // Permitimos que sea NULL para las filas antiguas y establecemos un valor por defecto si es necesario,
                // aunque al ser nullable, SQLite lo manejará bien.
                db.execSQL("ALTER TABLE workouts ADD COLUMN name TEXT DEFAULT NULL")
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
                    // 👇 Añadimos TODAS las migraciones en orden
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigrationOnDowngrade() // O la estrategia que prefieras
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

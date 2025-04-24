package com.example.gymtrackerviews // Tu paquete

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "workout_sets",
    foreignKeys = [ForeignKey(
        entity = Workout::class,
        parentColumns = ["id"],
        childColumns = ["workout_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "workout_id", index = true)
    val workoutId: Long,

    @ColumnInfo(name = "exercise_name")
    val exerciseName: String,

    @ColumnInfo(name = "repetitions")
    val repetitions: Int,

    @ColumnInfo(name = "weight")
    val weight: Double,

    @ColumnInfo(name = "timestamp")
    val timestamp: Date = Date()
)
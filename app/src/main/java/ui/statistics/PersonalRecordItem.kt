package com.example.gymtrackerviews.ui.statistics

import java.util.Date

data class PersonalRecordItem(
    val exerciseName: String,
    val maxWeight: Double,
    val repsAtMaxWeight: Int,
    val dateOfRecord: Date,
    val workoutName: String? // Nombre del workout donde se hizo el PR
)

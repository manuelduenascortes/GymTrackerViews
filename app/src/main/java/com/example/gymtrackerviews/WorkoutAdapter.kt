package com.example.gymtrackerviews // Tu paquete

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gymtrackerviews.databinding.ItemWorkoutBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class WorkoutAdapter(
    private val onItemClick: (WorkoutSummary) -> Unit,
    private val onDeleteClick: (WorkoutSummary) -> Unit
) : ListAdapter<WorkoutSummary, WorkoutAdapter.WorkoutViewHolder>(WorkoutSummaryDiffCallback()) {

    inner class WorkoutViewHolder(private val binding: ItemWorkoutBinding) : RecyclerView.ViewHolder(binding.root) {
        private var currentWorkoutSummary: WorkoutSummary? = null
        private val dateFormatShort = SimpleDateFormat("dd MMM yy, HH:mm", Locale.getDefault())
        private val dateFormatJustDate = SimpleDateFormat("dd MMM yy", Locale.getDefault())


        init {
            binding.root.setOnClickListener {
                currentWorkoutSummary?.let { summary -> onItemClick(summary) }
            }
            binding.buttonDeleteWorkout.setOnClickListener {
                currentWorkoutSummary?.let { summary -> onDeleteClick(summary) }
            }
        }

        fun bind(workoutSummary: WorkoutSummary) {
            currentWorkoutSummary = workoutSummary
            val workout = workoutSummary.workout

            // 1. Nombre del Entrenamiento o Fecha
            if (!workout.name.isNullOrBlank()) {
                binding.textViewWorkoutName.text = workout.name
                binding.textViewWorkoutStartTime.text = "Realizado el: ${dateFormatShort.format(workout.startTime)}"
                binding.textViewWorkoutStartTime.visibility = View.VISIBLE
            } else {
                binding.textViewWorkoutName.text = "Entrenamiento del ${dateFormatJustDate.format(workout.startTime)}"
                binding.textViewWorkoutStartTime.visibility = View.GONE // Ocultar si la fecha ya está en el título
            }

            // 2. Grupos Musculares
            if (!workout.mainMuscleGroup.isNullOrBlank()) {
                binding.textViewWorkoutMuscleGroups.text = workout.mainMuscleGroup
                binding.textViewWorkoutMuscleGroups.visibility = View.VISIBLE
            } else {
                binding.textViewWorkoutMuscleGroups.visibility = View.GONE
            }

            // 3. Estado/Duración y Series
            if (workout.endTime != null) {
                val durationMillis = workout.endTime.time - workout.startTime.time
                val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
                var durationString = ""
                if (hours > 0) { durationString += "${hours}h " }
                if (hours > 0 || minutes > 0) {
                    durationString += "${minutes}m"
                } else {
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
                    if (seconds > 0) {
                        durationString += "${seconds}s"
                    } else {
                        durationString = "< 1s" // Si es muy corto
                    }
                }
                binding.textViewWorkoutStatusDuration.text = "Duración: $durationString"
            } else {
                binding.textViewWorkoutStatusDuration.text = "En curso"
            }
            binding.textViewSetCount.text = "Series: ${workoutSummary.setCount}"


            // 4. Preview de Notas
            if (!workout.notes.isNullOrBlank()) {
                binding.textViewWorkoutNotesPreview.text = "Notas: ${workout.notes}"
                binding.textViewWorkoutNotesPreview.visibility = View.VISIBLE
            } else {
                binding.textViewWorkoutNotesPreview.visibility = View.GONE
            }

            // ID (sigue oculto)
            binding.textViewWorkoutId.text = "Workout ID: ${workout.id}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val binding = ItemWorkoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class WorkoutSummaryDiffCallback : DiffUtil.ItemCallback<WorkoutSummary>() {
    override fun areItemsTheSame(oldItem: WorkoutSummary, newItem: WorkoutSummary): Boolean {
        return oldItem.workout.id == newItem.workout.id
    }
    override fun areContentsTheSame(oldItem: WorkoutSummary, newItem: WorkoutSummary): Boolean {
        return oldItem == newItem
    }
}

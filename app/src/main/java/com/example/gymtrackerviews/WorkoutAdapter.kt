package com.example.gymtrackerviews // Tu paquete

import android.view.LayoutInflater
import android.view.View // Necesario para View.VISIBLE/GONE
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gymtrackerviews.databinding.ItemWorkoutBinding // Binding para item_workout.xml
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

// TODO: Asegúrate de que la clase WorkoutSummary y Workout están definidas y son accesibles.
// Asumo que WorkoutSummary tiene una propiedad 'workout: Workout' y 'setCount: Int'
// y que Workout tiene 'id', 'startTime', 'endTime', 'notes'.

class WorkoutAdapter(
    private val onItemClick: (WorkoutSummary) -> Unit, // Espera WorkoutSummary
    private val onDeleteClick: (WorkoutSummary) -> Unit  // Espera WorkoutSummary
) : ListAdapter<WorkoutSummary, WorkoutAdapter.WorkoutViewHolder>(WorkoutSummaryDiffCallback()) {

    inner class WorkoutViewHolder(private val binding: ItemWorkoutBinding) : RecyclerView.ViewHolder(binding.root) {
        private var currentWorkoutSummary: WorkoutSummary? = null

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

            val dateFormat = SimpleDateFormat("dd MMM yy, HH:mm:ss", Locale.getDefault())
            binding.textViewWorkoutStartTime.text = "Inicio: ${dateFormat.format(workout.startTime)}"

            if (workout.endTime != null) {
                // CAMBIO AQUÍ: Se eliminó el '!!' de workout.endTime
                val durationMillis = workout.endTime.time - workout.startTime.time
                val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
                var durationString = "Duración: "
                if (hours > 0) { durationString += "${hours}h " }
                if (hours > 0 || minutes > 0) {
                    durationString += "${minutes}m"
                } else {
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
                    if (seconds > 0) {
                        durationString += "${seconds}s"
                    } else {
                        durationString = "Duración: < 1s"
                    }
                }
                binding.textViewWorkoutStatusDuration.text = durationString
                binding.textViewWorkoutStatusDuration.visibility = View.VISIBLE
            } else {
                binding.textViewWorkoutStatusDuration.text = "En curso"
                binding.textViewWorkoutStatusDuration.visibility = View.VISIBLE
            }

            binding.textViewSetCount.text = "Series: ${workoutSummary.setCount}"
            binding.textViewSetCount.visibility = View.VISIBLE

            val notes = workout.notes
            if (!notes.isNullOrBlank()) {
                binding.textViewWorkoutNotesPreview.text = "Notas: $notes"
                binding.textViewWorkoutNotesPreview.visibility = View.VISIBLE
            } else {
                binding.textViewWorkoutNotesPreview.visibility = View.GONE
            }
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

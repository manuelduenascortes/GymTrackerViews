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

class WorkoutAdapter(
    private val onItemClick: (Workout) -> Unit,
    private val onDeleteClick: (Workout) -> Unit
) : ListAdapter<Workout, WorkoutAdapter.WorkoutViewHolder>(WorkoutDiffCallback()) { // Pass the callback instance

    // ViewHolder holds the views for a single item
    inner class WorkoutViewHolder(private val binding: ItemWorkoutBinding) : RecyclerView.ViewHolder(binding.root) {
        private var currentWorkout: Workout? = null

        init {
            // Set click listener for the entire row (navigation)
            binding.root.setOnClickListener {
                currentWorkout?.let { workout ->
                    onItemClick(workout) // Call the navigation lambda
                }
            }
            // Set click listener for the delete button
            binding.buttonDeleteWorkout.setOnClickListener {
                currentWorkout?.let { workout ->
                    onDeleteClick(workout) // Call the delete lambda
                }
            }
        }

        // Binds data to the views in the ViewHolder
        fun bind(workout: Workout) {
            currentWorkout = workout // Store the current workout

            // Format and display start time
            val dateFormat = SimpleDateFormat("dd MMM yy, HH:mm:ss", Locale.getDefault())
            binding.textViewWorkoutStartTime.text = "Inicio: ${dateFormat.format(workout.startTime)}"

            // Calculate and display status/duration
            if (workout.endTime != null) {
                val durationMillis = workout.endTime.time - workout.startTime.time
                val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
                var durationString = "Duración: "
                if (hours > 0) { durationString += "${hours}h " }
                if (hours > 0 || minutes > 0) { durationString += "${minutes}m" }
                else if (hours == 0L) {
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
                    if (seconds > 0) { durationString += "${seconds}s" }
                    else { durationString = "Duración: < 1m" }
                }
                binding.textViewWorkoutStatusDuration.text = durationString
                binding.textViewWorkoutStatusDuration.visibility = View.VISIBLE
            } else {
                binding.textViewWorkoutStatusDuration.text = "En curso"
                binding.textViewWorkoutStatusDuration.visibility = View.VISIBLE
            }

            // Show/Hide notes preview
            if (!workout.notes.isNullOrBlank()) {
                binding.textViewWorkoutNotesPreview.text = workout.notes
                binding.textViewWorkoutNotesPreview.visibility = View.VISIBLE
            } else {
                binding.textViewWorkoutNotesPreview.visibility = View.GONE
            }

            // Display ID
            binding.textViewWorkoutId.text = "Workout ID: ${workout.id}"
        }
    } // End ViewHolder

    // Creates new ViewHolders
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val binding = ItemWorkoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkoutViewHolder(binding)
    }

    // Binds data to an existing ViewHolder
    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

} // End Adapter

// --- DiffUtil Callback ---
// 👇 ENSURE THIS CLASS AND ITS METHODS ARE EXACTLY AS BELOW 👇
class WorkoutDiffCallback : DiffUtil.ItemCallback<Workout>() {
    // Checks if two items represent the same object (usually by ID)
    override fun areItemsTheSame(oldItem: Workout, newItem: Workout): Boolean {
        return oldItem.id == newItem.id
    }

    // Checks if the contents of two items are the same
    override fun areContentsTheSame(oldItem: Workout, newItem: Workout): Boolean {
        // Since Workout is a data class, '==' compares all properties
        return oldItem == newItem
    }
}
// --- End DiffUtil Callback ---

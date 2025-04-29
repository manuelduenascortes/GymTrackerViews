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
) : ListAdapter<WorkoutSummary, WorkoutAdapter.WorkoutViewHolder>(WorkoutSummaryDiffCallback()) {

    inner class WorkoutViewHolder(private val binding: ItemWorkoutBinding) : RecyclerView.ViewHolder(binding.root) {
        private var currentWorkoutSummary: WorkoutSummary? = null

        init {
            binding.root.setOnClickListener {
                currentWorkoutSummary?.workout?.let { workout -> onItemClick(workout) }
            }
            binding.buttonDeleteWorkout.setOnClickListener {
                currentWorkoutSummary?.workout?.let { workout -> onDeleteClick(workout) }
            }
        }

        fun bind(workoutSummary: WorkoutSummary) {
            currentWorkoutSummary = workoutSummary
            val workout = workoutSummary.workout // Obtenemos el objeto Workout interno

            // Formatear hora de inicio
            val dateFormat = SimpleDateFormat("dd MMM yy, HH:mm:ss", Locale.getDefault())
            binding.textViewWorkoutStartTime.text = "Inicio: ${dateFormat.format(workout.startTime)}"

            // Calcular y Mostrar Estado/Duración
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

            // Mostrar conteo de series
            binding.textViewSetCount.text = "Series: ${workoutSummary.setCount}"
            binding.textViewSetCount.visibility = View.VISIBLE

            // --- Mostrar/Ocultar Preview de Notas (Lógica Revisada) ---
            val notes = workout.notes // Obtenemos las notas
            if (!notes.isNullOrBlank()) { // Comprobamos si NO son nulas O vacías/blancas
                binding.textViewWorkoutNotesPreview.text = "Notas: ${notes}" // Añadimos prefijo "Notas: "
                binding.textViewWorkoutNotesPreview.visibility = View.VISIBLE // Hacemos VISIBLE
            } else {
                // Si son nulas o vacías, ocultamos el TextView
                binding.textViewWorkoutNotesPreview.visibility = View.GONE // Hacemos INVISIBLE
            }
            // --- Fin Preview Notas ---

            // ID del Workout (sigue oculto)
            binding.textViewWorkoutId.text = "Workout ID: ${workout.id}"
            // binding.textViewWorkoutId.visibility = View.GONE // Ya debería estar GONE en el XML
        }
    } // Fin ViewHolder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val binding = ItemWorkoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

} // Fin Adapter

// DiffUtil Callback para WorkoutSummary (sin cambios)
class WorkoutSummaryDiffCallback : DiffUtil.ItemCallback<WorkoutSummary>() {
    override fun areItemsTheSame(oldItem: WorkoutSummary, newItem: WorkoutSummary): Boolean {
        return oldItem.workout.id == newItem.workout.id
    }
    override fun areContentsTheSame(oldItem: WorkoutSummary, newItem: WorkoutSummary): Boolean {
        return oldItem == newItem
    }
}

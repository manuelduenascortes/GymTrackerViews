package com.example.gymtrackerviews // Tu paquete

import android.view.LayoutInflater
import android.view.View // <--- ¡¡IMPORT AÑADIDO!! ---
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gymtrackerviews.databinding.ItemWorkoutBinding // Binding para item_workout.xml
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit // Import para calcular diferencia de tiempo

class WorkoutAdapter(
    private val onItemClick: (Workout) -> Unit,
    private val onDeleteClick: (Workout) -> Unit
) : ListAdapter<Workout, WorkoutAdapter.WorkoutViewHolder>(WorkoutDiffCallback()) {

    inner class WorkoutViewHolder(private val binding: ItemWorkoutBinding) : RecyclerView.ViewHolder(binding.root) {
        private var currentWorkout: Workout? = null

        init {
            binding.root.setOnClickListener { currentWorkout?.let { onItemClick(it) } }
            binding.buttonDeleteWorkout.setOnClickListener { currentWorkout?.let { onDeleteClick(it) } }
        }

        fun bind(workout: Workout) {
            currentWorkout = workout
            binding.textViewWorkoutId.text = "Workout ID: ${workout.id}"

            // Formatear hora de inicio
            val dateFormat = SimpleDateFormat("dd MMM yy, HH:mm:ss", Locale.getDefault())
            binding.textViewWorkoutStartTime.text = "Inicio: ${dateFormat.format(workout.startTime)}"

            // --- Calcular y Mostrar Estado/Duración ---
            if (workout.endTime != null) {
                // Si hay hora de fin, calcular duración
                val durationMillis = workout.endTime.time - workout.startTime.time

                // Convertir milisegundos a formato Horas y Minutos
                val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60 // Minutos restantes

                var durationString = "Duración: "
                if (hours > 0) {
                    durationString += "${hours}h "
                }
                // Mostrar minutos solo si hay horas o si los minutos son > 0 (para no mostrar "Duración: 0m")
                if (hours > 0 || minutes > 0) {
                    durationString += "${minutes}m"
                } else if (hours == 0L) {
                    // Si duró menos de un minuto, mostrar segundos o "Menos de 1m"
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
                    if (seconds > 0) {
                        durationString += "${seconds}s"
                    } else {
                        durationString = "Duración: < 1m" // O manejar como prefieras
                    }
                }


                binding.textViewWorkoutStatusDuration.text = durationString
                // Ahora sí reconoce View.VISIBLE
                binding.textViewWorkoutStatusDuration.visibility = View.VISIBLE

            } else {
                // Si no hay hora de fin, mostrar "En curso"
                binding.textViewWorkoutStatusDuration.text = "En curso"
                // Ahora sí reconoce View.VISIBLE
                binding.textViewWorkoutStatusDuration.visibility = View.VISIBLE
            }
            // --- Fin Cálculo Estado/Duración ---
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

// DiffUtil Callback (sin cambios)
class WorkoutDiffCallback : DiffUtil.ItemCallback<Workout>() {
    override fun areItemsTheSame(oldItem: Workout, newItem: Workout): Boolean { return oldItem.id == newItem.id }
    override fun areContentsTheSame(oldItem: Workout, newItem: Workout): Boolean { return oldItem == newItem }
}

package com.example.gymtrackerviews // Tu paquete

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gymtrackerviews.databinding.ItemWorkoutBinding // Binding para item_workout.xml
import java.text.SimpleDateFormat
import java.util.Locale

// 👇 1. Añadimos OTRA lambda al constructor para el click de borrar
class WorkoutAdapter(
    private val onItemClick: (Workout) -> Unit, // Lambda para click en el item (navegar)
    private val onDeleteClick: (Workout) -> Unit // Lambda para click en el botón borrar
) : ListAdapter<Workout, WorkoutAdapter.WorkoutViewHolder>(WorkoutDiffCallback()) {

    inner class WorkoutViewHolder(private val binding: ItemWorkoutBinding) : RecyclerView.ViewHolder(binding.root) {
        private var currentWorkout: Workout? = null

        init {
            // Listener para TODA la fila (para navegar)
            binding.root.setOnClickListener {
                currentWorkout?.let { workout ->
                    onItemClick(workout) // Llama a la lambda de navegación
                }
            }
            // Listener para el BOTÓN de borrar
            binding.buttonDeleteWorkout.setOnClickListener {
                currentWorkout?.let { workout ->
                    onDeleteClick(workout) // Llama a la lambda de borrado
                }
            }
        }

        fun bind(workout: Workout) {
            currentWorkout = workout
            binding.textViewWorkoutId.text = "Workout ID: ${workout.id}"
            // Usamos el binding generado para el layout item_workout.xml
            val dateFormat = SimpleDateFormat("dd MMM yy, HH:mm:ss", Locale.getDefault())
            binding.textViewWorkoutStartTime.text = "Inicio: ${dateFormat.format(workout.startTime)}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        // Asegúrate que usa ItemWorkoutBinding
        val binding = ItemWorkoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

// DiffUtil Callback (sin cambios)
class WorkoutDiffCallback : DiffUtil.ItemCallback<Workout>() {
    override fun areItemsTheSame(oldItem: Workout, newItem: Workout): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: Workout, newItem: Workout): Boolean {
        return oldItem == newItem
    }
}
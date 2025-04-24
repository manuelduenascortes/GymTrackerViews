package com.example.gymtrackerviews // Tu paquete

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gymtrackerviews.databinding.ItemWorkoutSetBinding // ViewBinding para item_workout_set.xml

// Adaptador para la lista de WorkoutSet
class WorkoutSetAdapter : ListAdapter<WorkoutSet, WorkoutSetAdapter.WorkoutSetViewHolder>(WorkoutSetDiffCallback()) {

    // ViewHolder que contiene las vistas de una fila de serie
    inner class WorkoutSetViewHolder(private val binding: ItemWorkoutSetBinding) : RecyclerView.ViewHolder(binding.root) {
        // Función para poner los datos de un WorkoutSet en las vistas
        fun bind(workoutSet: WorkoutSet) {
            binding.textViewExerciseName.text = workoutSet.exerciseName
            binding.textViewReps.text = "${workoutSet.repetitions} reps" // Añadimos "reps"

            // Formateamos el peso para quitar el ".0" si es un número entero
            val weightFormatted = if (workoutSet.weight == workoutSet.weight.toInt().toDouble()) {
                workoutSet.weight.toInt().toString()
            } else {
                workoutSet.weight.toString()
            }
            binding.textViewWeight.text = "@ ${weightFormatted} kg" // Añadimos "@" y "kg"
        }
    }

    // Crea nuevas vistas (invocado por el layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutSetViewHolder {
        val binding = ItemWorkoutSetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkoutSetViewHolder(binding)
    }

    // Reemplaza el contenido de una vista (invocado por el layout manager)
    override fun onBindViewHolder(holder: WorkoutSetViewHolder, position: Int) {
        // Obtiene el elemento de esa posición y llama a bind
        holder.bind(getItem(position))
    }
}

// DiffUtil para calcular diferencias entre listas de WorkoutSet de forma eficiente
class WorkoutSetDiffCallback : DiffUtil.ItemCallback<WorkoutSet>() {
    override fun areItemsTheSame(oldItem: WorkoutSet, newItem: WorkoutSet): Boolean {
        return oldItem.id == newItem.id // Mismo item si mismo ID
    }

    override fun areContentsTheSame(oldItem: WorkoutSet, newItem: WorkoutSet): Boolean {
        return oldItem == newItem // Mismo contenido si el objeto es igual (data class)
    }
}
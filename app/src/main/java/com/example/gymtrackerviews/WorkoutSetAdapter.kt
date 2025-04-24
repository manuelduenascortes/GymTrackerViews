package com.example.gymtrackerviews // Tu paquete

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gymtrackerviews.databinding.ItemWorkoutSetBinding // ViewBinding para item_workout_set.xml

// 👇 1. Añadimos otro parámetro lambda para el click de borrar
class WorkoutSetAdapter(private val onDeleteClick: (WorkoutSet) -> Unit) :
    ListAdapter<WorkoutSet, WorkoutSetAdapter.WorkoutSetViewHolder>(WorkoutSetDiffCallback()) {

    inner class WorkoutSetViewHolder(private val binding: ItemWorkoutSetBinding) : RecyclerView.ViewHolder(binding.root) {
        // 👇 2. Variable para guardar la serie actual de esta fila
        private var currentWorkoutSet: WorkoutSet? = null

        // 👇 3. Bloque init para configurar listeners al crear el ViewHolder
        init {
            // Listener para el botón de borrar
            binding.buttonDeleteSet.setOnClickListener {
                // Llama a la función lambda onDeleteClick si la serie actual no es null
                currentWorkoutSet?.let {
                    onDeleteClick(it)
                }
            }
            // Podríamos añadir un listener para toda la fila aquí si quisiéramos editar
            // binding.root.setOnClickListener { ... }
        }


        fun bind(workoutSet: WorkoutSet) {
            // 👇 4. Guarda la serie actual
            currentWorkoutSet = workoutSet

            binding.textViewExerciseName.text = workoutSet.exerciseName
            binding.textViewReps.text = "${workoutSet.repetitions} reps"
            val weightFormatted = if (workoutSet.weight == workoutSet.weight.toInt().toDouble()) {
                workoutSet.weight.toInt().toString()
            } else {
                workoutSet.weight.toString()
            }
            binding.textViewWeight.text = "@ ${weightFormatted} kg"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutSetViewHolder {
        val binding = ItemWorkoutSetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkoutSetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutSetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

// DiffUtil Callback (sin cambios)
class WorkoutSetDiffCallback : DiffUtil.ItemCallback<WorkoutSet>() {
    override fun areItemsTheSame(oldItem: WorkoutSet, newItem: WorkoutSet): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: WorkoutSet, newItem: WorkoutSet): Boolean {
        return oldItem == newItem
    }
}
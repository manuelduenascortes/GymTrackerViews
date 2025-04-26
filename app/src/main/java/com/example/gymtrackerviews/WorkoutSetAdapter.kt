package com.example.gymtrackerviews // Tu paquete

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gymtrackerviews.databinding.ItemWorkoutSetBinding

class WorkoutSetAdapter(
    private val onItemClick: (WorkoutSet) -> Unit, // Para editar
    private val onDeleteClick: (WorkoutSet) -> Unit  // Para borrar
) : ListAdapter<WorkoutSet, WorkoutSetAdapter.WorkoutSetViewHolder>(WorkoutSetDiffCallback()) {

    inner class WorkoutSetViewHolder(private val binding: ItemWorkoutSetBinding) : RecyclerView.ViewHolder(binding.root) {
        private var currentWorkoutSet: WorkoutSet? = null

        init {
            binding.root.setOnClickListener {
                currentWorkoutSet?.let { set ->
                    onItemClick(set)
                }
            }
            binding.buttonDeleteSet.setOnClickListener {
                currentWorkoutSet?.let { set ->
                    onDeleteClick(set)
                }
            }
        }

        fun bind(workoutSet: WorkoutSet) {
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

class WorkoutSetDiffCallback : DiffUtil.ItemCallback<WorkoutSet>() {
    override fun areItemsTheSame(oldItem: WorkoutSet, newItem: WorkoutSet): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: WorkoutSet, newItem: WorkoutSet): Boolean {
        return oldItem == newItem
    }
}
package com.example.gymtrackerviews.adapter // O tu paquete principal si no creaste el subpaquete 'adapter'

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gymtrackerviews.Exercise // TODO: Asegúrate de importar tu entidad Exercise
import com.example.gymtrackerviews.databinding.ItemExerciseDefinitionBinding // Se genera a partir de item_exercise_definition.xml

class ExerciseAdapter(
    private val onItemClick: (Exercise) -> Unit,
    private val onDeleteClick: (Exercise) -> Unit
    // Podríamos añadir un onEditClick más adelante
) : ListAdapter<Exercise, ExerciseAdapter.ExerciseViewHolder>(ExerciseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val binding = ItemExerciseDefinitionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExerciseViewHolder(binding, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val currentExercise = getItem(position)
        if (currentExercise != null) {
            holder.bind(currentExercise)
        }
    }

    class ExerciseViewHolder(
        private val binding: ItemExerciseDefinitionBinding,
        private val onItemClick: (Exercise) -> Unit,
        private val onDeleteClick: (Exercise) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentExercise: Exercise? = null

        init {
            // Click en todo el ítem (podría ser para ver detalles o editar)
            binding.root.setOnClickListener {
                currentExercise?.let { exercise ->
                    onItemClick(exercise)
                }
            }
            // Click en el botón de borrar
            binding.buttonDeleteExercise.setOnClickListener {
                currentExercise?.let { exercise ->
                    onDeleteClick(exercise)
                }
            }
        }

        fun bind(exercise: Exercise) {
            currentExercise = exercise
            binding.textViewExerciseName.text = exercise.name
            binding.textViewExerciseMuscleGroup.text = exercise.muscleGroup ?: "Sin grupo muscular" // Muestra un texto por defecto si es nulo

            // Mostrar descripción si existe, si no, ocultar el TextView
            if (!exercise.description.isNullOrBlank()) {
                binding.textViewExerciseDescription.text = exercise.description
                binding.textViewExerciseDescription.visibility = View.VISIBLE
            } else {
                binding.textViewExerciseDescription.visibility = View.GONE
            }
        }
    }

    class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem == newItem // Funciona bien para data classes
        }
    }
}

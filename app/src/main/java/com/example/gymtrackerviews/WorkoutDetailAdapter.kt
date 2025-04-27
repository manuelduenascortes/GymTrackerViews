package com.example.gymtrackerviews // Tu paquete

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gymtrackerviews.databinding.ItemExerciseHeaderBinding
import com.example.gymtrackerviews.databinding.ItemWorkoutSetDetailBinding

// Constantes para tipos de vista
private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_SET = 1

class WorkoutDetailAdapter(
    private val onSetClick: (WorkoutSet) -> Unit, // Editar
    private val onSetDeleteClick: (WorkoutSet) -> Unit // Borrar
) : ListAdapter<WorkoutDetailListItem, RecyclerView.ViewHolder>(WorkoutDetailDiffCallback()) {

    // --- ViewHolder Cabecera ---
    class HeaderViewHolder(private val binding: ItemExerciseHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(headerItem: WorkoutDetailListItem.HeaderItem) {
            binding.textViewHeaderExerciseName.text = headerItem.exerciseName
        }
        companion object {
            fun from(parent: ViewGroup): HeaderViewHolder {
                val binding = ItemExerciseHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return HeaderViewHolder(binding)
            }
        }
    }

    // --- ViewHolder Serie ---
    class SetViewHolder(
        private val binding: ItemWorkoutSetDetailBinding,
        private val onSetClick: (WorkoutSet) -> Unit,
        private val onSetDeleteClick: (WorkoutSet) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentWorkoutSet: WorkoutSet? = null

        init {
            binding.root.setOnClickListener { currentWorkoutSet?.let { onSetClick(it) } }
            binding.buttonDeleteSetRow.setOnClickListener { currentWorkoutSet?.let { onSetDeleteClick(it) } }
        }

        fun bind(setItem: WorkoutDetailListItem.SetItem) {
            currentWorkoutSet = setItem.workoutSet
            val workoutSet = setItem.workoutSet
            binding.textViewSetReps.text = "${workoutSet.repetitions} reps"
            val weightFormatted = if (workoutSet.weight == workoutSet.weight.toInt().toDouble()) {
                workoutSet.weight.toInt().toString()
            } else { workoutSet.weight.toString() }
            binding.textViewSetWeight.text = "@ ${weightFormatted} kg"
        }

        companion object {
            fun from(parent: ViewGroup, onSetClick: (WorkoutSet) -> Unit, onSetDeleteClick: (WorkoutSet) -> Unit): SetViewHolder {
                val binding = ItemWorkoutSetDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return SetViewHolder(binding, onSetClick, onSetDeleteClick)
            }
        }
    }

    // --- Métodos Adapter ---
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is WorkoutDetailListItem.HeaderItem -> ITEM_VIEW_TYPE_HEADER
            is WorkoutDetailListItem.SetItem -> ITEM_VIEW_TYPE_SET
            // Añadir null check por si acaso getItem devuelve null temporalmente
            null -> throw IllegalStateException("Item at position $position is null")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> HeaderViewHolder.from(parent)
            ITEM_VIEW_TYPE_SET -> SetViewHolder.from(parent, onSetClick, onSetDeleteClick)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Añadir null check por si acaso
        getItem(position)?.let { item ->
            when (holder) {
                is HeaderViewHolder -> holder.bind(item as WorkoutDetailListItem.HeaderItem)
                is SetViewHolder -> holder.bind(item as WorkoutDetailListItem.SetItem)
            }
        }
    }
}

// --- DiffUtil Callback ---
class WorkoutDetailDiffCallback : DiffUtil.ItemCallback<WorkoutDetailListItem>() {
    override fun areItemsTheSame(oldItem: WorkoutDetailListItem, newItem: WorkoutDetailListItem): Boolean {
        return oldItem.id == newItem.id // Usa el ID de la sealed class
    }
    override fun areContentsTheSame(oldItem: WorkoutDetailListItem, newItem: WorkoutDetailListItem): Boolean {
        return oldItem == newItem // Compara data classes
    }
}

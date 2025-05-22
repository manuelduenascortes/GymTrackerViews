package com.example.gymtrackerviews // Tu paquete

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gymtrackerviews.databinding.ItemWorkoutBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class WorkoutAdapter(
    private val onItemClick: (WorkoutSummary) -> Unit,
    private val onDeleteClick: (WorkoutSummary) -> Unit
) : ListAdapter<WorkoutSummary, WorkoutAdapter.WorkoutViewHolder>(WorkoutSummaryDiffCallback()) {

    inner class WorkoutViewHolder(private val binding: ItemWorkoutBinding) : RecyclerView.ViewHolder(binding.root) {
        private var currentWorkoutSummary: WorkoutSummary? = null
        private val dateFormatDateTitle = SimpleDateFormat("dd MMM yy", Locale.getDefault()) // Formato para el título de fecha

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

            // 1. Título Principal: Siempre la Fecha
            binding.textViewWorkoutDateTitle.text = "Entrenamiento del ${dateFormatDateTitle.format(workout.startTime)}"

            // 2. Nombre Personalizado del Entrenamiento
            // Un nombre es "personalizado" si existe Y no es igual a los grupos musculares (ya que los grupos musculares se usan como nombre autogenerado)
            val isCustomNameProvided = !workout.name.isNullOrBlank()
            val isNameDifferentFromMuscleGroups = workout.name != workout.mainMuscleGroup

            if (isCustomNameProvided && (workout.mainMuscleGroup.isNullOrBlank() || isNameDifferentFromMuscleGroups)) {
                binding.textViewWorkoutCustomName.text = workout.name
                binding.textViewWorkoutCustomName.visibility = View.VISIBLE
            } else {
                binding.textViewWorkoutCustomName.visibility = View.GONE
            }

            // 3. Grupos Musculares
            // Mostrar solo si hay grupos musculares
            if (!workout.mainMuscleGroup.isNullOrBlank()) {
                binding.textViewWorkoutMuscleGroups.text = workout.mainMuscleGroup
                binding.textViewWorkoutMuscleGroups.visibility = View.VISIBLE
            } else {
                binding.textViewWorkoutMuscleGroups.visibility = View.GONE
            }

            // Ajustar la constraint superior de layoutWorkoutMetaInfo dinámicamente
            // Esto asegura que layoutWorkoutMetaInfo se coloque debajo del último elemento visible
            // entre el título de fecha, nombre personalizado y grupos musculares.
            val metaInfoTopAnchorId: Int
            if (binding.textViewWorkoutMuscleGroups.visibility == View.VISIBLE) {
                metaInfoTopAnchorId = binding.textViewWorkoutMuscleGroups.id
            } else if (binding.textViewWorkoutCustomName.visibility == View.VISIBLE) {
                metaInfoTopAnchorId = binding.textViewWorkoutCustomName.id
            } else {
                metaInfoTopAnchorId = binding.textViewWorkoutDateTitle.id
            }
            (binding.layoutWorkoutMetaInfo.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).apply {
                topToBottom = metaInfoTopAnchorId
            }


            // 4. Estado/Duración y Series
            if (workout.endTime != null) {
                val durationMillis = workout.endTime.time - workout.startTime.time
                val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
                var durationString = ""
                if (hours > 0) { durationString += "${hours}h " }
                if (hours > 0 || minutes > 0) {
                    durationString += "${minutes}m"
                } else {
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
                    if (seconds > 0) {
                        durationString += "${seconds}s"
                    } else {
                        durationString = "< 1s"
                    }
                }
                binding.textViewWorkoutStatusDuration.text = "Duración: $durationString"
            } else {
                binding.textViewWorkoutStatusDuration.text = "En curso"
            }
            binding.textViewSetCount.text = "Series: ${workoutSummary.setCount}"


            // 5. Preview de Notas
            if (!workout.notes.isNullOrBlank()) {
                binding.textViewWorkoutNotesPreview.text = "Notas: ${workout.notes}"
                binding.textViewWorkoutNotesPreview.visibility = View.VISIBLE
            } else {
                binding.textViewWorkoutNotesPreview.visibility = View.GONE
            }

            binding.textViewWorkoutId.text = "Workout ID: ${workout.id}" // Sigue oculto por defecto
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

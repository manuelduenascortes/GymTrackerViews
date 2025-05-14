package com.example.gymtrackerviews // O tu paquete principal

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymtrackerviews.databinding.DialogEditWorkoutNameBinding
import com.example.gymtrackerviews.databinding.FragmentWorkoutDetailBinding
// TODO: Asegúrate de importar tus clases necesarias.
// Es MUY IMPORTANTE que los imports para Workout, WorkoutSet, WorkoutDetailAdapter,
// WorkoutDetailListItem, WorkoutDetailViewModel, WorkoutDetailViewModelFactory,
// GymTrackerApplication y DialogAddSetBinding sean correctos y apunten a las clases
// que realmente existen en tu proyecto y en los paquetes correctos.
// Si Android Studio marca un import en rojo, significa que no encuentra esa clase donde se espera.

// Ejemplo de imports que podrías necesitar (ajusta los paquetes según tu proyecto):
// import com.example.gymtrackerviews.model.Workout
// import com.example.gymtrackerviews.model.WorkoutSet
// import com.example.gymtrackerviews.adapter.WorkoutDetailAdapter
// import com.example.gymtrackerviews.model.WorkoutDetailListItem // Tu sealed class
// import com.example.gymtrackerviews.viewmodel.WorkoutDetailViewModel
// import com.example.gymtrackerviews.viewmodel.WorkoutDetailViewModelFactory
// import com.example.gymtrackerviews.GymTrackerApplication
// import com.example.gymtrackerviews.databinding.DialogAddSetBinding


import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class WorkoutDetailFragment : Fragment() {

    private var _binding: FragmentWorkoutDetailBinding? = null
    private val binding get() = _binding!!

    private val args: WorkoutDetailFragmentArgs by navArgs()

    private val viewModel: WorkoutDetailViewModel by viewModels {
        // Esto asume que GymTrackerApplication, AppDatabase, WorkoutDao, WorkoutSetDao
        // y WorkoutDetailViewModelFactory están correctamente definidos y accesibles.
        val application = requireActivity().application as GymTrackerApplication
        WorkoutDetailViewModelFactory(args.workoutId, application.database.workoutDao(), application.database.workoutSetDao())
    }

    // El constructor del adapter debe coincidir con tu WorkoutDetailAdapter.kt
    // que espera onSetClick y onSetDeleteClick.
    private lateinit var workoutDetailAdapter: WorkoutDetailAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        workoutDetailAdapter = WorkoutDetailAdapter(
            onSetClick = { workoutSetToEdit ->
                Log.d("WorkoutDetailFragment", "Set Item Clicked - Set ID: ${workoutSetToEdit.id}")
                if (viewModel.workout.value?.endTime == null) {
                    showAddOrEditSetDialog(workoutSetToEdit)
                } else {
                    context?.let { Toast.makeText(it, "Workout finalizado, no se puede editar.", Toast.LENGTH_SHORT).show() }
                }
            },
            onSetDeleteClick = { workoutSetToDelete ->
                Log.d("WorkoutDetailFragment", "Set Delete Clicked - Set ID: ${workoutSetToDelete.id}")
                if (viewModel.workout.value?.endTime == null) {
                    // TODO: Considerar mostrar un diálogo de confirmación aquí
                    viewModel.deleteWorkoutSet(workoutSetToDelete)
                    context?.let { Toast.makeText(it, "Serie borrada", Toast.LENGTH_SHORT).show()}
                } else {
                    context?.let { Toast.makeText(it, "Workout finalizado, no se puede borrar.", Toast.LENGTH_SHORT).show() }
                }
            }
        )
        binding.recyclerViewSets.apply {
            adapter = workoutDetailAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.workout.collectLatest { workout -> // workout es de tipo Workout?
                        workout?.let {
                            binding.textViewWorkoutNameDetail.text = it.name ?: "Entrenamiento sin nombre"
                            (activity as? AppCompatActivity)?.supportActionBar?.title = it.name ?: "Detalle Workout"

                            val isFinished = it.endTime != null
                            binding.textViewDetailEndTime.visibility = if (isFinished) View.VISIBLE else View.GONE
                            if (isFinished) {
                                val sdf = SimpleDateFormat("dd MMM yy, HH:mm", Locale.getDefault())
                                binding.textViewDetailEndTime.text = "Finalizado: ${sdf.format(it.endTime!!)}"
                            }

                            binding.buttonFinishWorkout.visibility = if (isFinished) View.GONE else View.VISIBLE
                            binding.buttonAddSet.isEnabled = !isFinished
                            binding.editTextWorkoutNotes.isEnabled = !isFinished
                            binding.buttonEditWorkoutName.isEnabled = !isFinished

                            val canOperateTimer = !isFinished
                            binding.buttonTimerToggle.isEnabled = canOperateTimer
                            binding.buttonTimerReset.isEnabled = canOperateTimer && viewModel.timerValue.value < viewModel.startRestTimeInMillis.value
                            binding.buttonTimerDecrease.isEnabled = canOperateTimer && !viewModel.isTimerRunning.value
                            binding.buttonTimerIncrease.isEnabled = canOperateTimer && !viewModel.isTimerRunning.value
                            if (viewModel.timerValue.value <= viewModel.minRestTimeMillis) binding.buttonTimerDecrease.isEnabled = false
                            if (viewModel.timerValue.value >= viewModel.maxRestTimeMillis) binding.buttonTimerIncrease.isEnabled = false

                            if (!binding.editTextWorkoutNotes.hasFocus()) {
                                binding.editTextWorkoutNotes.setText(it.notes ?: "")
                            }
                        }
                    }
                }

                launch {
                    viewModel.workoutSets.collectLatest { sets: List<WorkoutSet> -> // sets es List<WorkoutSet>
                        // Transformar List<WorkoutSet> a List<WorkoutDetailListItem>
                        val detailListItems = mutableListOf<WorkoutDetailListItem>()
                        // Agrupar WorkoutSet por exerciseName
                        val groupedSets = sets.groupBy { it.exerciseName }

                        // Crear la lista para el adapter con cabeceras y sets
                        groupedSets.toSortedMap().forEach { (exerciseName, setsForExercise) -> // Ordenar por nombre de ejercicio
                            detailListItems.add(WorkoutDetailListItem.HeaderItem(exerciseName))
                            // Ordenar las series por timestamp dentro de cada ejercicio
                            setsForExercise.sortedBy { it.timestamp }.forEach { workoutSet ->
                                detailListItems.add(WorkoutDetailListItem.SetItem(workoutSet))
                            }
                        }
                        workoutDetailAdapter.submitList(detailListItems) // El adapter espera List<WorkoutDetailListItem>
                        binding.textViewEmptySets.visibility = if (detailListItems.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.timerValue.collect { timeInMillis ->
                        val minutes = (timeInMillis / 1000) / 60
                        val seconds = (timeInMillis / 1000) % 60
                        binding.textViewTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                    }
                }

                launch {
                    viewModel.isTimerRunning.collect { isRunning ->
                        val iconResId = if (isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                        binding.buttonTimerToggle.setIconResource(iconResId)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonAddSet.setOnClickListener {
            if (binding.buttonAddSet.isEnabled) {
                showAddOrEditSetDialog(null)
            }
        }

        binding.buttonFinishWorkout.setOnClickListener {
            val notes = binding.editTextWorkoutNotes.text.toString().trim()
            viewModel.finishWorkout(notes)
            context?.let { Toast.makeText(it, "Workout finalizado", Toast.LENGTH_SHORT).show() }
        }

        binding.buttonEditWorkoutName.setOnClickListener {
            if (binding.buttonEditWorkoutName.isEnabled) {
                showEditWorkoutNameDialog()
            }
        }

        binding.buttonTimerToggle.setOnClickListener { if(viewModel.workout.value?.endTime == null) viewModel.toggleTimer() }
        binding.buttonTimerDecrease.setOnClickListener { if(viewModel.workout.value?.endTime == null) viewModel.decreaseTimer() }
        binding.buttonTimerIncrease.setOnClickListener { if(viewModel.workout.value?.endTime == null) viewModel.increaseTimer() }
        binding.buttonTimerReset.setOnClickListener { if(viewModel.workout.value?.endTime == null) viewModel.resetTimer() }

        binding.editTextWorkoutNotes.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && isAdded) {
                val notes = binding.editTextWorkoutNotes.text.toString().trim()
                viewModel.updateWorkoutNotes(notes)
            }
        }
    }

    private fun showEditWorkoutNameDialog() {
        if (!isAdded || context == null) return

        val dialogBinding = DialogEditWorkoutNameBinding.inflate(LayoutInflater.from(requireContext()))
        val editTextNewName = dialogBinding.editTextEditWorkoutNameDialog

        editTextNewName.setText(viewModel.workout.value?.name ?: "")

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { dialog, _ ->
                val newName = editTextNewName.text.toString().trim()
                viewModel.updateWorkoutName(newName)
                context?.let { Toast.makeText(it, "Nombre guardado", Toast.LENGTH_SHORT).show() }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    // TODO: Implementa este método basándote en tu lógica anterior para añadir/editar series.
    // Necesitarás inflar tu dialog_add_set.xml (probablemente con DialogAddSetBinding)
    // y llamar a viewModel.insertSet(...) o viewModel.updateSet(...).
    private fun showAddOrEditSetDialog(existingSet: WorkoutSet?) {
        if (!isAdded || context == null) return
        val isEditing = existingSet != null

        // Ejemplo (necesitarás DialogAddSetBinding y que esté importado):
        /*
        val dialogAddSetBinding = DialogAddSetBinding.inflate(LayoutInflater.from(requireContext()))
        if (isEditing && existingSet != null) {
            // Pre-rellenar campos del diálogo
            dialogAddSetBinding.editTextExerciseName.setText(existingSet.exerciseName)
            dialogAddSetBinding.editTextReps.setText(existingSet.repetitions.toString())
            dialogAddSetBinding.editTextWeight.setText(existingSet.weight.toString())
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEditing) "Editar Serie" else "Añadir Nueva Serie")
            .setView(dialogAddSetBinding.root)
            .setPositiveButton(if (isEditing) "Actualizar" else "Guardar") { dialog, _ ->
                val name = dialogAddSetBinding.editTextExerciseName.text.toString().trim()
                val reps = dialogAddSetBinding.editTextReps.text.toString().toIntOrNull()
                val weight = dialogAddSetBinding.editTextWeight.text.toString().toDoubleOrNull()

                if (name.isNotEmpty() && reps != null && weight != null && reps > 0 && weight >= 0) {
                    if (isEditing && existingSet != null) {
                        val updatedSet = existingSet.copy(exerciseName = name, repetitions = reps, weight = weight)
                        viewModel.updateSet(updatedSet)
                        context?.let { Toast.makeText(it, "Serie actualizada", Toast.LENGTH_SHORT).show() }
                    } else {
                        viewModel.insertSet(name, reps, weight)
                        context?.let { Toast.makeText(it, "Serie guardada", Toast.LENGTH_SHORT).show() }
                    }
                    dialog.dismiss()
                } else {
                    context?.let { Toast.makeText(it, "Datos inválidos para la serie", Toast.LENGTH_SHORT).show() }
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
        */
        context?.let { Toast.makeText(it, if (isEditing) "Editar serie (TODO)" else "Añadir serie (TODO)", Toast.LENGTH_SHORT).show() }
    }


    override fun onPause() {
        super.onPause()
        if (isAdded && _binding != null && viewModel.workout.value?.endTime == null) {
            val notes = binding.editTextWorkoutNotes.text.toString().trim()
            viewModel.updateWorkoutNotes(notes)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.gymtrackerviews // O tu paquete principal

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
// import androidx.navigation.fragment.findNavController // No se usa directamente aquí
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymtrackerviews.databinding.DialogAddSetBinding
import com.example.gymtrackerviews.databinding.DialogEditWorkoutNameBinding
import com.example.gymtrackerviews.databinding.FragmentWorkoutDetailBinding
import com.example.gymtrackerviews.R // Importante para R.drawable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class WorkoutDetailFragment : Fragment() {

    private var _binding: FragmentWorkoutDetailBinding? = null
    private val binding get() = _binding!!

    private val args: WorkoutDetailFragmentArgs by navArgs()

    private val viewModel: WorkoutDetailViewModel by viewModels {
        val application = requireActivity().application as GymTrackerApplication
        WorkoutDetailViewModelFactory(
            args.workoutId,
            application.database.workoutDao(),
            application.database.workoutSetDao(),
            application.database.exerciseDao()
        )
    }

    private lateinit var workoutDetailAdapter: WorkoutDetailAdapter
    private lateinit var exerciseNamesCombinedAdapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutDetailBinding.inflate(inflater, container, false)
        exerciseNamesCombinedAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
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
                    showDeleteSetConfirmationDialog(workoutSetToDelete)
                } else {
                    context?.let { Toast.makeText(it, "Workout finalizado, no se puede borrar.", Toast.LENGTH_SHORT).show() }
                }
            },
            onSetDuplicateClick = { workoutSetToDuplicate ->
                Log.d("WorkoutDetailFragment", "Set Duplicate Clicked - Set ID: ${workoutSetToDuplicate.id}")
                if (viewModel.workout.value?.endTime == null) {
                    viewModel.insertSet(
                        workoutSetToDuplicate.exerciseName,
                        workoutSetToDuplicate.repetitions,
                        workoutSetToDuplicate.weight
                    )
                    context?.let { Toast.makeText(it, "'${workoutSetToDuplicate.exerciseName}' duplicada", Toast.LENGTH_SHORT).show() }
                } else {
                    context?.let { Toast.makeText(it, "Workout finalizado, no se puede duplicar.", Toast.LENGTH_SHORT).show() }
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
                    viewModel.workout.collectLatest { workout ->
                        workout?.let {
                            binding.textViewWorkoutNameDetail.text = it.name ?: "Entrenamiento sin nombre"
                            val toolbarTitle = if (it.endTime == null) {
                                "Entrenamiento en curso"
                            } else {
                                "Entrenamiento finalizado"
                            }
                            (activity as? AppCompatActivity)?.supportActionBar?.title = toolbarTitle

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
                            binding.buttonTimerReset.isEnabled = canOperateTimer &&
                                    (viewModel.isTimerRunning.value || viewModel.timerValue.value != viewModel.startRestTimeInMillis.value)
                            binding.buttonTimerDecrease.isEnabled = canOperateTimer && !viewModel.isTimerRunning.value
                            binding.buttonTimerIncrease.isEnabled = canOperateTimer && !viewModel.isTimerRunning.value

                            if (viewModel.startRestTimeInMillis.value <= viewModel.minRestTimeMillis) {
                                binding.buttonTimerDecrease.isEnabled = false
                            }
                            if (viewModel.startRestTimeInMillis.value >= viewModel.maxRestTimeMillis) {
                                binding.buttonTimerIncrease.isEnabled = false
                            }

                            if (!binding.editTextWorkoutNotes.hasFocus()) {
                                binding.editTextWorkoutNotes.setText(it.notes ?: "")
                            }
                        }
                    }
                }

                launch {
                    viewModel.workoutSets.collectLatest { sets: List<WorkoutSet> ->
                        val detailListItems = mutableListOf<WorkoutDetailListItem>()
                        val groupedSets = sets.groupBy { it.exerciseName }
                        groupedSets.toSortedMap().forEach { (exerciseName, setsForExercise) ->
                            detailListItems.add(WorkoutDetailListItem.HeaderItem(exerciseName))
                            setsForExercise.sortedBy { it.timestamp }.forEach { workoutSet ->
                                detailListItems.add(WorkoutDetailListItem.SetItem(workoutSet))
                            }
                        }
                        workoutDetailAdapter.submitList(detailListItems)
                        binding.textViewEmptySets.visibility = if (detailListItems.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.timerValue.collect { timeInMillis ->
                        val minutes = (timeInMillis / 1000) / 60
                        val seconds = (timeInMillis / 1000) % 60
                        binding.textViewTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                        val currentWorkout = viewModel.workout.value
                        val isFinished = currentWorkout?.endTime != null
                        val canOperateTimer = !isFinished
                        binding.buttonTimerReset.isEnabled = canOperateTimer &&
                                (viewModel.isTimerRunning.value || timeInMillis != viewModel.startRestTimeInMillis.value)
                    }
                }

                launch {
                    viewModel.isTimerRunning.collect { isRunning ->
                        // <<< ICONOS PLAY/PAUSE CORREGIDOS AQUÍ para usar tus archivos XML >>>
                        val iconResId = if (isRunning) R.drawable.pause_24px else R.drawable.play_arrow_24px
                        binding.buttonTimerToggle.setIconResource(iconResId)
                        // <<< FIN CORRECCIÓN ICONOS PLAY/PAUSE >>>

                        val currentWorkout = viewModel.workout.value
                        val isFinished = currentWorkout?.endTime != null
                        val canOperateTimer = !isFinished
                        binding.buttonTimerReset.isEnabled = canOperateTimer &&
                                (isRunning || viewModel.timerValue.value != viewModel.startRestTimeInMillis.value)

                        binding.buttonTimerDecrease.isEnabled = canOperateTimer && !isRunning
                        binding.buttonTimerIncrease.isEnabled = canOperateTimer && !isRunning
                        if (viewModel.startRestTimeInMillis.value <= viewModel.minRestTimeMillis) {
                            binding.buttonTimerDecrease.isEnabled = false
                        }
                        if (viewModel.startRestTimeInMillis.value >= viewModel.maxRestTimeMillis) {
                            binding.buttonTimerIncrease.isEnabled = false
                        }
                    }
                }

                launch {
                    viewModel.exerciseNames.collectLatest { namesFromDb ->
                        if (context != null) {
                            val defaultExerciseNames = try {
                                resources.getStringArray(R.array.default_exercise_list).toList()
                            } catch (e: Exception) {
                                Log.e("WorkoutDetailFragment", "Error al cargar default_exercise_list de arrays.xml", e)
                                emptyList<String>()
                            }

                            val combinedNamesSet = mutableSetOf<String>()
                            combinedNamesSet.addAll(defaultExerciseNames)
                            combinedNamesSet.addAll(namesFromDb)

                            val combinedList = combinedNamesSet.toList().sorted()

                            exerciseNamesCombinedAdapter.clear()
                            exerciseNamesCombinedAdapter.addAll(combinedList)
                            exerciseNamesCombinedAdapter.notifyDataSetChanged()

                            Log.d("WorkoutDetailFragment", "Exercise names adapter updated. Defaults: ${defaultExerciseNames.size}, DB: ${namesFromDb.size}, Combined: ${combinedList.size} items.")
                        }
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
        binding.buttonTimerDecrease.setOnClickListener { if(viewModel.workout.value?.endTime == null && !viewModel.isTimerRunning.value) viewModel.decreaseTimer() }
        binding.buttonTimerIncrease.setOnClickListener { if(viewModel.workout.value?.endTime == null && !viewModel.isTimerRunning.value) viewModel.increaseTimer() }
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

    private fun showAddOrEditSetDialog(existingSet: WorkoutSet?) {
        if (!isAdded || context == null) {
            Log.w("WorkoutDetailFragment", "Fragment not added or context is null in showAddOrEditSetDialog")
            return
        }
        val dialogBinding = DialogAddSetBinding.inflate(LayoutInflater.from(requireContext()))
        val isEditing = existingSet != null

        dialogBinding.editTextExerciseName.setAdapter(exerciseNamesCombinedAdapter)
        Log.d("WorkoutDetailFragment", "AutoCompleteTextView adapter set from combined exercise names list.")

        if (isEditing && existingSet != null) {
            dialogBinding.editTextExerciseName.setText(existingSet.exerciseName, false)
            dialogBinding.editTextReps.setText(existingSet.repetitions.toString())
            dialogBinding.editTextWeight.setText(existingSet.weight.toString())
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(if (isEditing) "Editar Serie" else "Añadir Nueva Serie")
        builder.setView(dialogBinding.root)

        builder.setPositiveButton(if (isEditing) "Actualizar" else "Guardar") { dialog, _ ->
            val exerciseName = dialogBinding.editTextExerciseName.text.toString().trim()
            val repsString = dialogBinding.editTextReps.text.toString()
            val weightString = dialogBinding.editTextWeight.text.toString()

            if (exerciseName.isEmpty() || repsString.isEmpty() || weightString.isEmpty()) {
                context?.let { Toast.makeText(it, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show() }
                return@setPositiveButton
            }

            try {
                val reps = repsString.toInt()
                val weight = weightString.toDouble()

                if (reps <= 0) {
                    context?.let { Toast.makeText(it, "Las repeticiones deben ser mayor que cero", Toast.LENGTH_SHORT).show() }
                    return@setPositiveButton
                }
                if (weight < 0) {
                    context?.let { Toast.makeText(it, "El peso no puede ser negativo", Toast.LENGTH_SHORT).show() }
                    return@setPositiveButton
                }

                if (isEditing && existingSet != null) {
                    val updatedSet = existingSet.copy(exerciseName = exerciseName, repetitions = reps, weight = weight)
                    viewModel.updateSet(updatedSet)
                    context?.let { Toast.makeText(it, "Serie actualizada", Toast.LENGTH_SHORT).show() }
                } else {
                    viewModel.insertSet(exerciseName, reps, weight)
                    context?.let { Toast.makeText(it, "Serie guardada", Toast.LENGTH_SHORT).show() }
                }
                dialog.dismiss()
            } catch (e: NumberFormatException) {
                context?.let { Toast.makeText(it, "Introduce números válidos para Reps y Peso", Toast.LENGTH_SHORT).show() }
            }
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }

        try {
            builder.create().show()
        } catch (e: Exception) {
            Log.e("WorkoutDetailFragment", "Error showing Add/Edit dialog", e)
            context?.let { Toast.makeText(it, "Error al mostrar el diálogo de serie", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun showDeleteSetConfirmationDialog(setToDelete: WorkoutSet) {
        if (!isAdded || context == null) return
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Borrado")
            .setMessage("¿Seguro que quieres borrar esta serie?\n(${setToDelete.exerciseName}: ${setToDelete.repetitions} reps @ ${setToDelete.weight} kg)")
            .setPositiveButton("Borrar") { _, _ ->
                viewModel.deleteWorkoutSet(setToDelete)
                context?.let { Toast.makeText(it, "Serie borrada", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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

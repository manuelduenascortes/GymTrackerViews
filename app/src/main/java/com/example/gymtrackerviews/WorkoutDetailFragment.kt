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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymtrackerviews.databinding.DialogAddSetBinding // IMPORTANTE: Para el diálogo de añadir/editar serie
import com.example.gymtrackerviews.databinding.DialogEditWorkoutNameBinding
import com.example.gymtrackerviews.databinding.FragmentWorkoutDetailBinding
// TODO: Asegúrate de importar tus clases necesarias.
// import com.example.gymtrackerviews.Workout
// import com.example.gymtrackerviews.WorkoutSet
// import com.example.gymtrackerviews.WorkoutDetailAdapter
// import com.example.gymtrackerviews.WorkoutDetailListItem
// import com.example.gymtrackerviews.WorkoutDetailViewModel
// import com.example.gymtrackerviews.WorkoutDetailViewModelFactory
// import com.example.gymtrackerviews.GymTrackerApplication
import com.example.gymtrackerviews.R // Para R.array.default_exercise_list

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
        WorkoutDetailViewModelFactory(args.workoutId, application.database.workoutDao(), application.database.workoutSetDao())
    }

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
                    // Mostrar diálogo de confirmación antes de borrar
                    showDeleteSetConfirmationDialog(workoutSetToDelete)
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
                    viewModel.workout.collectLatest { workout ->
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
                showAddOrEditSetDialog(null) // Pasar null para añadir nueva serie
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

    // MÉTODO showAddOrEditSetDialog IMPLEMENTADO COMPLETAMENTE
    private fun showAddOrEditSetDialog(existingSet: WorkoutSet?) {
        if (!isAdded || context == null) {
            Log.w("WorkoutDetailFragment", "Fragment not added or context is null in showAddOrEditSetDialog")
            return
        }
        // Inflar el layout del diálogo usando ViewBinding
        // TODO: Asegúrate de que tienes 'DialogAddSetBinding' generado a partir de 'dialog_add_set.xml'
        // y que el import 'com.example.gymtrackerviews.databinding.DialogAddSetBinding' es correcto.
        val dialogBinding = DialogAddSetBinding.inflate(LayoutInflater.from(requireContext()))
        val isEditing = existingSet != null

        // Configuración del AutoCompleteTextView para nombres de ejercicios
        try {
            // TODO: Asegúrate de tener un string-array llamado 'default_exercise_list' en res/values/arrays.xml
            val exercises: Array<String> = resources.getStringArray(R.array.default_exercise_list)
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, exercises)
            // TODO: Asegúrate de que el ID en dialog_add_set.xml es 'editTextExerciseName' y es un AutoCompleteTextView
            dialogBinding.editTextExerciseName.setAdapter(adapter)
            Log.d("WorkoutDetailFragment", "AutoCompleteTextView adapter set for exercises.")
        } catch (e: Exception) {
            Log.e("WorkoutDetailFragment", "Error setting AutoCompleteTextView adapter", e)
            context?.let { Toast.makeText(it, "Error al cargar lista de ejercicios.", Toast.LENGTH_SHORT).show()}
        }

        // Si estamos editando, pre-rellenar los campos
        if (isEditing && existingSet != null) {
            // TODO: Asegúrate de que los IDs en dialog_add_set.xml son correctos
            dialogBinding.editTextExerciseName.setText(existingSet.exerciseName, false) // false para no filtrar
            dialogBinding.editTextReps.setText(existingSet.repetitions.toString())
            dialogBinding.editTextWeight.setText(existingSet.weight.toString())
            Log.d("WorkoutDetailFragment", "Dialog pre-filled for editing set ID: ${existingSet.id}")
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(if (isEditing) "Editar Serie" else "Añadir Nueva Serie")
        builder.setView(dialogBinding.root)

        builder.setPositiveButton(if (isEditing) "Actualizar" else "Guardar") { dialog, _ ->
            // TODO: Asegúrate de que los IDs en dialog_add_set.xml son correctos
            val exerciseName = dialogBinding.editTextExerciseName.text.toString().trim()
            val repsString = dialogBinding.editTextReps.text.toString()
            val weightString = dialogBinding.editTextWeight.text.toString()

            if (exerciseName.isEmpty() || repsString.isEmpty() || weightString.isEmpty()) {
                context?.let { Toast.makeText(it, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show() }
                return@setPositiveButton // No cerrar el diálogo si hay error
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
                dialog.dismiss() // Cerrar el diálogo solo si todo fue bien
            } catch (e: NumberFormatException) {
                context?.let { Toast.makeText(it, "Introduce números válidos para Reps y Peso", Toast.LENGTH_SHORT).show() }
            }
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }

        try {
            builder.create().show()
            Log.d("WorkoutDetailFragment", "Add/Edit dialog shown successfully. Editing: $isEditing")
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
                Log.d("WorkoutDetailFragment", "Delete confirmed for set ID: ${setToDelete.id}")
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

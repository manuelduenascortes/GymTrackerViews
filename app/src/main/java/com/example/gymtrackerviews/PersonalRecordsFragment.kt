package com.example.gymtrackerviews.ui.statistics

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymtrackerviews.GymTrackerApplication
import com.example.gymtrackerviews.databinding.FragmentPersonalRecordsBinding // Importante
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.gymtrackerviews.databinding.ItemPersonalRecordBinding

class PersonalRecordsFragment : Fragment() {

    private var _binding: FragmentPersonalRecordsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by activityViewModels {
        val application = requireActivity().application as GymTrackerApplication
        StatisticsViewModelFactory(
            application.database.workoutDao(),
            application.database.workoutSetDao(),
            application.database.exerciseDao()
        )
    }

    private lateinit var personalRecordAdapter: PersonalRecordAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPersonalRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Mis Récords Personales"

        setupPersonalRecordsRecyclerView()
        observePersonalRecords()
    }

    private fun setupPersonalRecordsRecyclerView() {
        personalRecordAdapter = PersonalRecordAdapter()
        binding.recyclerViewPersonalRecords.apply {
            adapter = personalRecordAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observePersonalRecords() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.personalRecords.collectLatest { records ->
                    Log.d("PersonalRecordsFragment", "Received ${records.size} personal records")
                    if (records.isEmpty()) {
                        binding.recyclerViewPersonalRecords.visibility = View.GONE
                        binding.textViewNoPersonalRecords.visibility = View.VISIBLE
                    } else {
                        binding.recyclerViewPersonalRecords.visibility = View.VISIBLE
                        binding.textViewNoPersonalRecords.visibility = View.GONE
                        personalRecordAdapter.submitList(records)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class PersonalRecordAdapter : ListAdapter<PersonalRecordItem, PersonalRecordAdapter.PersonalRecordViewHolder>(PersonalRecordDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd MMM yy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonalRecordViewHolder {
        val binding = ItemPersonalRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PersonalRecordViewHolder(binding, dateFormat)
    }

    override fun onBindViewHolder(holder: PersonalRecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PersonalRecordViewHolder(
        private val binding: ItemPersonalRecordBinding,
        private val dateFormat: SimpleDateFormat
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: PersonalRecordItem) {
            binding.textViewPrExerciseName.text = record.exerciseName
            val weightStr = if (record.maxWeight == record.maxWeight.toInt().toDouble()) record.maxWeight.toInt().toString() else String.format(Locale.getDefault(),"%.1f", record.maxWeight)
            binding.textViewPrDetails.text = "Récord: ${weightStr} kg x ${record.repsAtMaxWeight} reps"
            var dateAndWorkoutInfo = "Logrado el ${dateFormat.format(record.dateOfRecord)}"
            if (!record.workoutName.isNullOrBlank()){
                dateAndWorkoutInfo += " (en ${record.workoutName})"
            }
            binding.textViewPrDateAndWorkout.text = dateAndWorkoutInfo
        }
    }
}

class PersonalRecordDiffCallback : DiffUtil.ItemCallback<PersonalRecordItem>() {
    override fun areItemsTheSame(oldItem: PersonalRecordItem, newItem: PersonalRecordItem): Boolean {
        return oldItem.exerciseName == newItem.exerciseName
    }

    override fun areContentsTheSame(oldItem: PersonalRecordItem, newItem: PersonalRecordItem): Boolean {
        return oldItem == newItem
    }
}

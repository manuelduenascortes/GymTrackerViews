package com.example.gymtrackerviews.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.gymtrackerviews.GymTrackerApplication
import com.example.gymtrackerviews.R
import com.example.gymtrackerviews.databinding.FragmentStatisticsBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels {
        val application = requireActivity().application as GymTrackerApplication
        StatisticsViewModelFactory(
            application.database.workoutDao(),
            application.database.workoutSetDao(),
            application.database.exerciseDao()
        )
    }

    private lateinit var exerciseFilterAdapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("StatisticsFragment", "onViewCreated called")
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Estadísticas"

        setupCharts()
        setupExerciseFilter()
        setupButtonListeners()
        observeViewModelData()
    }

    private fun setupButtonListeners() {
        binding.buttonViewPersonalRecords.setOnClickListener {
            if (isAdded) {
                try {
                    findNavController().navigate(R.id.action_statisticsFragment_to_personalRecordsFragment)
                } catch (e: Exception) {
                    Log.e("StatisticsFragment", "Error al navegar a récords personales", e)
                    Toast.makeText(context, "Error al abrir récords", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupCharts() {
        setupLineChart(
            binding.lineChartWorkoutsPerWeek,
            "Entrenamientos por Semana"
        )
        setupLineChart(
            binding.lineChartExerciseProgress,
            "Progreso por Ejercicio"
        )
    }

    private fun setupLineChart(chart: LineChart, descriptionText: String) {
        chart.apply {
            description.isEnabled = true
            description.text = descriptionText
            description.textSize = 10f
            context?.let {
                description.textColor = MaterialColors.getColor(it, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY)
            }
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            legend.isEnabled = true
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            context?.let { ctx ->
                val textColor = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
                xAxis.textColor = textColor
                axisLeft.textColor = textColor
                legend.textColor = textColor
            }
            axisLeft.setDrawGridLines(true)
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
        }
    }

    private fun setupExerciseFilter() {
        exerciseFilterAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        binding.autoCompleteTextViewExerciseFilter.setAdapter(exerciseFilterAdapter)

        binding.autoCompleteTextViewExerciseFilter.setOnItemClickListener { parent, _, position, _ ->
            val selectedExercise = parent.getItemAtPosition(position) as String
            viewModel.setSelectedExercise(selectedExercise)
            // Opcional: quitar el foco para que el teclado no aparezca si se usara inputType != none
            // binding.autoCompleteTextViewExerciseFilter.clearFocus()
        }

        // <<< AÑADIDO: OnClickListener para mostrar el desplegable al tocar >>>
        binding.autoCompleteTextViewExerciseFilter.setOnClickListener {
            // Si el adapter tiene items, muestra el desplegable.
            // Esto es útil si el usuario toca el campo después de haber seleccionado algo
            // y quiere ver la lista de nuevo.
            if (exerciseFilterAdapter.count > 0) {
                binding.autoCompleteTextViewExerciseFilter.showDropDown()
            }
        }
        // <<< FIN AÑADIDO >>>
    }

    private fun observeViewModelData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.workoutsPerWeek.collectLatest { weeklyData ->
                        Log.d("StatisticsFragment", "Received ${weeklyData.size} weekly data points")
                        if (weeklyData.size < 2 && weeklyData.none { it.value > 0}) {
                            binding.lineChartWorkoutsPerWeek.visibility = View.GONE
                            binding.textViewNoDataWorkoutsPerWeek.visibility = View.VISIBLE
                        } else {
                            binding.lineChartWorkoutsPerWeek.visibility = View.VISIBLE
                            binding.textViewNoDataWorkoutsPerWeek.visibility = View.GONE
                            binding.lineChartWorkoutsPerWeek.xAxis.valueFormatter = WeekAxisValueFormatter()
                            updateChartWithData(
                                binding.lineChartWorkoutsPerWeek,
                                weeklyData,
                                "Nº Entrenamientos",
                                isWeeklyData = true
                            )
                        }
                    }
                }

                launch {
                    viewModel.allExerciseNames.collectLatest { names ->
                        Log.d("StatisticsFragment", "Received ${names.size} exercise names for filter")
                        // Guardar el texto actual antes de limpiar, si es que hay alguno
                        val currentText = binding.autoCompleteTextViewExerciseFilter.text.toString()
                        val currentFilterActive = viewModel.selectedExerciseProgress.value.isNotEmpty() && names.contains(currentText)


                        exerciseFilterAdapter.clear()
                        exerciseFilterAdapter.addAll(names)
                        // No llamar a notifyDataSetChanged() aquí si se va a restaurar el texto,
                        // ya que el setAdapter y el setText lo harán implícitamente.
                        // Si el texto actual era un filtro válido, y la lista de nombres lo sigue incluyendo,
                        // se podría intentar restaurarlo para que no se borre visualmente al actualizar la lista de ejercicios.
                        // Sin embargo, para un dropdown, normalmente se espera que se limpie o se mantenga la selección
                        // si la lista de origen cambia.
                        // Por ahora, dejaremos que se actualice. Si el usuario ya había seleccionado algo,
                        // el gráfico de progreso para ese ejercicio seguirá mostrándose.
                        // Y si toca el AutoCompleteTextView, la lista completa aparecerá.
                    }
                }

                launch {
                    viewModel.selectedExerciseProgress.collectLatest { exerciseProgressData ->
                        val selectedExercise = binding.autoCompleteTextViewExerciseFilter.text.toString()
                        Log.d("StatisticsFragment", "Received ${exerciseProgressData.size} progress data points for $selectedExercise")

                        if (selectedExercise.isBlank()){
                            binding.lineChartExerciseProgress.visibility = View.GONE
                            binding.textViewNoDataExerciseProgress.text = "Selecciona un ejercicio para ver el progreso."
                            binding.textViewNoDataExerciseProgress.visibility = View.VISIBLE
                        } else if (exerciseProgressData.size < 2 && exerciseProgressData.none { it.value > 0}) {
                            binding.lineChartExerciseProgress.visibility = View.GONE
                            binding.textViewNoDataExerciseProgress.text = "No hay suficientes datos para '$selectedExercise'."
                            binding.textViewNoDataExerciseProgress.visibility = View.VISIBLE
                        } else {
                            binding.lineChartExerciseProgress.visibility = View.VISIBLE
                            binding.textViewNoDataExerciseProgress.visibility = View.GONE
                            binding.lineChartExerciseProgress.xAxis.valueFormatter = DateAxisValueFormatter()
                            updateChartWithData(
                                binding.lineChartExerciseProgress,
                                exerciseProgressData,
                                "Max Peso (kg) - $selectedExercise",
                                isWeeklyData = false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun updateChartWithData(chart: LineChart, dataPoints: List<ChartEntryData>, label: String, isWeeklyData: Boolean) {
        val entries = ArrayList<Entry>()
        val dates = ArrayList<Date>()

        dataPoints.forEachIndexed { index, dataPoint ->
            entries.add(Entry(index.toFloat(), dataPoint.value))
            dates.add(dataPoint.date)
        }

        if (entries.isEmpty()) {
            chart.clear()
            chart.invalidate()
            Log.d("StatisticsFragment", "Chart '$label' cleared (no entries).")
            return
        }

        val lineDataSet = LineDataSet(entries, label)
        context?.let { ctx ->
            val primaryColor = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorPrimary, Color.BLUE)
            val onSurfaceColor = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
            lineDataSet.color = primaryColor
            lineDataSet.valueTextColor = onSurfaceColor
            lineDataSet.setCircleColor(primaryColor)
            lineDataSet.fillColor = primaryColor
            lineDataSet.setDrawFilled(true)
            lineDataSet.fillAlpha = 65
        }
        lineDataSet.lineWidth = 2f
        lineDataSet.circleRadius = 4f
        lineDataSet.setDrawValues(true)
        lineDataSet.valueTextSize = 10f
        lineDataSet.valueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                return if (isWeeklyData) {
                    entry?.y?.toInt().toString()
                } else {
                    String.format(Locale.getDefault(), "%.1f", entry?.y)
                }
            }
        }

        val lineData = LineData(lineDataSet)
        chart.data = lineData

        if (chart.xAxis.valueFormatter is DateAxisValueFormatter) {
            (chart.xAxis.valueFormatter as DateAxisValueFormatter).setDates(dates)
        } else if (chart.xAxis.valueFormatter is WeekAxisValueFormatter) {
            (chart.xAxis.valueFormatter as WeekAxisValueFormatter).setDates(dates)
        }

        chart.invalidate()
        chart.animateX(500)
        Log.d("StatisticsFragment", "Chart '$label' updated with ${entries.size} entries.")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Formateadores de ejes (se mantienen)
class DateAxisValueFormatter : ValueFormatter() {
    private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    private var dates: List<Date> = emptyList()

    fun setDates(dates: List<Date>) {
        this.dates = dates
    }

    override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
        val index = value.toInt()
        return if (index >= 0 && index < dates.size) {
            dateFormat.format(dates[index])
        } else {
            ""
        }
    }
}

class WeekAxisValueFormatter : ValueFormatter() {
    private val calendar = Calendar.getInstance()
    private var dates: List<Date> = emptyList()

    fun setDates(dates: List<Date>) {
        this.dates = dates
    }

    override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
        val index = value.toInt()
        return if (index >= 0 && index < dates.size) {
            calendar.time = dates[index]
            val week = calendar.get(Calendar.WEEK_OF_YEAR)
            "Sem ${week}"
        } else {
            ""
        }
    }
}

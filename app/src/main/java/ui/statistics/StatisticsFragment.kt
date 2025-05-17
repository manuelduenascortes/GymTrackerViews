package com.example.gymtrackerviews.ui.statistics // Asegúrate que este es el paquete correcto

import android.graphics.Color // Necesario para Color.BLACK o Color.GRAY si los usas como fallback
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.gymtrackerviews.GymTrackerApplication
import com.example.gymtrackerviews.R // Necesario para R.attr
import com.example.gymtrackerviews.WorkoutListViewModel
import com.example.gymtrackerviews.WorkoutListViewModelFactory
import com.example.gymtrackerviews.WorkoutSummary
import com.example.gymtrackerviews.databinding.FragmentStatisticsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.color.MaterialColors // IMPORTANTE: Para obtener colores del tema
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
// import java.util.concurrent.TimeUnit // No se usa en esta versión

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutListViewModel by viewModels {
        val application = requireActivity().application as GymTrackerApplication
        WorkoutListViewModelFactory(application.database.workoutDao())
    }

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
        setupChart()
        observeWorkoutData()
    }

    private fun setupChart() {
        binding.lineChartSeriesProgress.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            legend.isEnabled = true

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.valueFormatter = DateAxisValueFormatter()

            // Obtener color del tema para las etiquetas del eje X
            context?.let { ctx ->
                val textColor = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
                xAxis.textColor = textColor
                axisLeft.textColor = textColor
            }


            axisLeft.setDrawGridLines(true)
            axisLeft.axisMinimum = 0f

            axisRight.isEnabled = false
        }
    }

    private fun observeWorkoutData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allWorkoutSummaries.collectLatest { workoutSummaries ->
                    Log.d("StatisticsFragment", "Received ${workoutSummaries.size} workout summaries")
                    if (workoutSummaries.size < 2) {
                        binding.lineChartSeriesProgress.visibility = View.GONE
                        binding.textViewNoData.visibility = View.VISIBLE
                        Log.d("StatisticsFragment", "Not enough data to display chart.")
                    } else {
                        binding.lineChartSeriesProgress.visibility = View.VISIBLE
                        binding.textViewNoData.visibility = View.GONE
                        updateChartData(workoutSummaries)
                    }
                }
            }
        }
    }

    private fun updateChartData(summaries: List<WorkoutSummary>) {
        val sortedSummaries = summaries.sortedBy { it.workout.startTime.time }
        val entries = ArrayList<Entry>()
        sortedSummaries.forEachIndexed { index, summary ->
            entries.add(Entry(index.toFloat(), summary.setCount.toFloat()))
        }

        if (entries.isEmpty()) {
            binding.lineChartSeriesProgress.clear()
            binding.lineChartSeriesProgress.invalidate()
            binding.lineChartSeriesProgress.visibility = View.GONE
            binding.textViewNoData.visibility = View.VISIBLE
            return
        }

        val lineDataSet = LineDataSet(entries, "Número de Series por Workout")

        // Estilo del dataset usando colores del tema
        context?.let { ctx ->
            // Para colorPrimary, usamos el atributo de Material Components
            val primaryColor = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorPrimary, Color.BLUE)
            // Para colorOnSurface (texto sobre el fondo principal)
            val onSurfaceColor = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)

            lineDataSet.color = primaryColor
            lineDataSet.valueTextColor = onSurfaceColor // Color del texto de los valores en el gráfico
            lineDataSet.setCircleColor(primaryColor)
        }

        lineDataSet.lineWidth = 2f
        lineDataSet.circleRadius = 4f
        lineDataSet.setDrawValues(true)
        lineDataSet.valueTextSize = 10f
        lineDataSet.valueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                return entry?.y?.toInt().toString()
            }
        }

        val lineData = LineData(lineDataSet)
        binding.lineChartSeriesProgress.data = lineData
        (binding.lineChartSeriesProgress.xAxis.valueFormatter as? DateAxisValueFormatter)?.setDates(sortedSummaries.map { it.workout.startTime })
        binding.lineChartSeriesProgress.invalidate()
        Log.d("StatisticsFragment", "Chart data updated with ${entries.size} entries.")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

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

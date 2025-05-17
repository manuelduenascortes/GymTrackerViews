package com.example.gymtrackerviews.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.gymtrackerviews.GymTrackerApplication
import com.example.gymtrackerviews.R // Necesario para R.string
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
import java.util.Date
import java.util.Locale

// Asegúrate de que StatisticsViewModel y StatisticsViewModelFactory están importados correctamente
// import com.example.gymtrackerviews.viewmodel.StatisticsViewModel
// import com.example.gymtrackerviews.viewmodel.StatisticsViewModelFactory
// import com.example.gymtrackerviews.ChartEntryData // Si está en otro archivo

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels {
        val application = requireActivity().application as GymTrackerApplication
        StatisticsViewModelFactory(application.database.workoutDao(), application.database.workoutSetDao())
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
        setupCharts()
        observeViewModelData()
    }

    private fun setupCharts() {
        // CAMBIO: Se elimina el tercer argumento 'label' de las llamadas a setupLineChart
        setupLineChart(
            binding.lineChartSeriesProgress,
            "Series por Workout" // Este es el descriptionText
        )
        setupLineChart(
            binding.lineChartVolumeProgress,
            "Volumen Total (kg) por Workout" // Este es el descriptionText
        )
    }

    // CAMBIO: Se elimina el parámetro 'label: String' de la firma del método
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
            legend.isEnabled = true // La leyenda mostrará el label del LineDataSet (que se pone en updateChartWithData)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.valueFormatter = DateAxisValueFormatter()

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


    private fun observeViewModelData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.seriesPerWorkout.collectLatest { seriesData ->
                        Log.d("StatisticsFragment", "Received ${seriesData.size} series data points")
                        if (seriesData.size < 2) {
                            binding.lineChartSeriesProgress.visibility = View.GONE
                            binding.textViewNoDataSeries.visibility = View.VISIBLE
                        } else {
                            binding.lineChartSeriesProgress.visibility = View.VISIBLE
                            binding.textViewNoDataSeries.visibility = View.GONE
                            updateChartWithData(
                                binding.lineChartSeriesProgress,
                                seriesData,
                                getString(R.string.chart_series_label) // La etiqueta para el DataSet se pasa aquí
                            )
                        }
                    }
                }

                launch {
                    viewModel.volumePerWorkout.collectLatest { volumeData ->
                        Log.d("StatisticsFragment", "Received ${volumeData.size} volume data points")
                        if (volumeData.size < 2) {
                            binding.lineChartVolumeProgress.visibility = View.GONE
                            binding.textViewNoDataVolume.visibility = View.VISIBLE
                        } else {
                            binding.lineChartVolumeProgress.visibility = View.VISIBLE
                            binding.textViewNoDataVolume.visibility = View.GONE
                            updateChartWithData(
                                binding.lineChartVolumeProgress,
                                volumeData,
                                getString(R.string.chart_volume_label) // La etiqueta para el DataSet se pasa aquí
                            )
                        }
                    }
                }
            }
        }
    }

    // El parámetro 'label' aquí SÍ se usa para el LineDataSet
    private fun updateChartWithData(chart: LineChart, dataPoints: List<ChartEntryData>, label: String) {
        val entries = ArrayList<Entry>()
        val dates = ArrayList<Date>()

        dataPoints.forEachIndexed { index, dataPoint ->
            entries.add(Entry(index.toFloat(), dataPoint.value))
            dates.add(dataPoint.date)
        }

        if (entries.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

        val lineDataSet = LineDataSet(entries, label) // 'label' se usa aquí
        context?.let { ctx ->
            val primaryColor = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorPrimary, Color.BLUE)
            val onSurfaceColor = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
            lineDataSet.color = primaryColor
            lineDataSet.valueTextColor = onSurfaceColor
            lineDataSet.setCircleColor(primaryColor)
        }
        lineDataSet.lineWidth = 2f
        lineDataSet.circleRadius = 4f
        lineDataSet.setDrawValues(true)
        lineDataSet.valueTextSize = 10f
        lineDataSet.valueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                return if (label == getString(R.string.chart_volume_label)) {
                    String.format(Locale.getDefault(), "%.1f", entry?.y)
                } else {
                    entry?.y?.toInt().toString()
                }
            }
        }

        val lineData = LineData(lineDataSet)
        chart.data = lineData
        (chart.xAxis.valueFormatter as? DateAxisValueFormatter)?.setDates(dates)
        chart.invalidate()
        Log.d("StatisticsFragment", "Chart '$label' updated with ${entries.size} entries.")
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

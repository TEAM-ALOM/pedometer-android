package com.example.pedometer.fragment
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.pedometer.BaseFragment
import com.example.pedometer.R
import com.example.pedometer.databinding.FragmentDayBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class Day(private val stepsCount: Int, private val stepsGoal: Int, private val selectedMonth: Int, private val selectedDay: Int) : BaseFragment<FragmentDayBinding>() {

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDayBinding {
        return FragmentDayBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updatePieChart()
    }

    private fun updatePieChart() {
        val stepsRemain = stepsGoal - stepsCount // 남은 걸음수 설정
        val stepsRatio = stepsCount.toFloat() / stepsGoal.toFloat() * 100 // 걸음수 비율 계산

        binding.viewSteps.text = getString(R.string.steps_percentage, stepsCount.toString(), stepsGoal.toString())
        binding.viewDate.text = getString(R.string.current_date, selectedMonth.toString(), selectedDay.toString())

        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(stepsCount.toFloat(), "이만큼 걸었어요"))

        // 오늘 걸음수가 목표 걸음수를 초과하면 목표 걸음수로 설정
        val effectiveStepsGoal = if (stepsCount > stepsGoal) stepsCount else stepsGoal
        val stepsRemainOrGoal = effectiveStepsGoal - stepsCount
        entries.add(PieEntry(stepsRemainOrGoal.toFloat(), "이만큼 남았어요"))

        val dataSet = PieDataSet(entries, "Sample Data")

        // 걸음수 비율에 따라 색상 설정
        dataSet.colors = when {
            stepsRatio == 100f -> listOf(Color.BLUE, Color.BLUE) // 남은 걸음수가 0%면 파란색
            stepsRatio >= 50f -> listOf(Color.BLUE, Color.YELLOW) // 0% 초과 ~ 50% 이하면 노란색
            else -> listOf(Color.BLUE, Color.RED) // 50% 초과 ~ 100% 이하면 빨간색
        }

        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 28f

        val data = PieData(dataSet)
        binding.chart.data = data

        binding.chart.setUsePercentValues(false)
        binding.chart.description?.isEnabled = false
        binding.chart.invalidate()
    }
}

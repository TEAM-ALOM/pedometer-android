package com.example.pedometer.fragment
import BaseFragment
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.pedometer.MainActivity
import com.example.pedometer.R
import com.example.pedometer.databinding.FragmentDayBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class Day : BaseFragment<FragmentDayBinding>() {

    override fun getFragmentBinding(//뷰바인딩 메소드 호출
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDayBinding {
        return FragmentDayBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val stepsToday=MainActivity.GlobalVariables.stepsNow//오늘 걸음수
        val stepsGoal=MainActivity.GlobalVariables.stepsGoal
        val month=7//월
        val day=7//일
        var textSteps=binding.viewSteps
        var textDate=binding.viewDate
        val stepsRemain=stepsGoal-stepsToday//남은 걸음수 설정
        textDate.text = getString(R.string.current_date, month.toString(), day.toString())
        textSteps.text = getString(R.string.steps_display, stepsToday.toString(), stepsGoal.toString())
        var pieChart = binding.chart


        val entries = ArrayList<PieEntry>()//파이차트 데이터 리스트
        entries.add(PieEntry(stepsToday.toFloat(), "이만큼 걸었어요"))
        entries.add(PieEntry(stepsRemain.toFloat(), "이만큼 남았어요"))

        val dataSet = PieDataSet(entries, "Sample Data")//파이차트 설정
        dataSet.colors = listOf(Color.GREEN, Color.BLUE)
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 14f


        val data = PieData(dataSet)
        pieChart.data = data

        pieChart.setUsePercentValues(false)
        pieChart.description.isEnabled = false

        pieChart.invalidate()
        updatePieChart(stepsToday, stepsGoal)
    }
    fun updatePieChart(stepsToday: Int, stepsGoal: Int) {
        val stepsRemain = stepsGoal - stepsToday // 남은 걸음수 설정

        binding.viewSteps.text = "$stepsToday / $stepsGoal"

        val entries = ArrayList<PieEntry>() // 파이차트 데이터 리스트
        entries.add(PieEntry(stepsToday.toFloat(), "이만큼 걸었어요"))
        entries.add(PieEntry(stepsRemain.toFloat(), "이만큼 남았어요"))

        val dataSet = PieDataSet(entries, "Sample Data") // 파이차트 설정
        dataSet.colors = listOf(Color.GREEN, Color.BLUE)
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 14f

        val data = PieData(dataSet)
        binding.chart.data = data

        binding.chart.setUsePercentValues(false)
        binding.chart.description.isEnabled = false

        binding.chart.invalidate()
    }
}
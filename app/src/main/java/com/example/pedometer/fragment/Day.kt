//
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.pedometer.R
import com.example.pedometer.databinding.FragmentDayBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class Day : BaseFragment<FragmentDayBinding>() {
    private var month = 1//is
    private var day = 1//is


    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDayBinding {
        return FragmentDayBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sharedPrefs = requireContext().getSharedPreferences("stepsData", Context.MODE_PRIVATE)
        super.onViewCreated(view, savedInstanceState)


        val stepsDay=0//오늘 걸음수
        val stepsGoal=0//목표 걸음수

        var textSteps=binding.viewSteps
        var textDate=binding.viewDate
        val stepsRemain=stepsGoal-stepsDay//남은 걸음수 설정

        val pieChart = binding.chart


        val entries = ArrayList<PieEntry>()//파이차트 데이터 리스트
        entries.add(PieEntry(stepsDay.toFloat(), "이만큼 걸었어요"))
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
        updatePieChart(stepsDay, stepsGoal,month,day)
        textDate.text = getString(R.string.current_date, month.toString(), day.toString())
        textSteps.text = getString(R.string.steps_display, stepsDay.toString(), stepsGoal.toString())
    }

    fun updatePieChart(stepsToday: Int, stepsGoal: Int,month:Int,day: Int) {
        val stepsRemain = stepsGoal - stepsToday // 남은 걸음수 설정

        _binding?.viewSteps?.text = "$stepsToday / $stepsGoal"

        val entries = ArrayList<PieEntry>() // 파이차트 데이터 리스트
        entries.add(PieEntry(stepsToday.toFloat(), "이만큼 걸었어요"))
        entries.add(PieEntry(stepsRemain.toFloat(), "이만큼 남았어요"))

        val dataSet = PieDataSet(entries, "Sample Data") // 파이차트 설정
        dataSet.colors = listOf(Color.CYAN, Color.YELLOW)
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 28f

        val data = PieData(dataSet)
        _binding?.chart?.data = data

        _binding?.chart?.setUsePercentValues(false)
        _binding?.chart?.description?.isEnabled = false

        _binding?.chart?.invalidate()
    }

    fun setStepsData(stepsCount: Int, stepsGoal: Int, selectedMonth: Int, selectedDay: Int) {
        month=selectedMonth
        day=selectedDay
        updatePieChart(stepsCount, stepsGoal,month,day)
    }
}

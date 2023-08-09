//
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

class Day( val stepsCount: Int,  val stepsGoal: Int,  val selectedMonth: Int,  val selectedDay: Int) : BaseFragment<FragmentDayBinding>() {

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

    fun updatePieChart() {

        val stepsRemain = stepsGoal - stepsCount // 남은 걸음수 설정
        binding.viewSteps.text = getString(R.string.steps_percentage, stepsCount.toString(), stepsGoal.toString())
        binding.viewDate.text = getString(R.string.current_date, selectedMonth.toString(), selectedDay.toString())


        val entries = ArrayList<PieEntry>() // 파이차트 데이터 리스트
        entries.add(PieEntry(stepsCount.toFloat(), "이만큼 걸었어요"))
        entries.add(PieEntry(stepsRemain.toFloat(), "이만큼 남았어요"))

        val dataSet = PieDataSet(entries, "Sample Data") // 파이차트 설정
        dataSet.colors = listOf(Color.CYAN, Color.YELLOW)
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 28f

        val data = PieData(dataSet)
        binding.chart.data = data

        binding.chart.setUsePercentValues(false)
        binding.chart.description?.isEnabled = false
        binding.chart.invalidate()
    }

}

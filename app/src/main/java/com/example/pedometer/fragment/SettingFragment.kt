
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.example.pedometer.MainActivity
import com.example.pedometer.databinding.FragmentSettingBinding

class SettingFragment : BaseFragment<FragmentSettingBinding>() {

    private val PREFS_NAME = "MyPrefs"
    private val PREF_TARGET_STEPS = "TargetSteps"

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingBinding {
        return FragmentSettingBinding.inflate(inflater, container, false)
    }

    private fun showPopup(stepsGoal: Int) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("목표 걸음 수 설정")
            .setMessage("목표 걸음 수가 설정되었습니다!\n목표 걸음 수: $stepsGoal")
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()

        // 목표 걸음 수를 SharedPreferences에 저장
        val sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putInt(PREF_TARGET_STEPS, stepsGoal)
            apply()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val confirmButton = binding.stepsSetting
        val personalStepsSetting = binding.personalStepsSetting

        // SharedPreferences에서 기존 저장된 값을 가져옴 (기본값: 10000)
        val sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedStepsGoal = sharedPrefs.getInt(PREF_TARGET_STEPS, 10000)
        personalStepsSetting.setText(savedStepsGoal.toString())
        personalStepsSetting.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val targetSteps = personalStepsSetting.text.toString().toIntOrNull()
                if (targetSteps != null) {
                    // 목표 걸음 수를 GlobalVariables에 저장
                    MainActivity.GlobalVariables.stepsGoal = targetSteps
                    // 팝업 띄우기
                    showPopup(targetSteps)
                    // 현재 Fragment를 닫음
                    requireActivity().supportFragmentManager.popBackStack()
                }
                true
            } else {
                false
            }
        }

        confirmButton.setOnClickListener {
            val targetSteps = personalStepsSetting.text.toString().toIntOrNull()
            if (targetSteps != null) {
                // 목표 걸음 수를 GlobalVariables에 저장
                MainActivity.GlobalVariables.stepsGoal = targetSteps
                // 팝업 띄우기
                showPopup(targetSteps)
                // 현재 Fragment를 닫음
                requireActivity().supportFragmentManager.popBackStack()
            }
        }

        // ImageView 클릭 이벤트 처리

    }
}

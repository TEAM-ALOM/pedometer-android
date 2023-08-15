package com.example.pedometer.fragment
import BaseFragment
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.example.pedometer.databinding.FragmentSettingBinding

class SettingFragment : BaseFragment<FragmentSettingBinding>() {


    private fun showPopup(stepsGoal: Int) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("목표 걸음 수 설정")
            .setMessage("목표 걸음 수가 설정 되었습니다!\n목표 걸음 수: $stepsGoal")
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingBinding {
        return FragmentSettingBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val confirmButton = binding.btncheck
        val personalStepsSetting = binding.personalStepsSetting

        // SharedPreference 기존 저장된 값을 가져옴 (기본값: 10000)
        val sharedPrefs = requireContext().getSharedPreferences("stepsData", Context.MODE_PRIVATE)
        val savedStepsGoal = sharedPrefs.getInt("stepsGoal", 10000)
        personalStepsSetting.setText(savedStepsGoal.toString())

        personalStepsSetting.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val tstepsGoal = personalStepsSetting.text.toString().toIntOrNull()
                if (tstepsGoal != null) {
                    // 목표 걸음 수를 SharedPreferences 저장
                    with(sharedPrefs.edit()) {
                        putInt("stepsGoal", tstepsGoal)
                        apply()
                    }

                    // 팝업 띄우기
                    showPopup(tstepsGoal)

                    // 현재 Fragment 닫음
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
                // 목표 걸음 수를 SharedPreferences 저장
                with(sharedPrefs.edit()) {
                    putInt("stepsGoal", targetSteps)
                    apply()
                }

                // 팝업 띄우기
                showPopup(targetSteps)

                // 현재 Fragment 닫음
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
    }
}

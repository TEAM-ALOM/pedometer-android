package com.example.pedometer.fragment
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.ViewModelProvider
import com.example.pedometer.BaseFragment
import com.example.pedometer.Model.StepViewModel
import com.example.pedometer.Model.StepViewModelFactory
import com.example.pedometer.Model.StepsDAO
import com.example.pedometer.Model.StepsDatabase
import com.example.pedometer.databinding.FragmentSettingBinding
import com.example.pedometer.repository.StepRepository
import com.example.pedometer.repository.StepRepositoryImpl

class SettingFragment : BaseFragment<FragmentSettingBinding>() {

    private lateinit var stepViewModelFactory: StepViewModelFactory
    private lateinit var stepViewModel: StepViewModel
    private lateinit var stepRepository: StepRepository

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

                val newGoal = personalStepsSetting.text.toString().toIntOrNull() ?: savedStepsGoal
                updateGoal(newGoal,sharedPrefs)
                true
            } else {
                false
            }
        }

        confirmButton.setOnClickListener {
            updateGoal(savedStepsGoal,sharedPrefs)
        }
    }
    private fun updateGoal(updatestepsGoal: Int, sharedPrefs: SharedPreferences) {
        // 목표 걸음 수를 SharedPreferences에 저장
        val stepsDAO = StepsDatabase.getInstance(requireContext())?.stepsDAO() // StepsDAO를 가져옴
        val nonNullableStepsDAO: StepsDAO = stepsDAO ?: error("StepsDAO must not be null")
        stepRepository = StepRepositoryImpl(nonNullableStepsDAO, requireContext())
        stepViewModelFactory = StepViewModelFactory(stepRepository)
        stepViewModel = ViewModelProvider(this, stepViewModelFactory)[StepViewModel::class.java]
        stepViewModel.updateStepsGoal(updatestepsGoal)
        stepViewModel.stepsGoal.observe(this) { stepsGoal ->//라이브 데이터 사용
            with(sharedPrefs.edit()) {
                putInt("stepsGoal", stepsGoal)
                apply()
            }
        }

        showPopup(updatestepsGoal)
        // 현재 Fragment를 닫음
        requireActivity().supportFragmentManager.popBackStack()
    }
}

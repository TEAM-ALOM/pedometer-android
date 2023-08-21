package com.example.pedometer.fragment
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
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
import com.example.pedometer.MyForegroundService
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
        val stepsDAO = StepsDatabase.getInstance(requireContext()).stepsDAO()
        val nonNullableStepsDAO: StepsDAO = stepsDAO
        stepRepository = StepRepositoryImpl(requireContext(), stepsDAO)
        stepViewModelFactory = StepViewModelFactory(stepRepository)
        stepViewModel = ViewModelProvider(this, stepViewModelFactory)[StepViewModel::class.java]
        stepViewModel.updateStepsGoal(updatestepsGoal)
        stepViewModel.stepsGoal.observe(viewLifecycleOwner) { stepsGoal ->
            with(sharedPrefs.edit()) {
                putInt("stepsGoal", stepsGoal)
                apply()
            }
            val updatedGoal = sharedPrefs.getInt("stepsGoal", 0)
            showPopup(updatedGoal)
            val intent = Intent(requireContext(), MyForegroundService::class.java)
            intent.putExtra("stepsGoal", updatedGoal)
        }

        requireActivity().supportFragmentManager.popBackStack()
    }

}

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
import com.example.pedometer.BaseFragment
import com.example.pedometer.MyForegroundService
import com.example.pedometer.databinding.FragmentSettingBinding
import com.example.pedometer.model.StepViewModel
import com.example.pedometer.model.StepViewModelFactory
import com.example.pedometer.model.StepsDatabase
import com.example.pedometer.repository.StepRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

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

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        CoroutineScope(Dispatchers.IO).launch {
            // 데이터베이스에서 현재 날짜에 해당하는 StepsEntity 가져오기
            val stepsEntity = stepsDAO.getByDate(currentDate)

            stepsEntity?.let {
                it.goalSteps = updatestepsGoal // 목표 걸음 수 업데이트
                stepsDAO.update(it) // 데이터베이스 업데이트
            }

            // SharedPreferences에 목표 걸음 수 업데이트
            with(sharedPrefs.edit()) {
                putInt("stepsGoal", updatestepsGoal)
                apply()
            }

            val updatedGoal = sharedPrefs.getInt("stepsGoal", 0)
            withContext(Dispatchers.Main) {
                showPopup(updatedGoal)

                val intent = Intent(requireContext(), MyForegroundService::class.java)
                requireContext().startService(intent)
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
    }



}

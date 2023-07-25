package com.example.pedometer.fragment

import BaseFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.pedometer.MainActivity
import com.example.pedometer.databinding.FragmentSettingBinding

class SettingFragment : BaseFragment<FragmentSettingBinding>() {

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingBinding {
        return FragmentSettingBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val confirmButton = binding.stepsSetting
        val personalStepsSetting = binding.personalStepsSetting

        confirmButton.setOnClickListener {
            val targetSteps = personalStepsSetting.text.toString().toIntOrNull()
            if (targetSteps != null) {
                // 목표 걸음 수를 GlobalVariables에 저장
                MainActivity.GlobalVariables.stepsGoal = targetSteps
            }
        }
    }
}

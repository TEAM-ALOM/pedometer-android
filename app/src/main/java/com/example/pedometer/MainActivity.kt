package com.example.pedometer

import BaseActivity
import Day
import android.os.Bundle
import com.example.pedometer.databinding.ActivityMainBinding


class MainActivity : BaseActivity<ActivityMainBinding>({ ActivityMainBinding.inflate(it) }) {
    val textStepsToday by lazy { binding.viewStepsToday } // 현재 걸음 수
    val textStepsAvg by lazy { binding.viewStepsAvg } // 일주일간 평균 걸음 수
    object GlobalVariables {
        var stepsNow: Int = 0
        var stepsGoal: Int = 0
        var stepsAvg: Int = 0

    }
    var stepsNow=GlobalVariables.stepsNow
    var stepsGoal=GlobalVariables.stepsGoal
    var stepsAvg=GlobalVariables.stepsAvg
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = binding.root//뷰 바인딩
        setContentView(view)

        textStepsToday.setTextSize(14f)
        textStepsToday.setTextSize(28f)

        textStepsToday.text = "현재 $stepsNow 걸음"//현재 걸음 수
        textStepsAvg.text = "일주일간 평균 $stepsAvg 걸음을 걸었습니다."//평균 걸음 수
        supportFragmentManager.beginTransaction()// Day 프래그먼트 frame layout에 전시
            .add(R.id.frameLayout, Day())
            .commit()

    }
}

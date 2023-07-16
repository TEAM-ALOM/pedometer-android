package com.example.pedometer

import BaseActivity
import Day
import android.os.Bundle
import com.example.pedometer.databinding.ActivityMainBinding

class MainActivity :  BaseActivity<ActivityMainBinding>({ ActivityMainBinding.inflate(it)}) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = binding.root//뷰 바인딩
        setContentView(view)
        val textStepsToday=binding.viewStepsToday// 현재 걸음 수
        val textStepsAvg=binding.viewStepsAvg// 일주일간 평균 걸음 수
        textStepsToday.setTextSize(14f)
        textStepsToday.setTextSize(28f)

        val stepsNow=5000
        val stepsAvg=8000
        textStepsToday.text = "현재 $stepsNow 걸음"//현재 걸음 수
        textStepsAvg.text = "일주일간 평균 $stepsAvg 걸음을 걸었습니다."//평균 걸음 수
        supportFragmentManager.beginTransaction()// Day 프래그먼트 frame layout에 전시
            .add(R.id.frameLayout, Day())
            .commit()

    }

}

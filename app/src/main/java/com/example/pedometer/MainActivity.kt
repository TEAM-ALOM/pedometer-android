package com.example.pedometer

import BaseActivity
import android.os.Bundle
import com.example.pedometer.databinding.ActivityMainBinding

class MainActivity :  BaseActivity<ActivityMainBinding>({ ActivityMainBinding.inflate(it)}) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = binding.root
        setContentView(view)
        val textStepsToday=binding.viewStepsToday
        val textStepsAvg=binding.viewStepsAvg
        textStepsToday.setText("현재 5000 걸음")
        textStepsAvg.setText("일주일간 평균 8000걸음을 걸었습니다.")
        //setFragment()
    }
    /*private fun setFragment() {
        val transaction = supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainerView, Day())
        transaction.commit()
    }*/
}
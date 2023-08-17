package com.example.pedometer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val context = getApplication<Application>().applicationContext
    val db = StepsDatabase.getInstance(context)
    fun getData() = viewModelScope.launch(Dispatchers.IO) {
        db.stepsDAO().getAll()
    }

    fun insertData(text:String) = viewModelScope.launch(Dispatchers.IO) {
        db.stepsDAO().insert(StepsEntity("20230817", 7500, 8000))
    }
}

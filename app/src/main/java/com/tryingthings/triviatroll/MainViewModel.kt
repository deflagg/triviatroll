package com.tryingthings.triviatroll

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.State

class MainViewModel : ViewModel() {
    private val _theAnswer = mutableStateOf(String())
    val theAnswer: State<String> = _theAnswer

    fun updateAnswer(newAnswer: String) {
        _theAnswer.value = newAnswer
    }
}
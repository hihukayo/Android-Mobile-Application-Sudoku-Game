package com.example.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.sudoku.data.Session
import com.example.sudoku.sound.SoundManager
import com.example.sudoku.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Session.init(this)
        SoundManager.init(this)
        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}

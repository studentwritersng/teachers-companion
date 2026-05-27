package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.TeacherCompanionApp
import com.example.ui.TeacherViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: TeacherViewModel = viewModel()
      val isDark by viewModel.isDarkMode.collectAsState()
      MyApplicationTheme(darkTheme = isDark, dynamicColor = false) {
        TeacherCompanionApp(viewModel)
      }
    }
  }
}

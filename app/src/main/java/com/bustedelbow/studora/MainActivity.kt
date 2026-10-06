package com.bustedelbow.studora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.bustedelbow.studora.ui.theme.StudoraTheme
import com.bustedelbow.studora.ui.timer.TimerScreen
import dagger.hilt.android.AndroidEntryPoint

/** Single entry point. Hilt supplies the [com.bustedelbow.studora.ui.timer.TimerViewModel]. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudoraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TimerScreen(
                        viewModel = hiltViewModel(),
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

package com.bustedelbow.studora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.bustedelbow.studora.ui.theme.StudoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudoraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Slice 3 is theme-only: no screens yet. The empty Box
                    // consumes the Scaffold's inner padding so content added in
                    // later slices is not obscured; the timer and heatmap UI
                    // land then.
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
                }
            }
        }
    }
}

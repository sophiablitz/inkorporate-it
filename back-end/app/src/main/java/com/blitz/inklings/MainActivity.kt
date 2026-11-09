package com.blitz.inklings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.blitz.inklings.ui.DrawingScreen
import com.blitz.inklings.ui.theme.InklingsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InklingsTheme {
                DrawingScreen()
            }
        }
    }
}

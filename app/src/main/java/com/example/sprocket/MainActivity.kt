package com.example.sprocket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.sprocket.data.repository.SprocketRepository
import com.example.sprocket.theme.SprocketBg
import com.example.sprocket.theme.SprocketTheme
import com.example.sprocket.ui.SprocketApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val repository = remember { SprocketRepository(applicationContext) }
            val vehicleState by repository.vehicleState.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (vehicleState.themePreference) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemDark
            }
            SprocketTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SprocketBg
                ) {
                    SprocketApp(repository = repository)
                }
            }
        }
    }
}

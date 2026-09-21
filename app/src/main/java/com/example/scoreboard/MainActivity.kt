package com.example.scoreboard

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scoreboard.data.DarkMode
import com.example.scoreboard.data.ScreenOrientationMode
import com.example.scoreboard.ui.ScoreboardApp
import com.example.scoreboard.ui.theme.ScoreboardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ScoreboardViewModel = viewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val orientationMode = state?.screenOrientationMode

            LaunchedEffect(orientationMode) {
                val requested = when (orientationMode) {
                    ScreenOrientationMode.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    ScreenOrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    ScreenOrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    null -> return@LaunchedEffect
                }
                if (requestedOrientation != requested) requestedOrientation = requested
            }
            val systemDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (state?.darkMode) {
                DarkMode.SYSTEM -> isSystemInDarkTheme()
                DarkMode.LIGHT -> false
                DarkMode.DARK -> true
                null -> systemDarkTheme
            }

            ScoreboardTheme(
                appTheme = state?.appTheme ?: com.example.scoreboard.data.AppTheme.ENERGY,
                customHue = state?.customHue ?: 24f,
                darkTheme = darkTheme,
            ) { teamColors ->
                val loadedState = state
                if (loadedState == null) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                } else {
                    ScoreboardApp(
                        state = loadedState,
                        darkTheme = darkTheme,
                        teamColors = teamColors,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

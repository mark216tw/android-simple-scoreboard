package com.example.scoreboard.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "scoreboard")

class ScoreboardRepository(private val context: Context) {
    val state: Flow<ScoreboardState> = context.dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            ScoreboardState(
                homeName = preferences[Keys.HOME_NAME] ?: "主隊",
                awayName = preferences[Keys.AWAY_NAME] ?: "客隊",
                homeScore = preferences[Keys.HOME_SCORE] ?: 0,
                awayScore = preferences[Keys.AWAY_SCORE] ?: 0,
                homeRounds = preferences[Keys.HOME_ROUNDS] ?: 0,
                awayRounds = preferences[Keys.AWAY_ROUNDS] ?: 0,
                currentRound = preferences[Keys.CURRENT_ROUND] ?: 1,
                roundDisplay = enumValueOrDefault(
                    preferences[Keys.ROUND_DISPLAY],
                    RoundDisplay.PER_TEAM,
                ),
                appTheme = enumValueOrDefault(preferences[Keys.APP_THEME], AppTheme.ENERGY),
                customHue = preferences[Keys.CUSTOM_HUE] ?: 24f,
                darkMode = enumValueOrDefault(preferences[Keys.DARK_MODE], DarkMode.SYSTEM),
                keepScreenOn = preferences[Keys.KEEP_SCREEN_ON] ?: true,
                scoreSoundEnabled = preferences[Keys.SCORE_SOUND_ENABLED] ?: true,
                teamColorsSwapped = preferences[Keys.TEAM_COLORS_SWAPPED] ?: false,
                screenOrientationMode = enumValueOrDefault(
                    preferences[Keys.SCREEN_ORIENTATION_MODE],
                    ScreenOrientationMode.SYSTEM,
                ),
            )
        }

    suspend fun save(state: ScoreboardState) {
        context.dataStore.edit { preferences ->
            preferences[Keys.HOME_NAME] = state.homeName
            preferences[Keys.AWAY_NAME] = state.awayName
            preferences[Keys.HOME_SCORE] = state.homeScore
            preferences[Keys.AWAY_SCORE] = state.awayScore
            preferences[Keys.HOME_ROUNDS] = state.homeRounds
            preferences[Keys.AWAY_ROUNDS] = state.awayRounds
            preferences[Keys.CURRENT_ROUND] = state.currentRound
            preferences[Keys.ROUND_DISPLAY] = state.roundDisplay.name
            preferences[Keys.APP_THEME] = state.appTheme.name
            preferences[Keys.CUSTOM_HUE] = state.customHue
            preferences[Keys.DARK_MODE] = state.darkMode.name
            preferences[Keys.KEEP_SCREEN_ON] = state.keepScreenOn
            preferences[Keys.SCORE_SOUND_ENABLED] = state.scoreSoundEnabled
            preferences[Keys.TEAM_COLORS_SWAPPED] = state.teamColorsSwapped
            preferences[Keys.SCREEN_ORIENTATION_MODE] = state.screenOrientationMode.name
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private object Keys {
        val HOME_NAME = stringPreferencesKey("home_name")
        val AWAY_NAME = stringPreferencesKey("away_name")
        val HOME_SCORE = intPreferencesKey("home_score")
        val AWAY_SCORE = intPreferencesKey("away_score")
        val HOME_ROUNDS = intPreferencesKey("home_rounds")
        val AWAY_ROUNDS = intPreferencesKey("away_rounds")
        val CURRENT_ROUND = intPreferencesKey("current_round")
        val ROUND_DISPLAY = stringPreferencesKey("round_display")
        val APP_THEME = stringPreferencesKey("app_theme")
        val CUSTOM_HUE = floatPreferencesKey("custom_hue")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SCORE_SOUND_ENABLED = booleanPreferencesKey("score_sound_enabled")
        val TEAM_COLORS_SWAPPED = booleanPreferencesKey("team_colors_swapped")
        val SCREEN_ORIENTATION_MODE = stringPreferencesKey("screen_orientation_mode")
    }
}

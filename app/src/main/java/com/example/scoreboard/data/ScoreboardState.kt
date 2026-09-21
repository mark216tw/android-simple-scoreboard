package com.example.scoreboard.data

enum class RoundDisplay {
    HIDDEN,
    CURRENT,
    PER_TEAM,
}

enum class AppTheme {
    ENERGY,
    OCEAN,
    MINT,
    GRAPE,
    LEMON,
    MATCH,
    CUSTOM,
}

enum class DarkMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class ScreenOrientationMode {
    SYSTEM,
    PORTRAIT,
    LANDSCAPE,
}

internal fun ScreenOrientationMode.next() = when (this) {
    ScreenOrientationMode.SYSTEM -> ScreenOrientationMode.PORTRAIT
    ScreenOrientationMode.PORTRAIT -> ScreenOrientationMode.LANDSCAPE
    ScreenOrientationMode.LANDSCAPE -> ScreenOrientationMode.SYSTEM
}

data class ScoreboardState(
    val homeName: String = "主隊",
    val awayName: String = "客隊",
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val homeRounds: Int = 0,
    val awayRounds: Int = 0,
    val currentRound: Int = 1,
    val roundDisplay: RoundDisplay = RoundDisplay.PER_TEAM,
    val appTheme: AppTheme = AppTheme.ENERGY,
    val customHue: Float = 24f,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val keepScreenOn: Boolean = true,
    val scoreSoundEnabled: Boolean = true,
    val teamColorsSwapped: Boolean = false,
    val screenOrientationMode: ScreenOrientationMode = ScreenOrientationMode.SYSTEM,
)

data class ScoreRecord(
    val id: Long,
    val actionTeamName: String,
    val action: ScoreRecordAction,
    val value: Int,
    val homeName: String,
    val homeScore: Int,
    val awayName: String,
    val awayScore: Int,
    val roundSnapshot: RoundSnapshot,
    val timestampMillis: Long,
)

enum class ScoreRecordAction {
    ADJUSTMENT,
    RESET,
}

data class RoundSnapshot(
    val display: RoundDisplay,
    val currentRound: Int,
    val homeRounds: Int,
    val awayRounds: Int,
)

fun RoundSnapshot.displayText(): String = when (display) {
    RoundDisplay.HIDDEN -> "局數未啟用"
    RoundDisplay.CURRENT -> "第 $currentRound 局"
    RoundDisplay.PER_TEAM -> "局數 $homeRounds：$awayRounds"
}

fun RoundSnapshot.compactDisplayText(): String = when (display) {
    RoundDisplay.HIDDEN -> "未啟用"
    RoundDisplay.CURRENT -> "第 $currentRound 局"
    RoundDisplay.PER_TEAM -> "$homeRounds：$awayRounds"
}

internal fun ScoreboardState.roundSnapshot() = RoundSnapshot(
    display = roundDisplay,
    currentRound = currentRound,
    homeRounds = homeRounds,
    awayRounds = awayRounds,
)

internal data class ScoreAdjustment(
    val state: ScoreboardState,
    val teamName: String,
    val actualDelta: Int,
    val resultingScore: Int,
)

internal data class TeamScoreReset(
    val state: ScoreboardState,
    val teamName: String,
    val previousScore: Int,
)

internal fun ScoreboardState.adjustScore(isHome: Boolean, delta: Int): ScoreAdjustment {
    require(delta != 0) { "Score adjustment cannot be zero" }
    val oldScore = if (isHome) homeScore else awayScore
    val newScore = (oldScore.toLong() + delta)
        .coerceIn(0L, Int.MAX_VALUE.toLong())
        .toInt()
    val updated = if (isHome) copy(homeScore = newScore) else copy(awayScore = newScore)

    return ScoreAdjustment(
        state = updated,
        teamName = if (isHome) homeName else awayName,
        actualDelta = newScore - oldScore,
        resultingScore = newScore,
    )
}

internal fun ScoreboardState.resetTeamScore(isHome: Boolean): TeamScoreReset {
    val previousScore = if (isHome) homeScore else awayScore
    val updated = if (isHome) copy(homeScore = 0) else copy(awayScore = 0)
    return TeamScoreReset(
        state = updated,
        teamName = if (isHome) homeName else awayName,
        previousScore = previousScore,
    )
}

internal fun ScoreboardState.swapped() = copy(
    homeName = awayName,
    awayName = homeName,
    homeScore = awayScore,
    awayScore = homeScore,
    homeRounds = awayRounds,
    awayRounds = homeRounds,
    teamColorsSwapped = !teamColorsSwapped,
)

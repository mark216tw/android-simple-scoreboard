package com.example.scoreboard.data

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreboardStateTest {
    @Test
    fun roundSnapshotMatchesDisplayMode() {
        assertEquals(
            "局數未啟用",
            ScoreboardState(roundDisplay = RoundDisplay.HIDDEN).roundSnapshot().displayText(),
        )
        assertEquals(
            "第 3 局",
            ScoreboardState(
                roundDisplay = RoundDisplay.CURRENT,
                currentRound = 3,
            ).roundSnapshot().displayText(),
        )
        assertEquals(
            "局數 2：1",
            ScoreboardState(
                roundDisplay = RoundDisplay.PER_TEAM,
                homeRounds = 2,
                awayRounds = 1,
            ).roundSnapshot().displayText(),
        )
    }

    @Test
    fun resettingOneTeamDoesNotChangeTheOtherTeam() {
        val original = ScoreboardState(homeScore = 12, awayScore = 8)

        val homeReset = original.resetTeamScore(isHome = true)
        val awayReset = original.resetTeamScore(isHome = false)

        assertEquals(0, homeReset.state.homeScore)
        assertEquals(8, homeReset.state.awayScore)
        assertEquals(12, homeReset.previousScore)
        assertEquals(12, awayReset.state.homeScore)
        assertEquals(0, awayReset.state.awayScore)
        assertEquals(8, awayReset.previousScore)
    }

    @Test
    fun exportedHistoryUsesChronologicalOrderAndCompleteSnapshots() {
        val older = ScoreRecord(
            id = 1,
            actionTeamName = "紅隊",
            action = ScoreRecordAction.ADJUSTMENT,
            value = 3,
            homeName = "紅隊",
            homeScore = 3,
            awayName = "藍隊",
            awayScore = 0,
            roundSnapshot = RoundSnapshot(RoundDisplay.PER_TEAM, 1, 2, 1),
            timestampMillis = 0,
        )
        val newer = ScoreRecord(
            id = 2,
            actionTeamName = "藍隊",
            action = ScoreRecordAction.RESET,
            value = 8,
            homeName = "紅隊",
            homeScore = 3,
            awayName = "藍隊",
            awayScore = 0,
            roundSnapshot = RoundSnapshot(RoundDisplay.HIDDEN, 1, 0, 0),
            timestampMillis = 1_000,
        )

        val exported = formatScoreHistory(listOf(newer, older), zoneId = ZoneId.of("UTC"))

        assertTrue(exported.indexOf("操作：紅隊 +3") < exported.indexOf("操作：藍隊 歸零（8 → 0）"))
        assertTrue(exported.contains("紅隊：3"))
        assertTrue(exported.contains("藍隊：0"))
        assertTrue(exported.contains("局數：2：1"))
        assertTrue(exported.contains("局數：未啟用"))

        val table = formatScoreHistory(
            records = listOf(newer, older),
            format = ScoreHistoryFormat.TABLE,
            zoneId = ZoneId.of("UTC"),
        )

        assertTrue(table.contains("| 主隊分數 | 客隊分數 | 局數 | 時間 |"))
        assertTrue(table.indexOf("2：1") < table.indexOf("未啟用"))
        assertTrue(table.contains("| 3 | 0 | 2：1 |"))
        assertFalse(table.contains("操作："))
    }

    @Test
    fun screenOrientationCyclesThroughAllModes() {
        assertEquals(ScreenOrientationMode.PORTRAIT, ScreenOrientationMode.SYSTEM.next())
        assertEquals(ScreenOrientationMode.LANDSCAPE, ScreenOrientationMode.PORTRAIT.next())
        assertEquals(ScreenOrientationMode.SYSTEM, ScreenOrientationMode.LANDSCAPE.next())
    }

    @Test
    fun scoreAdjustmentsSupportOneTwoThreeAndMinusOne() {
        var state = ScoreboardState(homeScore = 4)

        state = state.adjustScore(isHome = true, delta = 1).state
        state = state.adjustScore(isHome = true, delta = 2).state
        state = state.adjustScore(isHome = true, delta = 3).state
        val result = state.adjustScore(isHome = true, delta = -1)

        assertEquals(9, result.state.homeScore)
        assertEquals(-1, result.actualDelta)
        assertEquals(9, result.resultingScore)
        assertEquals("主隊", result.teamName)
    }

    @Test
    fun scoreNeverFallsBelowZero() {
        val result = ScoreboardState(homeScore = 0).adjustScore(isHome = true, delta = -1)

        assertEquals(0, result.state.homeScore)
        assertEquals(0, result.actualDelta)
    }

    @Test
    fun swapMovesNamesScoresAndRoundsTogether() {
        val original = ScoreboardState(
            homeName = "紅隊",
            awayName = "藍隊",
            homeScore = 12,
            awayScore = 8,
            homeRounds = 2,
            awayRounds = 1,
            currentRound = 4,
        )

        val swapped = original.swapped()

        assertEquals("藍隊", swapped.homeName)
        assertEquals("紅隊", swapped.awayName)
        assertEquals(8, swapped.homeScore)
        assertEquals(12, swapped.awayScore)
        assertEquals(1, swapped.homeRounds)
        assertEquals(2, swapped.awayRounds)
        assertEquals(4, swapped.currentRound)
        assertEquals(true, swapped.teamColorsSwapped)

        assertEquals(false, swapped.swapped().teamColorsSwapped)
    }
}

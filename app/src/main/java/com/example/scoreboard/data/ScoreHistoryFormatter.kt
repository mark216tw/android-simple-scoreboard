package com.example.scoreboard.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val historyTimeFormatter = DateTimeFormatter.ofPattern(
    "yyyy/MM/dd HH:mm:ss",
    Locale.TAIWAN,
)

enum class ScoreHistoryFormat {
    DETAILED,
    TABLE,
}

fun formatScoreHistory(
    records: List<ScoreRecord>,
    format: ScoreHistoryFormat = ScoreHistoryFormat.DETAILED,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = when (format) {
    ScoreHistoryFormat.DETAILED -> formatDetailedHistory(records, zoneId)
    ScoreHistoryFormat.TABLE -> formatTableHistory(records, zoneId)
}

private fun formatDetailedHistory(records: List<ScoreRecord>, zoneId: ZoneId): String = buildString {
    appendLine("簡單記分板 計分紀錄")
    appendLine("共 ${records.size} 筆")

    records.asReversed().forEachIndexed { index, record ->
        appendLine()
        appendLine("${index + 1}. ${historyTimeFormatter.format(Instant.ofEpochMilli(record.timestampMillis).atZone(zoneId))}")
        appendLine("操作：${record.actionText()}")
        appendLine("${record.homeName}：${record.homeScore}")
        appendLine("${record.awayName}：${record.awayScore}")
        appendLine(record.roundSnapshot.exportText())
    }
}

private fun formatTableHistory(records: List<ScoreRecord>, zoneId: ZoneId): String = buildString {
    appendLine("簡單記分板 計分紀錄")
    appendLine("共 ${records.size} 筆")
    appendLine()
    appendLine("| 主隊分數 | 客隊分數 | 局數 | 時間 |")
    appendLine("|---:|---:|:---:|:---|")
    records.asReversed().forEach { record ->
        val time = historyTimeFormatter.format(Instant.ofEpochMilli(record.timestampMillis).atZone(zoneId))
        appendLine("| ${record.homeScore} | ${record.awayScore} | ${record.roundSnapshot.compactDisplayText()} | $time |")
    }
}

fun ScoreRecord.actionText(): String = when (action) {
    ScoreRecordAction.ADJUSTMENT -> {
        val amount = if (value > 0) "+$value" else value.toString()
        "$actionTeamName $amount"
    }
    ScoreRecordAction.RESET -> "$actionTeamName 歸零（$value → 0）"
}

private fun RoundSnapshot.exportText(): String = when (display) {
    RoundDisplay.HIDDEN -> "局數：未啟用"
    RoundDisplay.CURRENT -> "局數：第 $currentRound 局"
    RoundDisplay.PER_TEAM -> "局數：$homeRounds：$awayRounds"
}

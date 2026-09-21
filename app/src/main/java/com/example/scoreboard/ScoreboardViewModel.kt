package com.example.scoreboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.scoreboard.data.AppTheme
import com.example.scoreboard.data.DarkMode
import com.example.scoreboard.data.RoundDisplay
import com.example.scoreboard.data.ScoreboardRepository
import com.example.scoreboard.data.ScoreRecord
import com.example.scoreboard.data.ScoreRecordAction
import com.example.scoreboard.data.ScoreboardState
import com.example.scoreboard.data.ScreenOrientationMode
import com.example.scoreboard.data.adjustScore
import com.example.scoreboard.data.next
import com.example.scoreboard.data.resetTeamScore
import com.example.scoreboard.data.roundSnapshot
import com.example.scoreboard.data.swapped
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class UiNotice(
    val id: Long,
    val message: String,
    val actionLabel: String? = null,
)

private data class PendingScoreReset(
    val noticeId: Long,
    val recordId: Long,
    val isHome: Boolean,
    val previousScore: Int,
    val teamName: String,
)

class ScoreboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ScoreboardRepository(application)
    private val applicationScope = (application as ScoreboardApplication).applicationScope
    private val pendingSaves = Channel<ScoreboardState>(Channel.CONFLATED)
    private val _state = MutableStateFlow<ScoreboardState?>(null)
    private val _scoreHistory = MutableStateFlow<List<ScoreRecord>>(emptyList())
    private val _isFullscreen = MutableStateFlow(false)
    private val _uiNotice = MutableStateFlow<UiNotice?>(null)
    private var nextRecordId = 1L
    private var nextNoticeId = 1L
    private var pendingScoreReset: PendingScoreReset? = null
    private var customHueSaveJob: Job? = null
    private var customHueDirty = false

    val state: StateFlow<ScoreboardState?> = _state.asStateFlow()
    val scoreHistory: StateFlow<List<ScoreRecord>> = _scoreHistory.asStateFlow()
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()
    val uiNotice: StateFlow<UiNotice?> = _uiNotice.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = repository.state.first()
            applicationScope.launch {
                for (stateToSave in pendingSaves) {
                    saveWithRetry(stateToSave)
                }
            }
        }
    }

    fun adjustScore(isHome: Boolean, delta: Int) {
        val current = _state.value ?: return
        val adjustment = current.adjustScore(isHome, delta)
        if (adjustment.actualDelta != 0) {
            invalidateScoreResetUndo()
            val recordState = adjustment.state
            _scoreHistory.value = listOf(
                ScoreRecord(
                    id = nextRecordId++,
                    actionTeamName = adjustment.teamName,
                    action = ScoreRecordAction.ADJUSTMENT,
                    value = adjustment.actualDelta,
                    homeName = recordState.homeName,
                    homeScore = recordState.homeScore,
                    awayName = recordState.awayName,
                    awayScore = recordState.awayScore,
                    roundSnapshot = recordState.roundSnapshot(),
                    timestampMillis = System.currentTimeMillis(),
                ),
            ) + _scoreHistory.value
            setState(recordState)
        }
    }

    fun resetTeamScore(isHome: Boolean) {
        val current = _state.value ?: return
        val reset = current.resetTeamScore(isHome)
        invalidateScoreResetUndo()
        if (reset.previousScore == 0) return

        val recordId = nextRecordId++
        val recordState = reset.state
        _scoreHistory.value = listOf(
            ScoreRecord(
                id = recordId,
                actionTeamName = reset.teamName,
                action = ScoreRecordAction.RESET,
                value = reset.previousScore,
                homeName = recordState.homeName,
                homeScore = recordState.homeScore,
                awayName = recordState.awayName,
                awayScore = recordState.awayScore,
                roundSnapshot = recordState.roundSnapshot(),
                timestampMillis = System.currentTimeMillis(),
            ),
        ) + _scoreHistory.value
        setState(recordState)

        val notice = showNotice("${reset.teamName}分數已歸零", actionLabel = "復原")
        pendingScoreReset = PendingScoreReset(
            noticeId = notice.id,
            recordId = recordId,
            isHome = isHome,
            previousScore = reset.previousScore,
            teamName = reset.teamName,
        )
    }

    fun addRound(isHome: Boolean) = update { current ->
        if (isHome) current.copy(homeRounds = current.homeRounds + 1)
        else current.copy(awayRounds = current.awayRounds + 1)
    }

    fun subtractRound(isHome: Boolean) = update { current ->
        if (isHome) current.copy(homeRounds = (current.homeRounds - 1).coerceAtLeast(0))
        else current.copy(awayRounds = (current.awayRounds - 1).coerceAtLeast(0))
    }

    fun changeCurrentRound(delta: Int) = update { current ->
        current.copy(currentRound = (current.currentRound + delta).coerceAtLeast(1))
    }

    fun renameTeam(isHome: Boolean, name: String) = update { current ->
        val cleanName = name.trim().ifEmpty { if (isHome) "主隊" else "客隊" }.take(12)
        if (isHome) current.copy(homeName = cleanName) else current.copy(awayName = cleanName)
    }

    fun resetGame() {
        invalidateScoreResetUndo()
        update { current ->
            current.copy(homeScore = 0, awayScore = 0, homeRounds = 0, awayRounds = 0, currentRound = 1)
        }
        _scoreHistory.value = emptyList()
    }

    fun swapTeams() {
        invalidateScoreResetUndo()
        update(transform = ScoreboardState::swapped)
    }

    fun setRoundDisplay(display: RoundDisplay) = update { it.copy(roundDisplay = display) }

    fun setTheme(theme: AppTheme) = update { it.copy(appTheme = theme) }

    fun previewCustomHue(hue: Float) {
        update(persist = false) {
            it.copy(appTheme = AppTheme.CUSTOM, customHue = hue.coerceIn(0f, 360f))
        }
        customHueDirty = true
        customHueSaveJob?.cancel()
        customHueSaveJob = viewModelScope.launch {
            delay(200)
            customHueSaveJob = null
            persistCustomHue()
        }
    }

    fun saveCustomHue() {
        customHueSaveJob?.cancel()
        customHueSaveJob = null
        persistCustomHue()
    }

    private fun persistCustomHue() {
        customHueDirty = false
        _state.value?.let(pendingSaves::trySend)
    }

    fun setDarkMode(mode: DarkMode) = update { it.copy(darkMode = mode) }

    fun setKeepScreenOn(enabled: Boolean) = update { it.copy(keepScreenOn = enabled) }

    fun setScoreSoundEnabled(enabled: Boolean) = update { it.copy(scoreSoundEnabled = enabled) }

    fun cycleScreenOrientation() {
        val nextMode = _state.value?.screenOrientationMode?.next() ?: return
        update { it.copy(screenOrientationMode = nextMode) }
        showNotice(
            when (nextMode) {
                ScreenOrientationMode.SYSTEM -> "螢幕方向：跟隨系統"
                ScreenOrientationMode.PORTRAIT -> "螢幕方向：鎖定直式"
                ScreenOrientationMode.LANDSCAPE -> "螢幕方向：鎖定橫式"
            },
        )
    }

    fun enterFullscreen() {
        _isFullscreen.value = true
        showNotice("已進入全螢幕，點擊工具列圖示退出")
    }

    fun exitFullscreen() {
        _isFullscreen.value = false
        showNotice("已退出全螢幕")
    }

    fun acknowledgeNotice(id: Long) {
        if (_uiNotice.value?.id == id) {
            _uiNotice.value = null
            if (pendingScoreReset?.noticeId == id) pendingScoreReset = null
        }
    }

    fun performNoticeAction(id: Long) {
        val pending = pendingScoreReset?.takeIf { it.noticeId == id } ?: return
        val current = _state.value ?: return
        val restored = if (pending.isHome) {
            current.copy(homeScore = pending.previousScore)
        } else {
            current.copy(awayScore = pending.previousScore)
        }
        _scoreHistory.value = _scoreHistory.value.filterNot { it.id == pending.recordId }
        pendingScoreReset = null
        _uiNotice.value = null
        setState(restored)
        showNotice("已復原${pending.teamName}分數")
    }

    private fun showNotice(message: String, actionLabel: String? = null): UiNotice {
        if (actionLabel == null) invalidateScoreResetUndo()
        val notice = UiNotice(nextNoticeId++, message, actionLabel)
        _uiNotice.value = notice
        return notice
    }

    private fun invalidateScoreResetUndo() {
        val pending = pendingScoreReset ?: return
        pendingScoreReset = null
        if (_uiNotice.value?.id == pending.noticeId) _uiNotice.value = null
    }

    private fun update(
        persist: Boolean = true,
        transform: (ScoreboardState) -> ScoreboardState,
    ) {
        val current = _state.value ?: return
        setState(transform(current), persist)
    }

    private fun setState(state: ScoreboardState, persist: Boolean = true) {
        _state.value = state
        if (persist) pendingSaves.trySend(state)
    }

    private suspend fun saveWithRetry(state: ScoreboardState) {
        repeat(3) { attempt ->
            try {
                repository.save(state)
                return
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (attempt == 2) {
                    Log.e("Scoreboard", "Unable to save scoreboard state", error)
                } else {
                    delay(100L * (attempt + 1))
                }
            }
        }
    }

    override fun onCleared() {
        customHueSaveJob?.cancel()
        if (customHueDirty) _state.value?.let(pendingSaves::trySend)
        pendingSaves.close()
        super.onCleared()
    }
}

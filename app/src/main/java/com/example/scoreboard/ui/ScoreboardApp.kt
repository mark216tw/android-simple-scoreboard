package com.example.scoreboard.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenRotationAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.scoreboard.ScoreboardViewModel
import com.example.scoreboard.audio.ScoreSoundPlayer
import com.example.scoreboard.data.AppTheme
import com.example.scoreboard.data.DarkMode
import com.example.scoreboard.data.RoundDisplay
import com.example.scoreboard.data.ScoreboardState
import com.example.scoreboard.data.ScoreRecord
import com.example.scoreboard.data.ScoreRecordAction
import com.example.scoreboard.data.ScoreHistoryFormat
import com.example.scoreboard.data.actionText
import com.example.scoreboard.data.compactDisplayText
import com.example.scoreboard.data.displayText
import com.example.scoreboard.data.formatScoreHistory
import com.example.scoreboard.ui.theme.ScoreboardColors
import com.example.scoreboard.ui.theme.previewColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScoreboardApp(
    state: ScoreboardState,
    darkTheme: Boolean,
    teamColors: ScoreboardColors,
    viewModel: ScoreboardViewModel,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var historyFormatName by rememberSaveable { mutableStateOf(ScoreHistoryFormat.DETAILED.name) }
    var showResetConfirmation by rememberSaveable { mutableStateOf(false) }
    var editingHomeTeam by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val scoreHistory by viewModel.scoreHistory.collectAsStateWithLifecycle()
    val isFullscreen by viewModel.isFullscreen.collectAsStateWithLifecycle()
    val uiNotice by viewModel.uiNotice.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    val context = LocalContext.current
    val scoreSoundPlayer = remember(state.scoreSoundEnabled) {
        if (state.scoreSoundEnabled) {
            runCatching { ScoreSoundPlayer(context.applicationContext) }.getOrNull()
        } else {
            null
        }
    }

    DisposableEffect(scoreSoundPlayer) {
        onDispose { scoreSoundPlayer?.close() }
    }

    val adjustScore: (Boolean, Int) -> Unit = { isHome, delta ->
        viewModel.adjustScore(isHome, delta)
        scoreSoundPlayer?.play()
    }
    val homeColor = if (state.teamColorsSwapped) teamColors.away else teamColors.home
    val awayColor = if (state.teamColorsSwapped) teamColors.home else teamColors.away
    val onHomeColor = if (state.teamColorsSwapped) teamColors.onAway else teamColors.onHome
    val onAwayColor = if (state.teamColorsSwapped) teamColors.onHome else teamColors.onAway

    ConfigureSystemUi(darkTheme, state.keepScreenOn, isFullscreen)

    LaunchedEffect(uiNotice?.id, lifecycleState) {
        val notice = uiNotice ?: return@LaunchedEffect
        if (!lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = notice.message,
            actionLabel = notice.actionLabel,
            withDismissAction = notice.actionLabel != null,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.performNoticeAction(notice.id)
        } else {
            viewModel.acknowledgeNotice(notice.id)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                ScoreboardToolbar(
                    orientationMode = state.screenOrientationMode,
                    historyCount = scoreHistory.size,
                    isFullscreen = isFullscreen,
                    onSwap = viewModel::swapTeams,
                    onOrientation = viewModel::cycleScreenOrientation,
                    onFullscreen = {
                        if (isFullscreen) viewModel.exitFullscreen() else viewModel.enterFullscreen()
                    },
                    onHistory = { showHistory = true },
                    onReset = { showResetConfirmation = true },
                    onSettings = { showSettings = true },
                )

                if (state.roundDisplay != RoundDisplay.HIDDEN) {
                    Spacer(Modifier.height(6.dp))
                    RoundControl(state, viewModel)
                }

                Spacer(Modifier.height(10.dp))
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val landscape = maxWidth > maxHeight
                    if (landscape) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TeamPanel(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                name = state.homeName,
                                score = state.homeScore,
                                color = homeColor,
                                contentColor = onHomeColor,
                                compact = true,
                                onRename = { editingHomeTeam = true },
                                onReset = { viewModel.resetTeamScore(true) },
                                onAdjust = { delta -> adjustScore(true, delta) },
                            )
                            TeamPanel(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                name = state.awayName,
                                score = state.awayScore,
                                color = awayColor,
                                contentColor = onAwayColor,
                                compact = true,
                                onRename = { editingHomeTeam = false },
                                onReset = { viewModel.resetTeamScore(false) },
                                onAdjust = { delta -> adjustScore(false, delta) },
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TeamPanel(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                name = state.homeName,
                                score = state.homeScore,
                                color = homeColor,
                                contentColor = onHomeColor,
                                compact = false,
                                onRename = { editingHomeTeam = true },
                                onReset = { viewModel.resetTeamScore(true) },
                                onAdjust = { delta -> adjustScore(true, delta) },
                            )
                            TeamPanel(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                name = state.awayName,
                                score = state.awayScore,
                                color = awayColor,
                                contentColor = onAwayColor,
                                compact = false,
                                onRename = { editingHomeTeam = false },
                                onReset = { viewModel.resetTeamScore(false) },
                                onAdjust = { delta -> adjustScore(false, delta) },
                            )
                        }
                    }
                }
            }

        }
    }

    if (showSettings) {
        SettingsSheet(
            state = state,
            onDismiss = { showSettings = false },
            onRoundDisplayChange = viewModel::setRoundDisplay,
            onThemeChange = viewModel::setTheme,
            onCustomHuePreview = viewModel::previewCustomHue,
            onCustomHueSave = viewModel::saveCustomHue,
            onDarkModeChange = viewModel::setDarkMode,
            onKeepScreenOnChange = viewModel::setKeepScreenOn,
            onScoreSoundChange = viewModel::setScoreSoundEnabled,
        )
    }

    if (showHistory) {
        ScoreHistorySheet(
            records = scoreHistory,
            format = ScoreHistoryFormat.valueOf(historyFormatName),
            onFormatChange = { historyFormatName = it.name },
            onDismiss = { showHistory = false },
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("重設本場？") },
            text = { Text("雙方分數、局數與計分紀錄將清除，團隊名稱和外觀設定會保留。") },
            confirmButton = {
                Button(onClick = {
                    viewModel.resetGame()
                    showResetConfirmation = false
                }) { Text("重設") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) { Text("取消") }
            },
        )
    }

    editingHomeTeam?.let { isHome ->
        RenameTeamDialog(
            initialName = if (isHome) state.homeName else state.awayName,
            onDismiss = { editingHomeTeam = null },
            onConfirm = { name ->
                viewModel.renameTeam(isHome, name)
                editingHomeTeam = null
            },
        )
    }
}

@Composable
private fun ConfigureSystemUi(
    darkTheme: Boolean,
    keepScreenOn: Boolean,
    isFullscreen: Boolean,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val background = MaterialTheme.colorScheme.background

    SideEffect {
        val window = (context as Activity).window
        @Suppress("DEPRECATION")
        window.statusBarColor = background.toArgb()
        @Suppress("DEPRECATION")
        window.navigationBarColor = background.toArgb()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isFullscreen) {
                hide(WindowInsetsCompat.Type.systemBars())
            } else {
                show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(view, keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun ScoreboardToolbar(
    orientationMode: com.example.scoreboard.data.ScreenOrientationMode,
    historyCount: Int,
    isFullscreen: Boolean,
    onSwap: () -> Unit,
    onOrientation: () -> Unit,
    onFullscreen: () -> Unit,
    onHistory: () -> Unit,
    onReset: () -> Unit,
    onSettings: () -> Unit,
) {
    var showMore by remember { mutableStateOf(false) }
    val orientationDescription = when (orientationMode) {
        com.example.scoreboard.data.ScreenOrientationMode.SYSTEM -> "切換螢幕方向，目前為跟隨系統"
        com.example.scoreboard.data.ScreenOrientationMode.PORTRAIT -> "切換螢幕方向，目前為鎖定直式"
        com.example.scoreboard.data.ScreenOrientationMode.LANDSCAPE -> "切換螢幕方向，目前為鎖定橫式"
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "簡單記分板",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onSwap) {
            Icon(Icons.Default.SwapHoriz, contentDescription = "交換隊伍")
        }
        IconButton(onClick = onOrientation) {
            Icon(Icons.Default.ScreenRotationAlt, contentDescription = orientationDescription)
        }
        IconButton(onClick = onFullscreen) {
            Icon(
                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (isFullscreen) "退出全螢幕" else "進入全螢幕",
            )
        }
        Box {
            IconButton(onClick = { showMore = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多功能")
            }
            DropdownMenu(
                expanded = showMore,
                onDismissRequest = { showMore = false },
            ) {
                DropdownMenuItem(
                    text = { Text("計分紀錄（$historyCount）") },
                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                    onClick = {
                        showMore = false
                        onHistory()
                    },
                )
                DropdownMenuItem(
                    text = { Text("重設本場") },
                    leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null) },
                    onClick = {
                        showMore = false
                        onReset()
                    },
                )
                DropdownMenuItem(
                    text = { Text("APP 設定") },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    onClick = {
                        showMore = false
                        onSettings()
                    },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.TeamPanel(
    modifier: Modifier,
    name: String,
    score: Int,
    color: Color,
    contentColor: Color,
    compact: Boolean,
    onRename: () -> Unit,
    onReset: () -> Unit,
    onAdjust: (Int) -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        TeamPanelContent(name, score, contentColor, compact, onRename, onReset, onAdjust)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TeamPanel(
    modifier: Modifier,
    name: String,
    score: Int,
    color: Color,
    contentColor: Color,
    compact: Boolean,
    onRename: () -> Unit,
    onReset: () -> Unit,
    onAdjust: (Int) -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        TeamPanelContent(name, score, contentColor, compact, onRename, onReset, onAdjust)
    }
}

@Composable
private fun TeamPanelContent(
    name: String,
    score: Int,
    contentColor: Color,
    compact: Boolean,
    onRename: () -> Unit,
    onReset: () -> Unit,
    onAdjust: (Int) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val shortPanel = maxHeight < 230.dp
        val scoreDp = when {
            shortPanel -> ((maxHeight - 108.dp) * 0.8f).coerceIn(28.dp, 62.dp)
            score >= 1_000 -> 72.dp
            compact -> 90.dp
            else -> 106.dp
        }
        val scoreSize = with(LocalDensity.current) { scoreDp.toSp() }
        Column(
            modifier = Modifier.fillMaxSize().padding(if (shortPanel) 6.dp else if (compact) 10.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextButton(onClick = onRename) {
                Text(
                    text = name,
                    color = contentColor,
                    style = if (shortPanel) MaterialTheme.typography.titleMedium else if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .semantics {
                        contentDescription = "$name，目前 $score 分，點擊加一分"
                    }
                    .clickable(role = Role.Button) { onAdjust(1) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = score.toString(),
                    color = contentColor,
                    fontSize = scoreSize,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    lineHeight = scoreSize,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(if (shortPanel) 48.dp else 52.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val contrastOverlay = if (contentColor.luminance() > 0.5f) Color.Black else Color.White
                ScoreButton(
                    modifier = Modifier.weight(1f),
                    label = "歸零",
                    description = "$name 分數歸零",
                    containerColor = contrastOverlay.copy(alpha = 0.36f),
                    contentColor = contentColor,
                    fontSize = 16,
                    onClick = onReset,
                )
                Spacer(Modifier.width(7.dp))
                ScoreButton(
                    modifier = Modifier.weight(1f),
                    label = "−1",
                    description = "$name 減一分",
                    containerColor = contrastOverlay.copy(alpha = 0.28f),
                    contentColor = contentColor,
                    onClick = { onAdjust(-1) },
                )
                Spacer(Modifier.width(12.dp))
                ScoreButton(
                    modifier = Modifier.weight(1f),
                    label = "+3",
                    description = "$name 加三分",
                    containerColor = contrastOverlay.copy(alpha = 0.16f),
                    contentColor = contentColor,
                    onClick = { onAdjust(3) },
                )
                Spacer(Modifier.width(7.dp))
                ScoreButton(
                    modifier = Modifier.weight(1f),
                    label = "+2",
                    description = "$name 加兩分",
                    containerColor = contrastOverlay.copy(alpha = 0.16f),
                    contentColor = contentColor,
                    onClick = { onAdjust(2) },
                )
            }
        }
    }
}

@Composable
private fun ScoreButton(
    modifier: Modifier,
    label: String,
    description: String,
    containerColor: Color,
    contentColor: Color,
    fontSize: Int = 20,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                modifier = Modifier.clearAndSetSemantics { },
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun RoundControl(state: ScoreboardState, viewModel: ScoreboardViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
    ) {
        when (state.roundDisplay) {
            RoundDisplay.HIDDEN -> Unit
            RoundDisplay.CURRENT -> {
                Row(
                    modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    RoundButton("−", "局數減一") { viewModel.changeCurrentRound(-1) }
                    Text(
                        text = "第 ${state.currentRound} 局",
                        modifier = Modifier.width(112.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    RoundButton("+", "局數加一") { viewModel.changeCurrentRound(1) }
                }
            }
            RoundDisplay.PER_TEAM -> {
                Row(
                    modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TeamRoundControl(
                        modifier = Modifier.weight(1f),
                        rounds = state.homeRounds,
                        teamName = state.homeName,
                        onMinus = { viewModel.subtractRound(true) },
                        onPlus = { viewModel.addRound(true) },
                    )
                    Text("局數", modifier = Modifier.padding(horizontal = 4.dp), fontWeight = FontWeight.Bold)
                    TeamRoundControl(
                        modifier = Modifier.weight(1f),
                        rounds = state.awayRounds,
                        teamName = state.awayName,
                        onMinus = { viewModel.subtractRound(false) },
                        onPlus = { viewModel.addRound(false) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamRoundControl(
    modifier: Modifier,
    rounds: Int,
    teamName: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        RoundButton("−", "$teamName 局數減一", onMinus)
        Text(rounds.toString(), fontSize = 22.sp, fontWeight = FontWeight.Black)
        RoundButton("+", "$teamName 局數加一", onPlus)
    }
}

@Composable
private fun RoundButton(label: String, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        contentColor = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                modifier = Modifier.clearAndSetSemantics { },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RenameTeamDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("編輯團隊名稱") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(12) },
                label = { Text("團隊名稱") },
                singleLine = true,
                supportingText = { Text("${name.length}/12") },
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }) { Text("完成") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoreHistorySheet(
    records: List<ScoreRecord>,
    format: ScoreHistoryFormat,
    onFormatChange: (ScoreHistoryFormat) -> Unit,
    onDismiss: () -> Unit,
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val context = LocalContext.current
    val exportText = remember(records, format) { formatScoreHistory(records, format) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "計分紀錄",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "本次開啟共 ${records.size} 筆，重設本場時會一併清除",
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = format == ScoreHistoryFormat.DETAILED,
                    onClick = { onFormatChange(ScoreHistoryFormat.DETAILED) },
                    label = { Text("詳細格式") },
                )
                FilterChip(
                    selected = format == ScoreHistoryFormat.TABLE,
                    onClick = { onFormatChange(ScoreHistoryFormat.TABLE) },
                    label = { Text("表格格式") },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = records.isNotEmpty(),
                    onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard.setPrimaryClip(ClipData.newPlainText("簡單記分板 計分紀錄", exportText))
                        Toast.makeText(context, "已複製全部計分紀錄", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("複製全部")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = records.isNotEmpty(),
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "簡單記分板 計分紀錄")
                            putExtra(Intent.EXTRA_TEXT, exportText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "分享計分紀錄"))
                    },
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("分享全部")
                }
            }
            if (records.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(
                        text = "尚無計分紀錄",
                        modifier = Modifier.padding(vertical = 30.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                if (format == ScoreHistoryFormat.DETAILED) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 480.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                    ) {
                        items(records, key = ScoreRecord::id) { record ->
                            ScoreHistoryRow(record, timeFormatter.format(Date(record.timestampMillis)))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                        }
                    }
                } else {
                    ScoreHistoryTableHeader()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 480.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                    ) {
                        items(records, key = ScoreRecord::id) { record ->
                            ScoreHistoryTableRow(record, timeFormatter.format(Date(record.timestampMillis)))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onDismiss) {
                Text("關閉")
            }
        }
    }
}

@Composable
private fun ScoreHistoryTableHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
    ) {
        Row(modifier = Modifier.padding(vertical = 10.dp)) {
            ScoreHistoryTableCell("主隊分數", 1f, FontWeight.Bold)
            ScoreHistoryTableCell("客隊分數", 1f, FontWeight.Bold)
            ScoreHistoryTableCell("局數", 1f, FontWeight.Bold)
            ScoreHistoryTableCell("時間", 1f, FontWeight.Bold)
        }
    }
}

@Composable
private fun ScoreHistoryTableRow(record: ScoreRecord, formattedTime: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScoreHistoryTableCell(record.homeScore.toString(), 1f, FontWeight.Bold)
        ScoreHistoryTableCell(record.awayScore.toString(), 1f, FontWeight.Bold)
        ScoreHistoryTableCell(record.roundSnapshot.compactDisplayText(), 1f)
        ScoreHistoryTableCell(formattedTime, 1f)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ScoreHistoryTableCell(
    text: String,
    weight: Float,
    fontWeight: FontWeight? = null,
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight).padding(horizontal = 3.dp),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = fontWeight,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ScoreHistoryRow(record: ScoreRecord, formattedTime: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    ) {
        Text(
            text = record.actionText(),
            color = if (record.action == ScoreRecordAction.ADJUSTMENT && record.value < 0) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${record.homeName} ${record.homeScore}：${record.awayScore} ${record.awayName}",
            modifier = Modifier.padding(top = 3.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${record.roundSnapshot.displayText()} · $formattedTime",
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    state: ScoreboardState,
    onDismiss: () -> Unit,
    onRoundDisplayChange: (RoundDisplay) -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onCustomHuePreview: (Float) -> Unit,
    onCustomHueSave: () -> Unit,
    onDarkModeChange: (DarkMode) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onScoreSoundChange: (Boolean) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
        ) {
            Text("APP 設定", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            SettingsHeading("局數顯示")
            ChoiceRow {
                RoundChoice("隱藏", RoundDisplay.HIDDEN, state.roundDisplay, onRoundDisplayChange)
                RoundChoice("第幾局", RoundDisplay.CURRENT, state.roundDisplay, onRoundDisplayChange)
                RoundChoice("雙方局數", RoundDisplay.PER_TEAM, state.roundDisplay, onRoundDisplayChange)
            }

            SettingsHeading("主題色彩")
            ThemeGrid(state.appTheme, state.customHue, onThemeChange)
            CustomThemePicker(
                hue = state.customHue,
                selected = state.appTheme == AppTheme.CUSTOM,
                colorsSwapped = state.teamColorsSwapped,
                onHueChange = onCustomHuePreview,
                onHueChangeFinished = onCustomHueSave,
                onSelect = { onThemeChange(AppTheme.CUSTOM) },
            )

            SettingsHeading("顯示模式")
            ChoiceRow {
                DarkModeChoice("跟隨系統", DarkMode.SYSTEM, state.darkMode, onDarkModeChange)
                DarkModeChoice("淺色", DarkMode.LIGHT, state.darkMode, onDarkModeChange)
                DarkModeChoice("深色", DarkMode.DARK, state.darkMode, onDarkModeChange)
            }

            Spacer(Modifier.height(18.dp))
            SettingsToggle(
                title = "計分音效",
                description = "計分按鈕播放清楚的短促提示音",
                checked = state.scoreSoundEnabled,
                onCheckedChange = onScoreSoundChange,
            )
            Spacer(Modifier.height(10.dp))
            SettingsToggle(
                title = "保持螢幕常亮",
                description = "記分期間不自動關閉螢幕",
                checked = state.keepScreenOn,
                onCheckedChange = onKeepScreenOnChange,
            )

            Spacer(Modifier.height(22.dp))
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onDismiss) {
                Text("完成")
            }
        }
    }
}

@Composable
private fun SettingsHeading(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun CustomThemePicker(
    hue: Float,
    selected: Boolean,
    colorsSwapped: Boolean,
    onHueChange: (Float) -> Unit,
    onHueChangeFinished: () -> Unit,
    onSelect: () -> Unit,
) {
    val selectedColor = Color.hsv(hue, 0.76f, 0.78f)
    val complementaryColor = Color.hsv((hue + 180f) % 360f, 0.76f, 0.78f)
    val homeColor = if (colorsSwapped) complementaryColor else selectedColor
    val awayColor = if (colorsSwapped) selectedColor else complementaryColor
    val rainbow = remember {
        Brush.horizontalGradient(
            listOf(
                Color.Red,
                Color.Yellow,
                Color.Green,
                Color.Cyan,
                Color.Blue,
                Color.Magenta,
                Color.Red,
            ),
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(18.dp),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "自訂色彩",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                )
                Box(Modifier.size(22.dp).clip(CircleShape).background(homeColor))
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(22.dp).clip(CircleShape).background(awayColor))
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = selected,
                    onClick = onSelect,
                    label = { Text(if (selected) "使用中" else "套用") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(rainbow),
                )
                Slider(
                    modifier = Modifier.semantics {
                        contentDescription = "自訂主題色相"
                        stateDescription = if (colorsSwapped) {
                            "色相 ${hue.toInt()} 度套用於客隊，主隊使用互補色"
                        } else {
                            "色相 ${hue.toInt()} 度套用於主隊，客隊使用互補色"
                        }
                    },
                    value = hue,
                    onValueChange = onHueChange,
                    onValueChangeFinished = onHueChangeFinished,
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = selectedColor,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                    ),
                )
            }
            Text(
                text = if (colorsSwapped) {
                    "拖曳時立即預覽，目前客隊使用所選色，主隊使用互補色"
                } else {
                    "拖曳時立即預覽，目前主隊使用所選色，客隊使用互補色"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            )
        }
    }
}

@Composable
private fun ChoiceRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.RoundChoice(
    label: String,
    value: RoundDisplay,
    selected: RoundDisplay,
    onChange: (RoundDisplay) -> Unit,
) {
    val isSelected = value == selected
    FilterChip(
        modifier = Modifier.weight(1f),
        selected = isSelected,
        onClick = { onChange(value) },
        label = {
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                maxLines = 1,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.DarkModeChoice(
    label: String,
    value: DarkMode,
    selected: DarkMode,
    onChange: (DarkMode) -> Unit,
) {
    val isSelected = value == selected
    FilterChip(
        modifier = Modifier.weight(1f),
        selected = isSelected,
        onClick = { onChange(value) },
        label = {
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                maxLines = 1,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
private fun ThemeGrid(
    selected: AppTheme,
    customHue: Float,
    onThemeChange: (AppTheme) -> Unit,
) {
    val themes = listOf(
        AppTheme.ENERGY to "活力橘",
        AppTheme.OCEAN to "海洋藍",
        AppTheme.MINT to "薄荷綠",
        AppTheme.GRAPE to "葡萄紫",
        AppTheme.LEMON to "檸檬黃",
        AppTheme.MATCH to "紅藍對決",
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        themes.chunked(3).forEach { rowThemes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowThemes.forEach { (theme, label) ->
                    ThemeChoice(
                        modifier = Modifier.weight(1f),
                        theme = theme,
                        label = label,
                        selected = theme == selected,
                        customHue = customHue,
                        onClick = { onThemeChange(theme) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeChoice(
    modifier: Modifier,
    theme: AppTheme,
    label: String,
    selected: Boolean,
    customHue: Float,
    onClick: () -> Unit,
) {
    val color = previewColor(theme, customHue)
    Surface(
        modifier = modifier
            .height(74.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        color = if (selected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, color) else null,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(color))
            Spacer(Modifier.height(5.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

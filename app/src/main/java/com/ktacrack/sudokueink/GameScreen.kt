package com.ktacrack.sudokueink

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import java.util.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.delay


enum class NotesMode {
    OFF, MANUAL, AUTO
}

enum class PencilMode {
    OFF, ON, AUTO
}

@Composable
fun GameScreen(
    difficulty: Difficulty,
    mode: GameMode,
    isZenMode: Boolean = false,
    isDaily: Boolean = false,
    dailySudoku: SudokuGame? = null,
    onDailyComplete: ((Int) -> Unit)? = null,
    onBack: () -> Unit
){
    val context = LocalContext.current
    val strings = rememberStrings()

    var resetTrigger by remember { mutableIntStateOf(0) }  // mutableIntStateOf
    var resetTimerTrigger by remember(resetTrigger) { mutableIntStateOf(0) }

    val dailyGame = DailySudokuManager.generateDailySudoku() // Retorna daily sudoku

    // Intentar carregar partida guardada
    val savedGame = remember(resetTrigger, isZenMode) {
        if (resetTrigger == 0)
            GameStateManager.loadGame(context, mode, difficulty, isDaily, isZenMode)
        else null
    } as SavedGameState?

    val initialGame = remember(resetTrigger, isDaily) {
        if (savedGame != null) {
            val restoredBoard = savedGame.board.map { row ->
                row.map { savedCell ->
                    SudokuCell(
                        value = savedCell.value,
                        isFixed = savedCell.isFixed,
                        notes = savedCell.notes.toSet()
                    )
                }
            }
            SudokuGame(restoredBoard, savedGame.solution)
        } else {
            if (isDaily && dailySudoku != null) {
                // ✅ dailySudoku ja és un SudokuGame complet
                dailySudoku
            } else {
                // Generar sudoku normal
                SudokuGenerator.generate(difficulty)
            }
        }
    }

    var isPaused by remember(resetTrigger) { mutableStateOf(savedGame != null) }
    var showPauseDialog by remember { mutableStateOf(false) }
    var shouldSaveOnExit by remember(resetTrigger) { mutableStateOf(true) }

    val scale = AdaptiveSizes.getScaleFactor()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val initialBoard = remember(resetTrigger) {
        if (savedGame != null) {
            savedGame.board.map { row ->
                row.map { savedCell ->
                    SudokuCell(
                        value = if (savedCell.isFixed) savedCell.value else 0,
                        isFixed = savedCell.isFixed,
                        notes = savedCell.notes.toSet()
                    )
                }
            }
        } else {
            initialGame.board.map { row -> row.map { it.copy() } }
        }
    }

    val solution = remember(resetTrigger) { initialGame.solution }

    var boardState by remember(resetTrigger) {
        mutableStateOf(
            initialGame.board.map { row ->
                row.map { cell -> cell.copy() }
            }
        )
    }

    var hadErrorsDuringGame by remember { mutableStateOf(false) }
    var selectedCell by remember(resetTrigger) { mutableStateOf<Pair<Int, Int>?>(null) }
    var selectedNumber by remember(resetTrigger) { mutableStateOf<Int?>(null) }
    var moveCount by remember(resetTrigger) { mutableIntStateOf(savedGame?.moveCount ?: 0) }
    var showVictoryDialog by remember(resetTrigger) { mutableStateOf(false) }
    var showErrorDialog by remember(resetTrigger) { mutableStateOf(false) }
    var showTimeoutDialog by remember(resetTrigger) { mutableStateOf(false) }
    var showResumeDialog by remember(resetTrigger) { mutableStateOf(savedGame != null) }
    var showDrawingCanvas by remember { mutableStateOf(false) }
    var pencilMode by remember(resetTrigger) {
        mutableStateOf(PencilModeManager.loadPencilMode(context))
    }

    // Recorda l'última selecció de mode Llapis entre partides i sessions
    LaunchedEffect(pencilMode) {
        PencilModeManager.savePencilMode(context, pencilMode)
    }

    // Reconeixedor compartit per al mode Llapis AUTO (dibuix directe sobre la cel·la)
    val digitRecognizer = remember { DigitRecognizer(context) }
    DisposableEffect(Unit) {
        onDispose { digitRecognizer.close() }
    }

    // ✅ ESBORRADES: showAchievementUnlocked i unlockedAchievement (no s'usaven)
    var pendingVictoryDialog by remember { mutableStateOf(false) }
    var pendingClearGame by remember { mutableStateOf(false) }
    var unlockedQueue by remember { mutableStateOf<List<Achievement>>(emptyList()) }
    var showingAchievement by remember { mutableStateOf<Achievement?>(null) }

    var startTimeOffset by remember(resetTrigger) { mutableIntStateOf(savedGame?.elapsedSeconds ?: 0) }
    var currentElapsedSeconds by remember(resetTrigger) { mutableIntStateOf(startTimeOffset) }
    var pausedAtSeconds by remember(resetTrigger) { mutableIntStateOf(startTimeOffset) }

    val timeLimitSeconds = remember(mode, difficulty) {
        if (mode == GameMode.ATTACK) {
            when (difficulty) {
                Difficulty.EASY -> 20 * 60
                Difficulty.MEDIUM -> 30 * 60
                Difficulty.HARD -> 45 * 60
            }
        } else {
            null
        }
    }

    val timerText = rememberTimer(
        resetTrigger = resetTrigger,
        startOffset = startTimeOffset,
        isPaused = isPaused || isZenMode,  // ✅ Pausar timer si Mode Zen
        resetTimer = resetTimerTrigger,
        onTimeUpdate = { elapsedSeconds ->
            currentElapsedSeconds = elapsedSeconds
        }
    )

    val displayTimerText = if (timeLimitSeconds != null) {
        val remaining = (timeLimitSeconds - currentElapsedSeconds).coerceAtLeast(0)
        val minutes = remaining / 60
        val seconds = remaining % 60
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    } else {
        timerText
    }

    var hintsRemaining by rememberSaveable {
        mutableIntStateOf(
            savedGame?.hintsRemaining ?: when (difficulty) {
                Difficulty.EASY -> 5
                Difficulty.MEDIUM -> 3
                Difficulty.HARD -> 1
            }
        )
    }

    LaunchedEffect(resetTrigger) {
        if (resetTrigger > 0) {
            hintsRemaining = when (difficulty) {
                Difficulty.EASY -> 5
                Difficulty.MEDIUM -> 3
                Difficulty.HARD -> 1
            }
        }
    }

    var notesMode by remember(resetTrigger) { mutableStateOf(NotesMode.OFF) }
    var showAutoNotesDialog by remember { mutableStateOf(false) }
    val moveHistory = rememberSaveable { mutableStateListOf<List<List<SudokuCell>>>() }

    // Guardar automàticament
    LaunchedEffect(boardState, hintsRemaining) {
        if (!showVictoryDialog) {
            val savedState = SavedGameState(
                difficulty = difficulty.name,
                mode = mode.name,
                board = boardState.map { row ->
                    row.map { cell ->
                        SavedCell(
                            value = cell.value,
                            isFixed = cell.isFixed,
                            notes = cell.notes.toList()
                        )
                    }
                },
                solution = solution,
                elapsedSeconds = currentElapsedSeconds,
                hintsRemaining = hintsRemaining,
                moveCount = moveCount
            )
            GameStateManager.saveGame(context, savedState, isDaily, isZenMode)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!showVictoryDialog && shouldSaveOnExit) {
                val savedState = SavedGameState(
                    difficulty = difficulty.name,
                    mode = mode.name,
                    board = boardState.map { row ->
                        row.map { cell ->
                            SavedCell(
                                value = cell.value,
                                isFixed = cell.isFixed,
                                notes = cell.notes.toList()
                            )
                        }
                    },
                    solution = solution,
                    elapsedSeconds = currentElapsedSeconds,
                    hintsRemaining = hintsRemaining,
                    moveCount = moveCount
                )
                GameStateManager.saveGame(context, savedState, isDaily, isZenMode)
            }
        }
    }

    LaunchedEffect(pendingClearGame) {
        if (pendingClearGame) {
            GameStateManager.clearGame(context, mode, difficulty, isDaily, isZenMode)
            pendingClearGame = false
        }
    }

    fun getValidNotesForCell(row: Int, col: Int): Set<Int> {
        val usedNumbers = mutableSetOf<Int>()

        for (c in 0 until 9) {
            if (boardState[row][c].value != 0) {
                usedNumbers.add(boardState[row][c].value)
            }
        }

        for (r in 0 until 9) {
            if (boardState[r][col].value != 0) {
                usedNumbers.add(boardState[r][col].value)
            }
        }

        val blockRow = (row / 3) * 3
        val blockCol = (col / 3) * 3
        for (r in blockRow until blockRow + 3) {
            for (c in blockCol until blockCol + 3) {
                if (boardState[r][c].value != 0) {
                    usedNumbers.add(boardState[r][c].value)
                }
            }
        }

        return (1..9).toSet() - usedNumbers
    }

    fun cleanAutoNotes(newBoard: List<List<SudokuCell>>): List<List<SudokuCell>> {
        if (notesMode != NotesMode.AUTO) return newBoard

        return newBoard.mapIndexed { row, rowList ->
            rowList.mapIndexed { col, cell ->
                if (cell.isFixed || cell.value != 0) {
                    cell
                } else if (cell.notes.isEmpty()) {
                    cell
                } else {
                    val usedNumbers = mutableSetOf<Int>()

                    for (c in 0 until 9) {
                        if (newBoard[row][c].value != 0) {
                            usedNumbers.add(newBoard[row][c].value)
                        }
                    }

                    for (r in 0 until 9) {
                        if (newBoard[r][col].value != 0) {
                            usedNumbers.add(newBoard[r][col].value)
                        }
                    }

                    val blockRow = (row / 3) * 3
                    val blockCol = (col / 3) * 3
                    for (r in blockRow until blockRow + 3) {
                        for (c in blockCol until blockCol + 3) {
                            if (newBoard[r][c].value != 0) {
                                usedNumbers.add(newBoard[r][c].value)
                            }
                        }
                    }

                    val validNotes = (1..9).toSet() - usedNumbers
                    cell.copy(notes = cell.notes.intersect(validNotes))
                }
            }
        }
    }

    if (showAutoNotesDialog) {
        AlertDialog(
            onDismissRequest = { showAutoNotesDialog = false },
            title = {
                Text(
                    text = strings.autoNotesTitle,
                    fontSize = (30 * scale).sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = strings.autoNotesExplanation,
                    fontSize = (26 * scale).sp,
                    lineHeight = (30 * scale).sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showAutoNotesDialog = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Text(strings.understood, fontSize = (24 * scale).sp)
                }
            }
        )
    }

    LaunchedEffect(pendingVictoryDialog) {
        if (pendingVictoryDialog) {
            val updatedStats = StatisticsManager.loadStatistics(context)
            val newlyUnlocked = mutableListOf<Achievement>()
            // ✅ ESBORRAT: var unlockedSomething = false (mai s'usava)

            AchievementManager.checkAchievements(
                context = context,
                stats = updatedStats,
                strings = strings,
                onNewAchievement = { newAchievement ->
                    newlyUnlocked.add(newAchievement)
                }
            )

            if (newlyUnlocked.isNotEmpty()) {
                unlockedQueue = newlyUnlocked
                showingAchievement = unlockedQueue.first()
            } else {
                showVictoryDialog = true
                pendingClearGame = true
                pendingVictoryDialog = false
            }
        }
    }

    if (showingAchievement != null) {
        AlertDialog(
            onDismissRequest = { /* forcem a clicar botó */ },
            icon = {
                Icon(
                    Icons.Default.EmojiEvents,
                    null,
                    modifier = Modifier.size((64 * scale).dp),
                    tint = Color(0xFFFFD700)
                )
            },
            title = { Text("🏆 ${strings.achievementUnlocked}") },
            text = {
                Column {
                    Text(
                        showingAchievement!!.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = (20 * scale).sp
                    )
                    Text(
                        showingAchievement!!.description,
                        fontSize = (16 * scale).sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    unlockedQueue = unlockedQueue.drop(1)

                    if (unlockedQueue.isNotEmpty()) {
                        showingAchievement = unlockedQueue.first()
                    } else {
                        showingAchievement = null
                        showVictoryDialog = true
                        pendingClearGame = true
                        pendingVictoryDialog = false
                    }
                }) {
                    Text(strings.awesome)
                }
            }
        )
    }

    fun updateBoard(newBoard: List<List<SudokuCell>>) {
        moveHistory.add(boardState)
        boardState = cleanAutoNotes(newBoard)

        // ✅ NOU: Incrementar moviments en Mode Zen
        if (isZenMode) {
            moveCount++
        }
    }

    // Aplica un dígit reconegut a una cel·la.
    // asNote (punt al racó): força nota i alterna el dígit al conjunt.
    // Sense punt: respecta el mode de notes actual.
    fun applyRecognizedDigit(row: Int, col: Int, digit: Int, asNote: Boolean = false) {
        if (boardState[row][col].isFixed || digit !in 1..9) return
        updateBoard(
            boardState.mapIndexed { r, rowList ->
                rowList.mapIndexed { c, cell ->
                    if (r == row && c == col) {
                        val toNote = asNote || notesMode == NotesMode.MANUAL
                        if (toNote) {
                            val newNotes = if (cell.notes.contains(digit)) {
                                cell.notes - digit
                            } else {
                                cell.notes + digit
                            }
                            cell.copy(notes = newNotes)
                        } else {
                            cell.copy(value = digit, notes = emptySet())
                        }
                    } else {
                        cell
                    }
                }
            }
        )
    }

    LaunchedEffect(timeLimitSeconds, currentElapsedSeconds, showVictoryDialog) {
        if (timeLimitSeconds != null &&
            !showVictoryDialog &&
            currentElapsedSeconds >= timeLimitSeconds
        ) {
            isPaused = true
            showTimeoutDialog = true
            shouldSaveOnExit = false
        }
    }

    LaunchedEffect(boardState) {
        val complete = boardState.all { row -> row.all { it.value != 0 } }
        if (complete && !showVictoryDialog) {
            val correct = boardState.mapIndexed { r, row ->
                row.mapIndexed { c, cell -> cell.value == solution[r][c] }
            }.all { it.all { it } }

            if (correct) {
                isPaused = true

                val hintsUsed = when (difficulty) {
                    Difficulty.EASY -> 5 - hintsRemaining
                    Difficulty.MEDIUM -> 3 - hintsRemaining
                    Difficulty.HARD -> 1 - hintsRemaining
                }

                if (!isDaily) {
                    StatisticsManager.recordCompletion(
                        context, difficulty, mode, currentElapsedSeconds,
                        hintsUsed = hintsUsed,
                        errorsCount = if (hadErrorsDuringGame) 1 else 0,
                        isDaily = false,
                        isZenMode = isZenMode
                    )
                } else {
                    DailySudokuManager.markDailyAsPlayed(context, currentElapsedSeconds)
                    DailySudokuManager.recordDailyCompletion(context)
                    DailySudokuManager.markTodayAsZenMode(context, isZenMode)
                    StatisticsManager.recordCompletion(
                        context, difficulty, mode, currentElapsedSeconds,
                        hintsUsed = hintsUsed,
                        errorsCount = if (hadErrorsDuringGame) 1 else 0,
                        isDaily = true,
                        isZenMode = isZenMode
                    )
                }
                pendingVictoryDialog = true

            } else {
                hadErrorsDuringGame = true
                showErrorDialog = true
            }
        }
    }

    if (isLandscape) {
        // LAYOUT HORITZONTAL
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding((16 * scale).dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height((46 * scale).dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = (10 * scale).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Esquerra: Tornar
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .width((160 * scale).dp)
                            .height((50 * scale).dp)
                    ) {
                        Text(strings.back, fontSize = (20 * scale).sp)
                    }

                    // Centre: Títol Daily (o buit)
                    if (isDaily) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = strings.dailySudokuTitle,
                                fontSize = (28 * scale).sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                            Text(
                                text = DailySudokuManager.getTodayFormatted(),
                                fontSize = (18 * scale).sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width((1).dp))  // Buit si no és daily
                    }

                    // Dreta: Spacer equilibrador (nou joc està a la columna dreta)
                    Spacer(modifier = Modifier.width((160 * scale).dp))
                }

                Spacer(modifier = Modifier.height((8 * scale).dp))

                // Títol dificultat + Timer (sense el bloc isDaily que estava aquí)
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (difficulty) {
                            Difficulty.EASY -> strings.difficultyEasy
                            Difficulty.MEDIUM -> strings.difficultyMedium
                            Difficulty.HARD -> strings.difficultyHard
                        },
                        modifier = Modifier.padding(bottom = 0.dp),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = (24 * scale).sp)
                    )
                    // Timer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 0.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (isPaused) {
                                    startTimeOffset = pausedAtSeconds
                                    isPaused = false
                                } else {
                                    isPaused = true
                                    pausedAtSeconds = currentElapsedSeconds
                                    showPauseDialog = true
                                }
                            },
                            modifier = Modifier.size((36 * scale).dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Reprendre" else "Pausar",
                                modifier = Modifier.size((24 * scale).dp)
                            )
                        }

                        //  MODIFICAT: condicional per Mode Zen
                        if (isZenMode) {
                            Text(
                                text = "${strings.moves}: $moveCount",
                                fontSize = (24 * scale).sp,
                                color = Color(0xFF4CAF50)
                            )
                        } else {
                            Text(text = displayTimerText, fontSize = (24 * scale).sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(0.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SudokuBoard(
                        board = boardState,
                        selectedCell = selectedCell,
                        pencilAutoMode = pencilMode == PencilMode.AUTO && !isPaused,
                        recognizer = digitRecognizer,
                        onDigitDrawn = { row, col, digit, asNote ->
                            if (!isPaused) applyRecognizedDigit(row, col, digit, asNote)
                        },
                        onCellClick = { row, col ->
                            if (!isPaused && !boardState[row][col].isFixed) {
                                if (pencilMode == PencilMode.ON) {
                                    selectedCell = Pair(row, col)
                                    showDrawingCanvas = true
                                } else {
                                    selectedCell = Pair(row, col)
                                }
                            }
                        },
                        onCellLongClick = { row, col ->
                            if (!isPaused && notesMode == NotesMode.AUTO && !boardState[row][col].isFixed) {
                                val validNotes = getValidNotesForCell(row, col)
                                updateBoard(
                                    boardState.mapIndexed { r, rowList ->
                                        rowList.mapIndexed { c, cell ->
                                            if (r == row && c == col) {
                                                cell.copy(notes = validNotes, value = 0)
                                            } else {
                                                cell
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width((16 * scale).dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height((36 * scale).dp))
                if(!isDaily) {
                    Button(
                        onClick = {
                            shouldSaveOnExit = false
                            GameStateManager.clearGame(context, mode, difficulty, isDaily, isZenMode)
                            resetTrigger++
                            hadErrorsDuringGame = false
                        },
                        modifier = Modifier.fillMaxWidth().height((50 * scale).dp)
                    ) {
                        Text(strings.newGame, fontSize = (20 * scale).sp)
                    }
                }

                Spacer(modifier = Modifier.height((6 * scale).dp))

                Button(
                    onClick = {
                        if (!isPaused) {
                            moveHistory.clear()
                            boardState = initialBoard.map { row -> row.map { cell -> cell.copy() } }
                            selectedCell = null
                            selectedNumber = null
                            moveCount = 0  // Resetear també moviments
                            hadErrorsDuringGame = false
                            hintsRemaining = when (difficulty) {
                                Difficulty.EASY -> 5
                                Difficulty.MEDIUM -> 3
                                Difficulty.HARD -> 1
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height((50 * scale).dp)
                ) {
                    Text(strings.reset, fontSize = (20 * scale).sp)
                }

                Spacer(modifier = Modifier.height((20 * scale).dp))

                // Números grid 3x3
                Column {
                    for (rowNum in 0 until 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (colNum in 0 until 3) {
                                val number = rowNum * 3 + colNum + 1
                                Button(
                                    onClick = {
                                        if (!isPaused) {
                                            selectedCell?.let { (row, col) ->
                                                if (!boardState[row][col].isFixed) {
                                                    selectedNumber = number
                                                    updateBoard(
                                                        boardState.mapIndexed { r, rowList ->
                                                            rowList.mapIndexed { c, cell ->
                                                                if (r == row && c == col) {
                                                                    when (notesMode) {
                                                                        NotesMode.MANUAL -> {
                                                                            val newNotes = if (cell.notes.contains(number)) {
                                                                                cell.notes - number
                                                                            } else {
                                                                                cell.notes + number
                                                                            }
                                                                            cell.copy(notes = newNotes)
                                                                        }
                                                                        NotesMode.AUTO -> {
                                                                            cell.copy(value = number, notes = emptySet())
                                                                        }
                                                                        NotesMode.OFF -> {
                                                                            cell.copy(value = number, notes = emptySet())
                                                                        }
                                                                    }
                                                                } else {
                                                                    cell
                                                                }
                                                            }
                                                        }
                                                    )
                                                    if (notesMode != NotesMode.MANUAL) {
                                                        selectedCell = null
                                                    }
                                                    selectedNumber = null
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size((50 * scale).dp)
                                        .padding((2 * scale).dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedNumber == number) Color.Black else Color.White,
                                        contentColor = if (selectedNumber == number) Color.White else Color.Black
                                    ),
                                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(text = number.toString(), fontSize = (32 * scale).sp)
                                }
                            }
                        }
                        if (rowNum < 2) {
                            Spacer(modifier = Modifier.height((8 * scale).dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height((24 * scale).dp))

                Button(
                    onClick = {
                        if (!isPaused) {
                            pencilMode = when (pencilMode) {
                                PencilMode.OFF -> PencilMode.ON
                                PencilMode.ON -> PencilMode.AUTO
                                PencilMode.AUTO -> PencilMode.OFF
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((50 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pencilMode != PencilMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (pencilMode != PencilMode.OFF) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(
                        text = when (pencilMode) {
                            PencilMode.OFF -> strings.pencilOff
                            PencilMode.ON -> strings.pencilOn
                            PencilMode.AUTO -> strings.pencilAuto
                        },
                        fontSize = (20 * scale).sp
                    )
                }

                Spacer(modifier = Modifier.height((8 * scale).dp))

                Button(
                    onClick = {
                        if (!isPaused) {
                            notesMode = when (notesMode) {
                                NotesMode.OFF -> NotesMode.MANUAL
                                NotesMode.MANUAL -> {
                                    showAutoNotesDialog = true
                                    NotesMode.AUTO
                                }
                                NotesMode.AUTO -> NotesMode.OFF
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((50 * scale).dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (notesMode) {
                            NotesMode.OFF -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.primary
                        },
                        contentColor = when (notesMode) {
                            NotesMode.OFF -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onPrimary
                        }
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(
                        text = when (notesMode) {
                            NotesMode.OFF -> strings.notesOff
                            NotesMode.MANUAL -> strings.notesManual
                            NotesMode.AUTO -> strings.notesAuto
                        },
                        fontSize = (20 * scale).sp
                    )
                }

                Spacer(modifier = Modifier.height((8 * scale).dp))

                Button(
                    onClick = {
                        if (!isPaused) {
                            selectedCell?.let { (row, col) ->
                                if (!boardState[row][col].isFixed) {
                                    updateBoard(
                                        boardState.mapIndexed { r, rowList ->
                                            rowList.mapIndexed { c, cell ->
                                                if (r == row && c == col) {
                                                    cell.copy(value = 0, notes = emptySet())
                                                } else {
                                                    cell
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height((50 * scale).dp)
                ) {
                    Text(strings.erase, fontSize = (20 * scale).sp)
                }

                Spacer(modifier = Modifier.height((8 * scale).dp))

                Button(
                    onClick = {
                        if (!isPaused && hintsRemaining > 0) {
                            val emptyCells = mutableListOf<Pair<Int, Int>>()
                            for (r in 0 until 9) {
                                for (c in 0 until 9) {
                                    if (boardState[r][c].value == 0 && !boardState[r][c].isFixed) {
                                        emptyCells.add(Pair(r, c))
                                    }
                                }
                            }
                            if (emptyCells.isNotEmpty()) {
                                val (r, c) = emptyCells.random()
                                updateBoard(
                                    boardState.mapIndexed { row, rowList ->
                                        rowList.mapIndexed { col, cell ->
                                            if (row == r && col == c) {
                                                cell.copy(value = solution[r][c], notes = emptySet())
                                            } else {
                                                cell
                                            }
                                        }
                                    }
                                )
                                hintsRemaining--
                            }
                        }
                    },
                    enabled = hintsRemaining > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((50 * scale).dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text("${strings.hint} ($hintsRemaining)", fontSize = (20 * scale).sp)
                }

                Spacer(modifier = Modifier.height((8 * scale).dp))

                Button(
                    onClick = {
                        if (!isPaused && moveHistory.isNotEmpty()) {
                            boardState = moveHistory.removeAt(moveHistory.lastIndex)
                        }
                    },
                    enabled = moveHistory.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((50 * scale).dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(strings.undo, fontSize = (20 * scale).sp)
                }
            }
        }
    } else {
        // LAYOUT VERTICAL
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding((16 * scale).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height((42 * scale).dp))

            // FILA 1: Tornar | Títol Daily | Nou Joc
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Esquerra: Tornar
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .width((160 * scale).dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(strings.back, fontSize = (24 * scale).sp)
                }

                // Centre: Títol Daily
                if (isDaily) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((0 * scale).dp)
                    ) {
                        Text(
                            text = strings.dailySudokuTitle,
                            fontSize = (24 * scale).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                        Text(
                            text = DailySudokuManager.getTodayFormatted(),
                            fontSize = (20 * scale).sp,
                            lineHeight = (16 * scale).sp,
                            color = Color.DarkGray
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width((1).dp))
                }

                // Dreta: Nou Joc (o Spacer si és Daily)
                if (!isDaily) {
                    Button(
                        onClick = {
                            shouldSaveOnExit = false
                            GameStateManager.clearGame(context, mode, difficulty, isDaily, isZenMode)
                            resetTrigger++
                            hadErrorsDuringGame = false
                        },
                        modifier = Modifier
                            .height((50 * scale).dp)
                            .width((180 * scale).dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(strings.newGame, fontSize = (20 * scale).sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width((180 * scale).dp))
                }
            }

            Spacer(modifier = Modifier.height((8 * scale).dp))

            // FILA 2: Nivell + Timer (esquerra) | Reiniciar (dreta)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Esquerra: Spacer per equilibrar
                Spacer(
                    modifier = Modifier
                        .width((160 * scale).dp)
                )

                // Centre: Nivell + Timer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Top)
                ) {
                    Text(
                        text = when (difficulty) {
                            Difficulty.EASY -> strings.difficultyEasy
                            Difficulty.MEDIUM -> strings.difficultyMedium
                            Difficulty.HARD -> strings.difficultyHard
                        },
                        modifier = Modifier.padding(bottom = 0.dp),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = (24 * scale).sp,
                            lineHeight = (18 * scale).sp
                        )
                        )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Reprendre" else "Pausar",
                            modifier = Modifier
                                .size((24 * scale).dp)
                                .clickable {
                                    if (isPaused) {
                                        startTimeOffset = pausedAtSeconds
                                        isPaused = false
                                    } else {
                                        isPaused = true
                                        pausedAtSeconds = currentElapsedSeconds
                                        showPauseDialog = true
                                    }
                                }
                        )
                        Spacer(modifier = Modifier.width((8 * scale).dp))
                        if (isZenMode) {
                            Text(
                                text = "${strings.moves}: $moveCount",
                                fontSize = (24 * scale).sp,
                                color = Color(0xFF4CAF50))
                        } else {
                            Text(text = displayTimerText, fontSize = (24 * scale).sp)
                        }
                    }
                }

                // Dreta: Reiniciar
                Button(
                    onClick = {
                        if (!isPaused) {
                            moveHistory.clear()
                            boardState = initialBoard.map { row -> row.map { cell -> cell.copy() } }
                            selectedCell = null
                            selectedNumber = null
                            moveCount = 0
                            hadErrorsDuringGame = false
                            hintsRemaining = when (difficulty) {
                                Difficulty.EASY -> 5
                                Difficulty.MEDIUM -> 3
                                Difficulty.HARD -> 1
                            }
                        }
                    },
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .width((180 * scale).dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(strings.reset, fontSize = (20 * scale).sp)
                }
            }

            Spacer(modifier = Modifier.height((12 * scale).dp))

            // FILA 3: Tauler
            SudokuBoard(
                board = boardState,
                selectedCell = selectedCell,
                pencilAutoMode = pencilMode == PencilMode.AUTO && !isPaused,
                recognizer = digitRecognizer,
                onDigitDrawn = { row, col, digit, asNote ->
                    if (!isPaused) applyRecognizedDigit(row, col, digit, asNote)
                },
                onCellClick = { row, col ->
                    if (!isPaused && !boardState[row][col].isFixed) {
                        if (pencilMode == PencilMode.ON) {
                            selectedCell = Pair(row, col)
                            showDrawingCanvas = true
                        } else {
                            selectedCell = Pair(row, col)
                        }
                    }
                },
                onCellLongClick = { row, col ->
                    if (!isPaused && notesMode == NotesMode.AUTO && !boardState[row][col].isFixed) {
                        val validNotes = getValidNotesForCell(row, col)
                        updateBoard(boardState.mapIndexed { r, rowList ->
                            rowList.mapIndexed { c, cell ->
                                if (r == row && c == col) cell.copy(notes = validNotes, value = 0) else cell
                            }
                        })
                    }
                }
            )

            Spacer(modifier = Modifier.height((10 * scale).dp))

            // Fila de números
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = (3 * scale).dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (number in 1..9) {
                    Button(
                        onClick = {
                            if (!isPaused) {
                                selectedCell?.let { (row, col) ->
                                    if (!boardState[row][col].isFixed) {
                                        selectedNumber = number
                                        updateBoard(
                                            boardState.mapIndexed { r, rowList ->
                                                rowList.mapIndexed { c, cell ->
                                                    if (r == row && c == col) {
                                                        when (notesMode) {
                                                            NotesMode.MANUAL -> {
                                                                val newNotes = if (cell.notes.contains(number)) {
                                                                    cell.notes - number
                                                                } else {
                                                                    cell.notes + number
                                                                }
                                                                cell.copy(notes = newNotes)
                                                            }
                                                            NotesMode.AUTO -> {
                                                                cell.copy(value = number, notes = emptySet())
                                                            }
                                                            NotesMode.OFF -> {
                                                                cell.copy(value = number, notes = emptySet())
                                                            }
                                                        }
                                                    } else {
                                                        cell
                                                    }
                                                }
                                            }
                                        )
                                        if (notesMode != NotesMode.MANUAL) {
                                            selectedCell = null
                                        }
                                        selectedNumber = null
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size((52 * scale).dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedNumber == number) Color.Black else Color.White,
                            contentColor = if (selectedNumber == number) Color.White else Color.Black
                        ),
                        border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(text = number.toString(), fontSize = (40 * scale).sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height((12 * scale).dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        if (!isPaused) {
                            pencilMode = when (pencilMode) {
                                PencilMode.OFF -> PencilMode.ON
                                PencilMode.ON -> PencilMode.AUTO
                                PencilMode.AUTO -> PencilMode.OFF
                            }
                        }
                    },
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .weight(1f)
                        .padding(horizontal = (1 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pencilMode != PencilMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (pencilMode != PencilMode.OFF) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(
                        text = when (pencilMode) {
                            PencilMode.OFF -> strings.pencilOff
                            PencilMode.ON -> strings.pencilOn
                            PencilMode.AUTO -> strings.pencilAuto
                        },
                        fontSize = (20 * scale).sp
                    )
                }

                Button(
                    onClick = {
                        if (!isPaused) {
                            notesMode = when (notesMode) {
                                NotesMode.OFF -> NotesMode.MANUAL
                                NotesMode.MANUAL -> {
                                    showAutoNotesDialog = true
                                    NotesMode.AUTO
                                }
                                NotesMode.AUTO -> NotesMode.OFF
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height((50 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (notesMode) {
                            NotesMode.OFF -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.primary
                        },
                        contentColor = when (notesMode) {
                            NotesMode.OFF -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onPrimary
                        }
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(
                        text = when (notesMode) {
                            NotesMode.OFF -> strings.notesOff
                            NotesMode.MANUAL -> strings.notesManual
                            NotesMode.AUTO -> strings.notesAuto
                        },
                        fontSize = (20 * scale).sp
                    )
                }

                Button(
                    onClick = {
                        if (!isPaused) {
                            selectedCell?.let { (row, col) ->
                                if (!boardState[row][col].isFixed) {
                                    updateBoard(
                                        boardState.mapIndexed { r, rowList ->
                                            rowList.mapIndexed { c, cell ->
                                                if (r == row && c == col) {
                                                    cell.copy(value = 0, notes = emptySet())
                                                } else {
                                                    cell
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .weight(1f)
                        .padding(horizontal = (1 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(strings.erase, fontSize = (20 * scale).sp)
                }

                Button(
                    onClick = {
                        if (!isPaused && hintsRemaining > 0) {
                            val emptyCells = mutableListOf<Pair<Int, Int>>()
                            for (r in 0 until 9) {
                                for (c in 0 until 9) {
                                    if (boardState[r][c].value == 0 && !boardState[r][c].isFixed) {
                                        emptyCells.add(Pair(r, c))
                                    }
                                }
                            }
                            if (emptyCells.isNotEmpty()) {
                                val (r, c) = emptyCells.random()
                                updateBoard(
                                    boardState.mapIndexed { row, rowList ->
                                        rowList.mapIndexed { col, cell ->
                                            if (row == r && col == c) {
                                                cell.copy(value = solution[r][c], notes = emptySet())
                                            } else {
                                                cell
                                            }
                                        }
                                    }
                                )
                                hintsRemaining--
                            }
                        }
                    },
                    enabled = hintsRemaining > 0,
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .weight(1f)
                        .padding(horizontal = (1 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text("${strings.hint} ($hintsRemaining)", fontSize = (20 * scale).sp)
                }

                Button(
                    onClick = {
                        if (!isPaused && moveHistory.isNotEmpty()) {
                            boardState = moveHistory.removeAt(moveHistory.lastIndex)
                        }
                    },
                    enabled = moveHistory.isNotEmpty(),
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .weight(1f)
                        .padding(horizontal = (1 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(strings.undo, fontSize = (20 * scale).sp)
                }
            }
        }
    }

    // Diàleg canvas dibuix
    if (showDrawingCanvas) {
        AlertDialog(
            onDismissRequest = { showDrawingCanvas = false },
            title = { Text("Reconeixement de dígits", fontSize = (24 * scale).sp) },
            text = {
                DrawingCanvas(
                    onDigitRecognized = { digit ->
                        selectedCell?.let { (row, col) ->
                            if (!boardState[row][col].isFixed && digit in 1..9) {
                                updateBoard(
                                    boardState.mapIndexed { r, rowList ->
                                        rowList.mapIndexed { c, cell ->
                                            if (r == row && c == col) {
                                                when (notesMode) {
                                                    NotesMode.MANUAL -> {
                                                        val newNotes = if (cell.notes.contains(digit)) {
                                                            cell.notes - digit
                                                        } else {
                                                            cell.notes + digit
                                                        }
                                                        cell.copy(notes = newNotes)
                                                    }
                                                    NotesMode.AUTO, NotesMode.OFF -> {
                                                        cell.copy(value = digit, notes = emptySet())
                                                    }
                                                }
                                            } else {
                                                cell
                                            }
                                        }
                                    }
                                )
                                showDrawingCanvas = false
                            }
                        }
                    },
                    onDismiss = { showDrawingCanvas = false }
                )
            },
            confirmButton = {}
        )
    }

    // Diàleg de pausa
    if (showPauseDialog) {  // ✅ ESBORRAT && isPaused (sempre és true aquí)
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = strings.gamePaused,
                    fontSize = (32 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = strings.gamePausedMessage,
                    fontSize = (26 * scale).sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        startTimeOffset = pausedAtSeconds
                        isPaused = false
                        showPauseDialog = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Text(strings.resume, fontSize = (24 * scale).sp)
                }
            },
            dismissButton = null
        )
    }

    // Diàleg d'error
    if (showErrorDialog) {
        LaunchedEffect(Unit) {
            pausedAtSeconds = currentElapsedSeconds
            isPaused = true
        }

        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                startTimeOffset = pausedAtSeconds
                isPaused = false
            },
            title = {
                Text(
                    text = strings.error,
                    fontSize = (32 * scale).sp,
                    lineHeight = (40 * scale).sp,
                    color = Color.Red
                )
            },
            text = {
                Text(
                    text = strings.errorMessage,
                    fontSize = (24 * scale).sp,
                    lineHeight = (32 * scale).sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showErrorDialog = false
                        startTimeOffset = pausedAtSeconds
                        isPaused = false
                    },
                    modifier = Modifier.height((50 * scale).dp)
                ) {
                    Text(strings.review, fontSize = (22 * scale).sp)
                }
            }
        )
    }

    // Diàleg de timeout (Time Attack)
    if (showTimeoutDialog) {
        AlertDialog(
            onDismissRequest = {
                showTimeoutDialog = false
                isPaused = false
            },
            title = {
                Text(
                    text = strings.timeUp,
                    fontSize = (32 * scale).sp,
                    lineHeight = (40 * scale).sp,
                    color = Color.Red
                )
            },
            text = {
                Text(
                    text = strings.timeUpMessage,
                    fontSize = (24 * scale).sp,
                    lineHeight = (32 * scale).sp
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)
                ) {
                    if(!isDaily) {
                        Button(
                            onClick = {
                                showTimeoutDialog = false
                                shouldSaveOnExit = false
                                GameStateManager.clearGame(context, mode, difficulty, isDaily, isZenMode)
                                resetTrigger++
                                hadErrorsDuringGame = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height((50 * scale).dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(strings.newGame, fontSize = (22 * scale).sp)
                        }
                    }

                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height((50 * scale).dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(strings.backToMenu, fontSize = (22 * scale).sp)
                    }
                }
            },
            dismissButton = null
        )
    }

    // Diàleg de victòria
    if (showVictoryDialog) {
        AlertDialog(
            onDismissRequest = { showVictoryDialog = false },
            title = {
                Text(
                    text = strings.congratulations,
                    fontSize = (32 * scale).sp,
                    lineHeight = (40 * scale).sp
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isZenMode) {
                        Text(
                            text = "${strings.completed}\n\n${strings.moves}: $moveCount",
                            fontSize = (24 * scale).sp,
                            lineHeight = (32 * scale).sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "${strings.completed}\n\n${strings.time} $displayTimerText",
                            fontSize = (24 * scale).sp,
                            lineHeight = (32 * scale).sp,
                            textAlign = TextAlign.Center
                        )

                        // Info Daily
                        if (isDaily) {
                            Spacer(modifier = Modifier.height((16 * scale).dp))
                            Text(
                                text = "🔥 ${strings.currentStreak}: ${DailySudokuManager.getCurrentStreak(context)} ${strings.days}",
                                fontSize = (20 * scale).sp,
                                color = Color(0xFFFF9800),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)
                ) {
                    if(!isDaily) {
                        Button(
                            onClick = {
                                showVictoryDialog = false
                                shouldSaveOnExit = false
                                GameStateManager.clearGame(context, mode, difficulty, isDaily, isZenMode)
                                resetTrigger++
                                hadErrorsDuringGame = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height((50 * scale).dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(strings.newGame, fontSize = (22 * scale).sp)
                        }
                    }

                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height((50 * scale).dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(strings.backToMenu, fontSize = (22 * scale).sp)
                    }
                }
            },
            dismissButton = null
        )
    }

    // Diàleg de continuar partida guardada
    if (showResumeDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = strings.resumeGame,
                    fontSize = (32 * scale).sp,
                    lineHeight = (40 * scale).sp
                )
            },
            text = {
                Column {
                    Text(
                        text = strings.resumeGameMessage,
                        fontSize = (26 * scale).sp,
                        lineHeight = (30 * scale).sp
                    )

                    Spacer(modifier = Modifier.height((16 * scale).dp))

                    Text(
                        text = if (isZenMode) {
                            "${strings.moves}: ${savedGame!!.moveCount}"
                        } else {
                            formatTime(savedGame!!.elapsedSeconds)
                        },
                        fontSize = (22 * scale).sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy((4 * scale).dp)
                ) {
                    Button(
                        onClick = {
                            startTimeOffset = pausedAtSeconds
                            showResumeDialog = false
                            isPaused = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(strings.continue_, fontSize = (22 * scale).sp)
                    }
                    if (!isDaily) {
                        Button(
                            onClick = {
                                showResumeDialog = false
                                shouldSaveOnExit = false
                                GameStateManager.clearGame(context, mode, difficulty, isDaily, isZenMode)
                                resetTrigger++
                                hadErrorsDuringGame = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentHeight(),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(strings.newGame, fontSize = (22 * scale).sp)
                        }
                    }
                }
            },
            dismissButton = null
        )
    }
}

@Composable
fun SudokuBoard(
    board: List<List<SudokuCell>>,
    // ✅ ESBORRAT: solution (no s'usava)
    selectedCell: Pair<Int, Int>?,
    onCellClick: (Int, Int) -> Unit,
    onCellLongClick: ((Int, Int) -> Unit)? = null,
    pencilAutoMode: Boolean = false,
    recognizer: DigitRecognizer? = null,
    onDigitDrawn: ((Int, Int, Int, Boolean) -> Unit)? = null
) {
    val scale = AdaptiveSizes.getScaleFactor()
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val cellSize = (screenWidthDp - 32) / 9
    val noteScale = (cellSize / 40f).coerceIn(0.2f, 2.2f)

    // Estat per al dibuix directe sobre la cel·la (mode Llapis AUTO)
    var drawingCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var paths by remember { mutableStateOf(listOf<Path>()) }
    var currentPath by remember { mutableStateOf(Path()) }
    var strokeEndTrigger by remember { mutableIntStateOf(0) }
    var cellWidthPx by remember { mutableStateOf(1) }
    var cellHeightPx by remember { mutableStateOf(1) }

    // Un traç és un "subratllat de nota" si és ample, gairebé horitzontal, i
    // se situa a la meitat inferior de la cel·la (distint dels traços de dígit)
    fun isUnderline(path: Path, w: Float, h: Float): Boolean {
        val b = path.getBounds()
        val wide = b.width >= 0.60f * w        // ocupa la major part de l'amplada
        val flat = b.height <= 0.25f * h       // gairebé horitzontal
        val cy = (b.top + b.bottom) / 2f
        val low = cy >= 0.60f * h              // a la part baixa de la cel·la
        return wide && flat && low
    }

    // Reconeix la tinta acumulada d'una cel·la i l'escriu al tauler.
    // Si hi ha un traç de subratllat, el dígit es marca com a nota.
    fun recognizeCell(cell: Pair<Int, Int>, cellPaths: List<Path>) {
        if (cellPaths.isEmpty() || recognizer == null) return
        val w = cellWidthPx.toFloat()
        val h = cellHeightPx.toFloat()
        val asNote = cellPaths.any { isUnderline(it, w, h) }
        val digitStrokes = cellPaths.filterNot { isUnderline(it, w, h) }
        if (digitStrokes.isEmpty()) return  // només un subratllat: no fem res
        val strokeWidthPx = 20f * scale
        val squarePx = minOf(cellWidthPx, cellHeightPx).coerceAtLeast(1)
        val bitmap = pathsToBitmap(digitStrokes, squarePx, strokeWidthPx)
        val digit = recognizer.recognizeDigit(bitmap)
        onDigitDrawn?.invoke(cell.first, cell.second, digit, asNote)
    }

    // Quan s'acaba un traç, esperem una pausa i reconeixem el dígit
    LaunchedEffect(strokeEndTrigger) {
        if (strokeEndTrigger > 0 && paths.isNotEmpty() && drawingCell != null) {
            delay(900)
            recognizeCell(drawingCell!!, paths)
            paths = emptyList()
            currentPath = Path()
            drawingCell = null
        }
    }
    val notePadding = when {
        screenWidthDp < 360 -> (2 * scale).dp
        screenWidthDp < 600 -> (2 * scale).dp
        else -> (8 * scale).dp
    }

    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .border((3 * scale).dp, Color.Black)
    ) {
        for (row in 0 until 9) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0 until 9) {
                    val cell = board[row][col]
                    val isSelected = selectedCell == Pair(row, col)

                    val textColor = when {
                        cell.isFixed -> Color(0xFF263238)
                        cell.value == 0 -> Color.Black
                        else -> Color.DarkGray //(0xFF2196F3)
                    }

                    val topBorder = when {
                        row == 3 || row == 6 -> (2 * scale).dp
                        else -> (0.5 * scale).dp
                    }

                    val leftBorder = when {
                        col == 3 || col == 6 -> (2 * scale).dp
                        else -> (0.5 * scale).dp
                    }

                    val bottomBorder = if (row == 8) 0.dp else (0.5 * scale).dp
                    val rightBorder = if (col == 8) 0.dp else (0.5 * scale).dp

                    val isDrawingHere = drawingCell == Pair(row, col)
                    val canDrawHere = pencilAutoMode && !cell.isFixed

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                when {
                                    isDrawingHere -> Color(0xFFFFCCBC)
                                    isSelected -> Color(0xFFFFCCBC)
                                    cell.isFixed -> Color.LightGray
                                    else -> Color.White
                                }
                            )
                            .then(
                                if (canDrawHere) {
                                    Modifier
                                        .onSizeChanged {
                                            cellWidthPx = it.width
                                            cellHeightPx = it.height
                                        }
                                        .graphicsLayer {
                                            compositingStrategy = CompositingStrategy.Offscreen
                                        }
                                        .pointerInput(row, col) {
                                            // awaitEachGesture captura el pen-down immediatament,
                                            // sense el "touch slop" que retarda detectDragGestures
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                if (drawingCell != Pair(row, col)) {
                                                    // Si hi havia tinta pendent en una altra
                                                    // cel·la, la reconeixem ara mateix en lloc
                                                    // d'esborrar-la
                                                    val prev = drawingCell
                                                    if (prev != null && paths.isNotEmpty()) {
                                                        recognizeCell(prev, paths)
                                                    }
                                                    drawingCell = Pair(row, col)
                                                    paths = emptyList()
                                                    // Reinicia (i cancel·la) el temporitzador
                                                    // pendent de la cel·la anterior
                                                    strokeEndTrigger++
                                                }
                                                val newPath = Path().apply {
                                                    moveTo(down.position.x, down.position.y)
                                                }
                                                currentPath = newPath
                                                down.consume()

                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val change = event.changes.firstOrNull { it.id == down.id }
                                                        ?: break
                                                    if (!change.pressed) {
                                                        // pen-up: tanquem el traç
                                                        paths = paths + newPath
                                                        currentPath = Path()
                                                        strokeEndTrigger++
                                                        change.consume()
                                                        break
                                                    }
                                                    newPath.lineTo(
                                                        change.position.x,
                                                        change.position.y
                                                    )
                                                    // Forcem recomposició de l'overlay de tinta
                                                    currentPath = Path().apply { addPath(newPath) }
                                                    change.consume()
                                                }
                                            }
                                        }
                                } else {
                                    Modifier.combinedClickable(
                                        onClick = { onCellClick(row, col) },
                                        onLongClick = { onCellLongClick?.invoke(row, col) }
                                    )
                                }
                            )
                            .drawBehind {
                                drawLine(
                                    color = Color.Black,
                                    start = Offset(0f, topBorder.toPx() / 2),
                                    end = Offset(size.width, topBorder.toPx() / 2),
                                    strokeWidth = topBorder.toPx()
                                )
                                drawLine(
                                    color = Color.Black,
                                    start = Offset(leftBorder.toPx() / 2, 0f),
                                    end = Offset(leftBorder.toPx() / 2, size.height),
                                    strokeWidth = leftBorder.toPx()
                                )
                                if (bottomBorder > 0.dp) {
                                    drawLine(
                                        color = Color.Black,
                                        start = Offset(
                                            0f,
                                            size.height - bottomBorder.toPx() / 2
                                        ),
                                        end = Offset(
                                            size.width,
                                            size.height - bottomBorder.toPx() / 2
                                        ),
                                        strokeWidth = bottomBorder.toPx()
                                    )
                                }
                                if (rightBorder > 0.dp) {
                                    drawLine(
                                        color = Color.Black,
                                        start = Offset(size.width - rightBorder.toPx() / 2, 0f),
                                        end = Offset(
                                            size.width - rightBorder.toPx() / 2,
                                            size.height
                                        ),
                                        strokeWidth = rightBorder.toPx()
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Overlay de tinta mentre es dibuixa en aquesta cel·la (Llapis AUTO)
                        if (isDrawingHere) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Traç visible més fi (30%); el gruix de reconeixement no canvia
                                val strokeWidthPx = 6f * scale
                                paths.forEach { p ->
                                    drawPath(
                                        path = p,
                                        color = Color.Black,
                                        style = Stroke(width = strokeWidthPx)
                                    )
                                }
                                drawPath(
                                    path = currentPath,
                                    color = Color.Black,
                                    style = Stroke(width = strokeWidthPx)
                                )
                            }
                        }
                        if (cell.value != 0) {
                            Text(
                                text = cell.value.toString(),
                                fontSize = (30 * scale).sp,
                                color = textColor,
                                fontWeight = if (cell.isFixed) FontWeight.Bold else FontWeight.Normal
                            )
                        } else if (cell.notes.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(notePadding),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy((0 * noteScale).dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    for (rowNotes in 0 until 3) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            for (colNotes in 0 until 3) {
                                                val noteNumber = rowNotes * 3 + colNotes + 1
                                                Box(
                                                    modifier = Modifier.weight(1f),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (cell.notes.contains(noteNumber)) {
                                                            noteNumber.toString()
                                                        } else {
                                                            ""
                                                        },
                                                        fontSize = (10 * noteScale).sp,
                                                        color = Color.DarkGray,
                                                        textAlign = TextAlign.Center,
                                                        fontWeight = FontWeight.Bold,
                                                        lineHeight = (8 * noteScale).sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberTimer(
    resetTrigger: Int = 0,
    startOffset: Int = 0,
    isPaused: Boolean = false,
    resetTimer: Int = 0,
    onTimeUpdate: (Int) -> Unit
): String {
    var elapsedTime by remember(resetTrigger, resetTimer) { mutableIntStateOf(startOffset) }
    var timeWhenPaused by remember(resetTrigger) { mutableIntStateOf(startOffset) }

    LaunchedEffect(resetTrigger, isPaused, resetTimer, startOffset) {
        if (!isPaused) {
            val startTime = System.currentTimeMillis() - (startOffset * 1000L)
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (!isPaused) {
                    elapsedTime = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                    timeWhenPaused = elapsedTime
                    onTimeUpdate(elapsedTime)
                } else {
                    elapsedTime = timeWhenPaused
                }
            }
        } else {
            elapsedTime = timeWhenPaused
        }
    }

    val minutes = elapsedTime / 60
    val seconds = elapsedTime % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

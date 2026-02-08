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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton



@Composable
fun GameScreen(difficulty: Difficulty, onBack: () -> Unit) {
    val context = LocalContext.current
    val strings = rememberStrings()
    var resetTrigger by remember { mutableStateOf(0) }
    var resetTimerTrigger by remember(resetTrigger) { mutableStateOf(0) } // Reiniciar timer
    var isPaused by remember(resetTrigger) { mutableStateOf(false) } // Pause accessible
    var shouldSaveOnExit by remember(resetTrigger) { mutableStateOf(true) }

    // Factor d'escala adaptatiu
    val scale = AdaptiveSizes.getScaleFactor()

    // Detectar orientació
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Intentar carregar partida guardada
    val savedGame = remember(resetTrigger) {
        if (resetTrigger == 0) {
            GameStateManager.loadGame(context, difficulty)  // ← Passa difficulty
        } else null
    }

    val initialGame = remember(resetTrigger) {
        if (savedGame != null) {
            // Restaurar des de partida guardada
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
            // Generar nou joc
            SudokuGenerator.generate(difficulty)
        }
    }

    // Guardar el tauler REALMENT inicial (només números fixos) per poder reiniciar
    val initialBoard = remember(resetTrigger) {
        if (savedGame != null) {
            // Si ve de partida guardada, crear tauler només amb fixos
            savedGame.board.map { row ->
                row.map { savedCell ->
                    SudokuCell(
                        value = if (savedCell.isFixed) savedCell.value else 0,
                        isFixed = savedCell.isFixed,
                        notes = emptySet()
                    )
                }
            }
        } else {
            // Si és nou joc, usar l'inicial directament
            initialGame.board.map { row ->
                row.map { cell -> cell.copy() }
            }
        }
    }

    // Guardem la solució correcta
    val solution = remember(resetTrigger) { initialGame.solution }

    // Estat editable del tauler
    var boardState by remember(resetTrigger) {
        mutableStateOf(
            initialGame.board.map { row ->
                row.map { cell -> cell.copy() }
            }
        )
    }

    var selectedCell by remember(resetTrigger) { mutableStateOf<Pair<Int, Int>?>(null) }
    var selectedNumber by remember(resetTrigger) { mutableStateOf<Int?>(null) }
    var showVictoryDialog by remember(resetTrigger) { mutableStateOf(false) }
    var showErrorDialog by remember(resetTrigger) { mutableStateOf(false) }
    var showDrawingCanvas by remember { mutableStateOf(false) }
    var isPencilMode by remember(resetTrigger) { mutableStateOf(false) }

    // Temps inicial (restaurar si hi ha partida guardada)
    var startTimeOffset by remember(resetTrigger) { mutableStateOf(savedGame?.elapsedSeconds ?: 0) }

    // Variable per guardar els segons actuals
    var currentElapsedSeconds by remember(resetTrigger) { mutableStateOf(startTimeOffset) }

    val timerText = rememberTimer(
        resetTrigger = resetTrigger,
        startOffset = startTimeOffset,
        isPaused = isPaused,
        resetTimer = resetTimerTrigger,
        onTimeUpdate = { elapsedSeconds ->
            currentElapsedSeconds = elapsedSeconds
        }
    )


    var hintsRemaining by remember(resetTrigger) {
        mutableStateOf(
            savedGame?.hintsRemaining ?: when (difficulty) {
                Difficulty.EASY -> 5
                Difficulty.MEDIUM -> 3
                Difficulty.HARD -> 1
            }
        )
    }

    var isNotesMode by remember(resetTrigger) { mutableStateOf(false) }
    val moveHistory = remember(resetTrigger) { mutableStateListOf<List<List<SudokuCell>>>() }

    // Guardar automàticament cada canvi
    LaunchedEffect(boardState, hintsRemaining) {
        if (!showVictoryDialog) {
            val savedState = SavedGameState(
                difficulty = difficulty.name,
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
                elapsedSeconds = currentElapsedSeconds,  // ← Usar la variable
                hintsRemaining = hintsRemaining
            )
            GameStateManager.saveGame(context, savedState)
        }
    }

    // Guardar quan l'usuari surt de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            if (!showVictoryDialog && shouldSaveOnExit) {
                val savedState = SavedGameState(
                    difficulty = difficulty.name,
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
                    elapsedSeconds = currentElapsedSeconds,  // ← Usar la variable
                    hintsRemaining = hintsRemaining
                )
                GameStateManager.saveGame(context, savedState)
            }
        }
    }

    // Esborrar partida guardada quan es completa
    LaunchedEffect(showVictoryDialog) {
        if (showVictoryDialog) {
            GameStateManager.clearGame(context, difficulty)  // ← Passa difficulty
        }
    }

    // Funció per actualitzar el tauler i guardar a l'historial
    fun updateBoard(newBoard: List<List<SudokuCell>>) {
        moveHistory.add(boardState)
        boardState = newBoard
    }

    // Comprovar si el tauler està complet i correcte
    val isComplete = boardState.all { row -> row.all { it.value != 0 } }
    val isCorrect = if (isComplete) {
        boardState.mapIndexed { r, row ->
            row.mapIndexed { c, cell ->
                cell.value == solution[r][c]
            }.all { it }
        }.all { it }
    } else {
        false
    }

    // Detectar quan es completa el Sudoku
    LaunchedEffect(boardState) {
        val complete = boardState.all { row -> row.all { it.value != 0 } }
        if (complete && !showVictoryDialog) {
            val correct = boardState.mapIndexed { r, row ->
                row.mapIndexed { c, cell ->
                    cell.value == solution[r][c]
                }.all { it }
            }.all { it }

            if (correct) {
                // Registrar estadística
                StatisticsManager.recordCompletion(context, difficulty, currentElapsedSeconds)
                showVictoryDialog = true
            } else {
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
            // COLUMNA ESQUERRA: Botó Back + Sudoku
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height((36 * scale).dp))

                // Botó Back a dalt a l'esquerra
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .offset(x = (10 * scale).dp)
                        .width((160 * scale).dp)
                        .height((50 * scale).dp)
                ) {
                    Text(strings.back, fontSize = (20 * scale).sp)
                }

                Spacer(modifier = Modifier.height(0.dp))

                // Nivell i Timer SOBRE el Sudoku
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (difficulty) {
                            Difficulty.EASY -> strings.difficultyEasy
                            Difficulty.MEDIUM -> strings.difficultyMedium
                            Difficulty.HARD -> strings.difficultyHard
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = (24 * scale).sp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = { isPaused = !isPaused },
                            modifier = Modifier.size((36 * scale).dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Reprendre" else "Pausar",
                                modifier = Modifier.size((24 * scale).dp)
                            )
                        }

                        Text(text = timerText, fontSize = (24 * scale).sp)

                        IconButton(
                            onClick = {
                                startTimeOffset = 0
                                resetTimerTrigger++
                            },
                            modifier = Modifier.size((36 * scale).dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reiniciar temps",
                                modifier = Modifier.size((24 * scale).dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(0.dp))

                // Sudoku centrat
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SudokuBoard(
                        board = boardState,
                        solution = solution,
                        selectedCell = selectedCell,
                        onCellClick = { row, col ->
                            if (!boardState[row][col].isFixed) {
                                if (isPencilMode) {
                                    // Mode llapis: obrir canvas directament
                                    selectedCell = Pair(row, col)
                                    showDrawingCanvas = true
                                } else {
                                    // Mode normal: només seleccionar
                                    selectedCell = Pair(row, col)
                                }
                            }
                        }
                    )

                }
            }

            Spacer(modifier = Modifier.width((16 * scale).dp))

            // COLUMNA DRETA: Controls
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height((36 * scale).dp))

                // Nou Joc
                Button(
                    onClick = {
                        shouldSaveOnExit = false
                        GameStateManager.clearGame(context, difficulty)  // ← Passa difficulty
                        resetTrigger++
                    },
                    modifier = Modifier.fillMaxWidth().height((50 * scale).dp)
                ) {
                    Text(strings.newGame, fontSize = (20 * scale).sp)
                }

                Spacer(modifier = Modifier.height((4 * scale).dp))

                // BOTÓ REINICIAR
                Button(
                    onClick = {
                        moveHistory.clear()
                        boardState = initialBoard.map { row ->
                            row.map { cell -> cell.copy() }
                        }
                        selectedCell = null
                        selectedNumber = null
                        startTimeOffset = 0
                        hintsRemaining = when (difficulty) {
                            Difficulty.EASY -> 5
                            Difficulty.MEDIUM -> 3
                            Difficulty.HARD -> 1
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height((50 * scale).dp)
                ) {
                    Text(strings.reset, fontSize = (20 * scale).sp)
                }


                Spacer(modifier = Modifier.height((20 * scale).dp))

                // Números (grid 3x3)
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
                                        selectedCell?.let { (row, col) ->
                                            if (!boardState[row][col].isFixed) {
                                                selectedNumber = number
                                                val newBoard = boardState.mapIndexed { r, rowList ->
                                                    rowList.mapIndexed { c, cell ->
                                                        if (r == row && c == col) {
                                                            if (isNotesMode) {
                                                                val newNotes =
                                                                    if (cell.notes.contains(number)) {
                                                                        cell.notes - number
                                                                    } else {
                                                                        cell.notes + number
                                                                    }
                                                                cell.copy(notes = newNotes)
                                                            } else {
                                                                cell.copy(
                                                                    value = number,
                                                                    notes = emptySet()
                                                                )
                                                            }
                                                        } else {
                                                            cell
                                                        }
                                                    }
                                                }
                                                updateBoard(newBoard)
                                                selectedNumber = null
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
                                    border = BorderStroke(
                                        (2 * scale).dp,
                                        MaterialTheme.colorScheme.onBackground
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(text = number.toString(), fontSize = (32 * scale).sp)
                                }
                            }
                        }
                        // Afegir espai entre files (excepte després de l'última)
                        if (rowNum < 2) {
                            Spacer(modifier = Modifier.height((8 * scale).dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height((24 * scale).dp))

                // 5 botons en columna
                // BOTÓ LLAPIS MODE
                Button(
                    onClick = { isPencilMode = !isPencilMode },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((50 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPencilMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isPencilMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(
                        if (isPencilMode) strings.pencilOn else strings.pencilOff,
                        fontSize = (20 * scale).sp
                    )
                }

                Spacer(modifier = Modifier.height((8 * scale).dp))

                Button(
                    onClick = { isNotesMode = !isNotesMode },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((50 * scale).dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNotesMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isNotesMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(
                        if (isNotesMode) strings.notesOn else strings.notesOff,
                        fontSize = (20 * scale).sp
                    )
                }

                Spacer(modifier = Modifier.height((8 * scale).dp))

                Button(
                    onClick = {
                        selectedCell?.let { (row, col) ->
                            if (!boardState[row][col].isFixed) {
                                val newBoard = boardState.mapIndexed { r, rowList ->
                                    rowList.mapIndexed { c, cell ->
                                        if (r == row && c == col) {
                                            cell.copy(value = 0, notes = emptySet())
                                        } else {
                                            cell
                                        }
                                    }
                                }
                                updateBoard(newBoard)
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
                        if (hintsRemaining > 0) {
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
                                val newBoard = boardState.mapIndexed { row, rowList ->
                                    rowList.mapIndexed { col, cell ->
                                        if (row == r && col == c) {
                                            cell.copy(value = solution[r][c], notes = emptySet())
                                        } else {
                                            cell
                                        }
                                    }
                                }
                                updateBoard(newBoard)
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
                        if (moveHistory.isNotEmpty()) {
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
            Spacer(modifier = Modifier.height((38 * scale).dp))

            // FILA SUPERIOR: Botó tornar (esquerra) i Nou Joc (dreta)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .width((160 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        strings.back,
                        fontSize = (20 * scale).sp
                    )
                }

                Button(
                    onClick = {
                        shouldSaveOnExit = false
                        GameStateManager.clearGame(context, difficulty)  // ← Passa difficulty
                        resetTrigger++
                    },
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .width((180 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        strings.newGame,
                        fontSize = (20 * scale).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height((4 * scale).dp))

            // FILA AMB NIVELL/TIMER CENTRAT I BOTÓ REINICIAR A LA DRETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width((180 * scale).dp)) // Espai per equilibrar

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (difficulty) {
                            Difficulty.EASY -> strings.difficultyEasy
                            Difficulty.MEDIUM -> strings.difficultyMedium
                            Difficulty.HARD -> strings.difficultyHard
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = (24 * scale).sp
                        )
                    )

                    Spacer(modifier = Modifier.height((0 * scale).dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = { isPaused = !isPaused },
                            modifier = Modifier.size((36 * scale).dp)
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Reprendre" else "Pausar",
                                modifier = Modifier.size((24 * scale).dp)
                            )
                        }

                        Text(text = timerText, fontSize = (24 * scale).sp)

                        IconButton(
                            onClick = {
                                startTimeOffset = 0
                                resetTimerTrigger++
                            },
                            modifier = Modifier.size((36 * scale).dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reiniciar temps",
                                modifier = Modifier.size((24 * scale).dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        moveHistory.clear()
                        boardState = initialBoard.map { row ->
                            row.map { cell -> cell.copy() }
                        }
                        selectedCell = null
                        selectedNumber = null
                        startTimeOffset = 0
                        hintsRemaining = when (difficulty) {
                            Difficulty.EASY -> 5
                            Difficulty.MEDIUM -> 3
                            Difficulty.HARD -> 1
                        }
                    },
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .width((180 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(strings.reset, fontSize = (20 * scale).sp)
                }
            }

            Spacer(modifier = Modifier.height((16 * scale).dp))

            SudokuBoard(
                board = boardState,
                solution = solution,
                selectedCell = selectedCell,
                onCellClick = { row, col ->
                    if (!boardState[row][col].isFixed) {
                        if (isPencilMode) {
                            // Mode llapis: obrir canvas directament
                            selectedCell = Pair(row, col)
                            showDrawingCanvas = true
                        } else {
                            // Mode normal: només seleccionar
                            selectedCell = Pair(row, col)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height((16 * scale).dp))

            // Només els números (sense botó de notes)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = (3 * scale).dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (number in 1..9) {
                    Button(
                        onClick = {
                            selectedCell?.let { (row, col) ->
                                if (!boardState[row][col].isFixed) {
                                    selectedNumber = number
                                    val newBoard = boardState.mapIndexed { r, rowList ->
                                        rowList.mapIndexed { c, cell ->
                                            if (r == row && c == col) {
                                                if (isNotesMode) {
                                                    val newNotes =
                                                        if (cell.notes.contains(number)) {
                                                            cell.notes - number
                                                        } else {
                                                            cell.notes + number
                                                        }
                                                    cell.copy(notes = newNotes)
                                                } else {
                                                    cell.copy(value = number, notes = emptySet())
                                                }
                                            } else {
                                                cell
                                            }
                                        }
                                    }
                                    updateBoard(newBoard)
                                    selectedNumber = null
                                }
                            }
                        },
                        modifier = Modifier.size((52 * scale).dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedNumber == number) Color.Black else Color.White,
                            contentColor = if (selectedNumber == number) Color.White else Color.Black
                        ),
                        border = BorderStroke(
                            (2 * scale).dp,
                            MaterialTheme.colorScheme.onBackground
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = number.toString(),
                            fontSize = (40 * scale).sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height((12 * scale).dp))

            // FILA DE 4 BOTONS: Notes, Esborrar, Pista, Desfer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // BOTÓ LLAPIS (NOU)
                Button(
                    onClick = { isPencilMode = !isPencilMode },
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .weight(1f)
                        .padding(horizontal = (1 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPencilMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isPencilMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(
                        if (isPencilMode) strings.pencilOn else strings.pencilOff,
                        fontSize = (20 * scale).sp
                    )
                }

                // BOTÓ NOTES
                Button(
                    onClick = { isNotesMode = !isNotesMode },
                    modifier = Modifier
                        .height((50 * scale).dp)
                        .weight(1f)
                        .padding(horizontal = (1 * scale).dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNotesMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isNotesMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(
                        text = if (isNotesMode) strings.notesOn else strings.notesOff,
                        fontSize = (20 * scale).sp
                    )
                }

                // BOTÓ ESBORRAR
                Button(
                    onClick = {
                        selectedCell?.let { (row, col) ->
                            if (!boardState[row][col].isFixed) {
                                val newBoard = boardState.mapIndexed { r, rowList ->
                                    rowList.mapIndexed { c, cell ->
                                        if (r == row && c == col) {
                                            cell.copy(value = 0, notes = emptySet())
                                        } else {
                                            cell
                                        }
                                    }
                                }
                                updateBoard(newBoard)
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

                // BOTÓ PISTA
                Button(
                    onClick = {
                        if (hintsRemaining > 0) {
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
                                val newBoard = boardState.mapIndexed { row, rowList ->
                                    rowList.mapIndexed { col, cell ->
                                        if (row == r && col == c) {
                                            cell.copy(value = solution[r][c], notes = emptySet())
                                        } else {
                                            cell
                                        }
                                    }
                                }
                                updateBoard(newBoard)
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

                // BOTÓ DESFER
                Button(
                    onClick = {
                        if (moveHistory.isNotEmpty()) {
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
    // Diàleg de canvas de dibuix
    if (showDrawingCanvas) {
        AlertDialog(
            onDismissRequest = { showDrawingCanvas = false },
            title = { Text("Reconeixement de dígits", fontSize = (24 * scale).sp) },
            text = {
                DrawingCanvas(
                    onDigitRecognized = { digit ->
                        // Col·locar el dígit reconegut a la cel·la seleccionada
                        selectedCell?.let { (row, col) ->
                            if (!boardState[row][col].isFixed && digit in 1..9) {
                                val newBoard = boardState.mapIndexed { r, rowList ->
                                    rowList.mapIndexed { c, cell ->
                                        if (r == row && c == col) {
                                            if (isNotesMode) {
                                                // Mode notes: afegir/treure nota
                                                val newNotes = if (cell.notes.contains(digit)) {
                                                    cell.notes - digit
                                                } else {
                                                    cell.notes + digit
                                                }
                                                cell.copy(notes = newNotes)
                                            } else {
                                                // Mode normal: posar número
                                                cell.copy(value = digit, notes = emptySet())
                                            }
                                        } else {
                                            cell
                                        }
                                    }
                                }
                                updateBoard(newBoard)
                            }
                        }
                        showDrawingCanvas = false
                    },
                    onDismiss = { showDrawingCanvas = false }
                )
            },
            confirmButton = {}
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
                Text(
                    text = "${strings.completed}\n\n${strings.time} $timerText",
                    fontSize = (24 * scale).sp,
                    lineHeight = (32 * scale).sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)
                ) {
                    // Botó NOU SUDOKU
                    Button(
                        onClick = {
                            showVictoryDialog = false
                            shouldSaveOnExit = false
                            GameStateManager.clearGame(context, difficulty)
                            resetTrigger++
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height((50 * scale).dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(strings.newGame, fontSize = (18 * scale).sp)
                    }

                    // Botó TORNAR AL MENÚ
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height((50 * scale).dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(strings.backToMenu, fontSize = (18 * scale).sp)
                    }
                }
            },
            dismissButton = null
        )
    }


    // Diàleg d'error
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
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
                    },
                    modifier = Modifier.height((50 * scale).dp)
                ) {
                    Text(strings.review, fontSize = (18 * scale).sp)
                }
            }
        )
    }
}
    @Composable
    fun SudokuBoard(
        board: List<List<SudokuCell>>,
        solution: List<List<Int>>,
        selectedCell: Pair<Int, Int>?,
        onCellClick: (Int, Int) -> Unit
    ) {
        val scale = AdaptiveSizes.getScaleFactor()
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp

        // Calcular mida real de cada cel·la
        val cellSize = (screenWidthDp - 32) / 9  // Restar padding i dividir per 9
        val noteScale = (cellSize / 40f).coerceIn(0.2f, 2.2f)  // Escala específica per notes

        // Padding adaptatiu
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

                        // Determinar el color del text
                        val textColor = when {
                            cell.isFixed -> Color(0xFF263238) // Números inicials
                            cell.value == 0 -> Color.Black  // Cel·la buida
                            else -> Color(0xFF2196F3)  // Tots els números de l'usuari en blau
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

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    when {
                                        isSelected -> Color(0xFFFFE082)     // Seleccionada: groc pastel
                                        cell.isFixed -> Color(0xFFE0E0E0)   // Fixa: gris mitjà
                                        else -> Color.White                 // Editable: blanc
                                    }
                                )
                                .clickable { onCellClick(row, col) }
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
                            if (cell.value != 0) {
                                // Mostrar número principal
                                Text(
                                    text = cell.value.toString(),
                                    fontSize = (30 * scale).sp,
                                    color = textColor,
                                    fontWeight = if (cell.isFixed) FontWeight.Bold else FontWeight.Normal
                                )
                            } else if (cell.notes.isNotEmpty()) {
                                // Mostrar notes en una graella 3x3
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(notePadding),  // ← Padding ajustat a mida
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
                                                horizontalArrangement = Arrangement.SpaceEvenly,  // ← SIN espais entre columnes
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                for (colNotes in 0 until 3) {
                                                    val noteNumber = rowNotes * 3 + colNotes + 1
                                                    Box(
                                                        modifier = Modifier.weight(1f),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = if (cell.notes.contains(
                                                                    noteNumber
                                                                )
                                                            ) {
                                                                noteNumber.toString()  // ← Mostra el número
                                                            } else {
                                                                ""  // ← Espai buit però manté la posició
                                                            },
                                                            fontSize = (10 * noteScale).sp,
                                                            color = Color.Gray,
                                                            textAlign = TextAlign.Center,
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
    onTimeUpdate: (Int) -> Unit = {}  // ← NOVA callback
): String {
    var elapsedTime by remember(resetTrigger, resetTimer) { mutableStateOf(startOffset) }

    LaunchedEffect(resetTrigger, isPaused, resetTimer) {
        val startTime = System.currentTimeMillis() - (startOffset * 1000L)
        while (true) {
            kotlinx.coroutines.delay(1000)
            if (!isPaused) {
                elapsedTime = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                onTimeUpdate(elapsedTime)  // ← Notificar el temps actual
            }
        }
    }

    val minutes = elapsedTime / 60
    val seconds = elapsedTime % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}




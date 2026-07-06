package com.ktacrack.sudokueink

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext


enum class Language(val code: String) {
    CATALAN("ca"),
    SPANISH("es"),
    ENGLISH("en")
}

object LanguageManager {
    private const val PREFS_NAME = "sudoku_language"
    private const val KEY_LANGUAGE = "language"

    fun saveLanguage(context: Context, language: Language) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    fun loadLanguage(context: Context): Language {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANGUAGE, Language.CATALAN.code) ?: Language.CATALAN.code
        return Language.values().find { it.code == code } ?: Language.CATALAN
    }
}

@Composable
fun rememberStrings(): Strings {
    val context = LocalContext.current
    val currentLanguage = remember { LanguageManager.loadLanguage(context) }

    return remember(currentLanguage) {
        when (currentLanguage) {
            Language.CATALAN -> StringsCa
            Language.SPANISH -> StringsEs
            Language.ENGLISH -> StringsEn
        }
    }
}

interface Strings {
    // Menu
    val appTitle: String
    val difficultyEasy: String
    val difficultyMedium: String
    val difficultyHard: String
    val statistics: String
    val createdBy: String
    val settings: String
    val zenMode: String
    val zenModeDescription: String

    // Game
    val back: String
    val newGame: String
    val reset: String
    val moves: String
    val pencil: String
    val pencilOn: String
    val pencilOff: String
    val pencilAuto: String
    val notesOn: String
    val notesOff: String
    val notesManual: String
    val notesAuto: String
    val autoNotesTitle: String
    val autoNotesExplanation: String
    val understood: String
    val erase: String
    val hint: String
    val undo: String

    // Finestra escriure número
    val drawNumber: String
    val recognize: String
    val cancel: String

    //Sudoku Diari
    val dailySudoku: String
    val dailySudokuTitle: String
    val alreadyPlayedToday: String
    val comeBackTomorrow: String
    val currentStreak: String
    val days: String
    val dailyBestTime: String
    val todayDifficulty: String
    
    // Dialogs
    val resumeGame: String
    val resumeGameMessage: String
    val congratulations: String
    val completed: String
    val time: String
    val backToMenu: String
    val continue_: String
    val error: String
    val errorMessage: String
    val review: String
    val timeUp: String
    val timeUpMessage: String
    val gamePaused: String
    val gamePausedMessage: String
    val resume: String

    // Statistics
    val statisticsTitle: String
    val gamesCompleted: String
    val bestTime: String
    val bestRemainingTime: String
    val totalGames: String

    // Game Modes (AFEGIR AQUESTS)
    val normalMode: String
    val attackMode: String

    // Settings
    val settingsTitle: String
    val language: String
    val catalan: String
    val spanish: String
    val english: String

    // Theme
    val theme: String
    val lightTheme: String
    val darkTheme: String

    // ACHIEVEMENTS - Títols generals
    val achievements: String
    val achievementsUnlocked: String
    val achievementUnlocked: String
    val progress: String
    val awesome: String

    // ACHIEVEMENTS - Individuals
    val achievementFirstWin: String
    val achievementFirstWinDesc: String

    val achievement10Games: String
    val achievement10GamesDesc: String

    val achievement50Games: String
    val achievement50GamesDesc: String

    val achievement100Games: String
    val achievement100GamesDesc: String

    val achievementSpeedEasy: String
    val achievementSpeedEasyDesc: String

    val achievementSpeedHard: String
    val achievementSpeedHardDesc: String

    val achievementHardMaster: String
    val achievementHardMasterDesc: String

    val achievementAttackSurvivor: String
    val achievementAttackSurvivorDesc: String

    val achievementNoHints: String
    val achievementNoHintsDesc: String

    val achievementNoErrors: String
    val achievementNoErrorsDesc: String

    val achievementDailyStreak7: String
    val achievementDailyStreak7Desc: String
    val achievementDailyPerfect: String
    val achievementDailyPerfectDesc: String
    val achievementZenMaster: String
    val achievementZenMasterDesc: String
}

object StringsCa : Strings {
    override val appTitle = "Sudoku E-ink"
    override val normalMode = "Joc Normal"
    override val attackMode = "Sudoku Atac"
    override val difficultyEasy = "Nivell Fàcil"
    override val difficultyMedium = "Nivell Intermedi"
    override val difficultyHard = "Nivell Difícil"
    override val statistics = "📊 Estadístiques"
    override val settings = "⚙️ Configuració"
    override val createdBy = "Creada per ktacrack"
    override val zenMode = "Mode Zen"
    override val zenModeDescription = "Sense cronòmetre"

    override val back = "← Tornar"
    override val newGame = "🔄 Nou Joc"
    override val reset = "Reiniciar Sudoku"
    override val moves = "Moviments"
    override val pencil = "Llapis"
    override val pencilOn = "Llapis ON"
    override val pencilOff = "Llapis OFF"
    override val pencilAuto = "Llapis AUTO"
    override val notesOn = "Notes ON"
    override val notesOff = "Notes OFF"
    override val erase = "Esborrar"
    override val hint = "Pista"
    override val undo = "Desfer"

    override val notesManual = "Notes ON"
    override val notesAuto = "Notes AUTO"
    override val autoNotesTitle = "Notes automàtiques"
    override val autoNotesExplanation = "Prem una estona una cel·la per afegir automàticament tots els possibles números com a notes. A mesura que vagis afegint números al sudoku, les notes incompatibles s'aniran esborrant automàticament."
    override val understood = "Entès"

    override val drawNumber = "Dibuixa un número:"
    override val recognize = "Reconèixer"
    override val cancel = "Cancel·lar"
    override val resumeGame= "▶️Continuar partida"
    override val resumeGameMessage = "Tens una partida guardada. Vols continuar-la?"
    override val congratulations = "🎉 Felicitats!"
    override val completed = "Has completat el Sudoku correctament!"
    override val time = "Temps:"
    override val backToMenu = "Tornar al menú"
    override val continue_ = "Continuar"
    override val error = "❌ Hi ha errors!"
    override val errorMessage = "El tauler està complet però té números incorrectes. Revisa les cel·les!"
    override val review = "Revisar"
    override val timeUp = "⏰ Temps esgotat!"
    override val timeUpMessage = "No has pogut completar el sudoku a temps."

    override val dailySudoku = "Sudoku Diari"
    override val dailySudokuTitle = "Sudoku del dia"
    override val todayDifficulty = "Avui"
    override val alreadyPlayedToday = "Ja has jugat el Sudoku d'avui!"
    override val comeBackTomorrow = "Torna demà per un nou repte"
    override val currentStreak = "Ratxa actual"
    override val days = "dies"
    override val dailyBestTime = "Millor temps d'avui"
    
    override val gamePaused = "Joc en pausa"
    override val gamePausedMessage = "Prem reprendre per continuar jugant"
    override val resume = "Reprendre"

    override val statisticsTitle = "📊 Estadístiques"
    override val gamesCompleted = "Partides completades:"
    override val bestTime = "Millor temps:"
    override val bestRemainingTime = "Millor temps restant:"
    override val totalGames = "Total de partides completades"
    override val theme = "Tema"
    override val lightTheme = "Clar"
    override val darkTheme = "Fosc"

    override val settingsTitle = "⚙️ Configuració"
    override val language = "Idioma"
    override val catalan = "Català"
    override val spanish = "Castellà"
    override val english = "Anglès"

    // ACHIEVEMENTS
    override val achievements = "Fites"
    override val achievementsUnlocked = "desbloquejades"
    override val achievementUnlocked = "Fita Desbloquejada!"
    override val progress = "Progrés"
    override val awesome = "Genial!"

    // Individuals
    override val achievementFirstWin = "Primera Victòria"
    override val achievementFirstWinDesc = "Completa el teu primer sudoku"

    override val achievement10Games = "Aficionat"
    override val achievement10GamesDesc = "Completa 10 sudokus"

    override val achievement50Games = "Sudoku Addicte"
    override val achievement50GamesDesc = "Completa 50 sudokus"

    override val achievement100Games = "Mestre Sudoku"
    override val achievement100GamesDesc = "Completa 100 sudokus"

    override val achievementSpeedEasy = "Velocista Fàcil"
    override val achievementSpeedEasyDesc = "Completa fàcil en menys de 3 minuts"

    override val achievementSpeedHard = "Flash Difícil"
    override val achievementSpeedHardDesc = "Completa difícil en menys de 10 minuts"

    override val achievementHardMaster = "Domador de Difícils"
    override val achievementHardMasterDesc = "Completa 25 sudokus difícils"

    override val achievementAttackSurvivor = "Supervivència Extrema"
    override val achievementAttackSurvivorDesc = "Completa mode atac amb menys de 10s restants"

    override val achievementNoHints = "Ment Pura"
    override val achievementNoHintsDesc = "Completa 5 sudokus sense usar pistes"

    override val achievementNoErrors = "Perfeccionista"
    override val achievementNoErrorsDesc = "Completa 10 sudokus sense errors"

    override val achievementDailyStreak7 = "Ratxa Diària"
    override val achievementDailyStreak7Desc = "7 dies consecutius de Sudoku Diari"

    override val achievementDailyPerfect = "Diari Perfecte"
    override val achievementDailyPerfectDesc = "3 Sudoku Diaris sense errors ni pistes"

    override val achievementZenMaster = "Màster Zen"
    override val achievementZenMasterDesc = "50 sudokus Zen completats"
}

object StringsEs : Strings {
    override val appTitle = "Sudoku E-ink"
    override val normalMode = "Juego Normal"
    override val attackMode = "Sudoku Ataque"
    override val difficultyEasy = "Nivel Fácil"
    override val difficultyMedium = "Nivel Intermedio"
    override val difficultyHard = "Nivel Difícil"
    override val statistics = "📊 Estadísticas"
    override val settings = "⚙️ Configuración"
    override val createdBy = "Creada por ktacrack"
    override val zenMode = "Modo Zen"
    override val zenModeDescription = "Sin cronómetro"

    override val back = "← Volver"
    override val newGame = "🔄 Nuevo Juego"
    override val reset = "Reiniciar Sudoku"
    override val moves = "Movimientos"
    override val pencil = "Lápiz"
    override val pencilOn = "Lápiz ON"
    override val pencilOff = "Lápiz OFF"
    override val pencilAuto = "Lápiz AUTO"
    override val notesOn = "Notas ON"
    override val notesOff = "Notas OFF"
    override val erase = "Borrar"
    override val hint = "Pista"
    override val undo = "Deshacer"

    override val notesManual = "Notas ON"
    override val notesAuto = "Notas AUTO"
    override val autoNotesTitle = "Notas automáticas"
    override val autoNotesExplanation = "Mantén pulsada una celda para añadir automáticamente todos los números posibles como notas. A medida que vayas añadiendo números al sudoku, las notas incompatibles se irán borrando automáticamente."
    override val understood = "Entendido"

    override val drawNumber = "Dibuja un número:"
    override val recognize = "Reconocer"
    override val cancel = "Cancelar"

    override val resumeGame= "▶️Continuar partida"
    override val resumeGameMessage = "Tienes una partida guardada. ¿Quieres continuarla?"
    override val congratulations = "🎉 ¡Felicidades!"
    override val completed = "¡Has completado el Sudoku correctamente!"
    override val time = "Tiempo:"
    override val backToMenu = "Volver al menú"
    override val continue_ = "Continuar"
    override val error = "❌ ¡Hay errores!"
    override val errorMessage = "El tablero está completo pero tiene números incorrectos. ¡Revisa las celdas!"
    override val review = "Revisar"
    override val timeUp = "⏰ ¡Tiempo agotado!"
    override val timeUpMessage = "No has podido completar el sudoku a tiempo."

    override val dailySudoku = "Sudoku Diario"
    override val dailySudokuTitle = "Sudoku del día"
    override val todayDifficulty = "Hoy"
    override val alreadyPlayedToday = "¡Ya has jugado el Sudoku de hoy!"
    override val comeBackTomorrow = "Vuelve mañana para un nuevo reto"
    override val currentStreak = "Racha actual"
    override val days = "días"
    override val dailyBestTime = "Mejor tiempo de hoy"
    
    override val gamePaused = "Juego en pausa"
    override val gamePausedMessage = "Pulsa reanudar para continuar jugando"
    override val resume = "Reanudar"

    override val statisticsTitle = "📊 Estadísticas"
    override val gamesCompleted = "Partidas completadas:"
    override val bestTime = "Mejor tiempo:"
    override val bestRemainingTime = "Mejor tiempo restante:"
    override val totalGames = "Total de partidas completadas"

    override val theme = "Tema"
    override val lightTheme = "Claro"
    override val darkTheme = "Oscuro"

    override val settingsTitle = "⚙️ Configuración"
    override val language = "Idioma"
    override val catalan = "Catalán"
    override val spanish = "Castellano"
    override val english = "Inglés"

    override val achievements = "Logros"
    override val achievementsUnlocked = "desbloqueados"
    override val achievementUnlocked = "¡Logro Desbloqueado!"
    override val progress = "Progreso"
    override val awesome = "¡Genial!"

    // Individuals
    override val achievementFirstWin = "Primera Victoria"
    override val achievementFirstWinDesc = "Completa tu primer sudoku"

    override val achievement10Games = "Aficionado"
    override val achievement10GamesDesc = "Completa 10 sudokus"

    override val achievement50Games = "Sudoku Adicto"
    override val achievement50GamesDesc = "Completa 50 sudokus"

    override val achievement100Games = "Maestro Sudoku"
    override val achievement100GamesDesc = "Completa 100 sudokus"

    override val achievementSpeedEasy = "Velocista Fácil"
    override val achievementSpeedEasyDesc = "Completa fácil en menos de 3 minutos"

    override val achievementSpeedHard = "Flash Difícil"
    override val achievementSpeedHardDesc = "Completa difícil quedando menos de 10 minutos"

    override val achievementHardMaster = "Domador de Difíciles"
    override val achievementHardMasterDesc = "Completa 25 sudokus difíciles"

    override val achievementAttackSurvivor = "Supervivencia Extrema"
    override val achievementAttackSurvivorDesc = "Completa modo ataque con menos de 10s"

    override val achievementNoHints = "Mente Pura"
    override val achievementNoHintsDesc = "Completa 5 sudokus sin usar pistas"

    override val achievementNoErrors = "Perfeccionista"
    override val achievementNoErrorsDesc = "Completa 10 sudokus sin errores"

    override val achievementDailyStreak7 = "Racha Diaria"
    override val achievementDailyStreak7Desc = "7 días consecutivos de Sudoku Diario"

    override val achievementDailyPerfect = "Diario Perfecto"
    override val achievementDailyPerfectDesc = "3 Sudokus Diarios sin errores ni pistas"

    override val achievementZenMaster = "Maestro Zen"
    override val achievementZenMasterDesc = "50 sudokus Zen completados"

}

object StringsEn : Strings {
    override val appTitle = "Sudoku E-ink"
    override val normalMode = "Normal Game"
    override val attackMode = "Sudoku Attack"
    override val difficultyEasy = "Level Easy"
    override val difficultyMedium = "Level Medium"
    override val difficultyHard = "Level Hard"
    override val statistics = "📊 Statistics"
    override val settings = "⚙️ Settings"
    override val createdBy = "Created by ktacrack"
    override val zenMode = "Zen Mode"
    override val zenModeDescription = "No timer"

    override val back = "← Back"
    override val newGame = "🔄 New Game"
    override val reset = "Reset Sudoku"
    override val moves = "Movements"
    override val pencil = "Pencil"
    override val pencilOn = "Pencil ON"
    override val pencilOff = "Pencil OFF"
    override val pencilAuto = "Pencil AUTO"
    override val notesOn = "Notes ON"
    override val notesOff = "Notes OFF"
    override val erase = "Erase"
    override val hint = "Hint"
    override val undo = "Undo"

    override val notesManual = "Notes ON"
    override val notesAuto = "Notes AUTO"
    override val autoNotesTitle = "Auto Notes"
    override val autoNotesExplanation = "Long press a cell to automatically add all possible numbers as notes. As you fill in the sudoku, incompatible notes will be automatically cleared."
    override val understood = "Got it"

    override val drawNumber = "Draw a number:"
    override val recognize = "Recognize"
    override val cancel = "Cancel"

    override val resumeGame= "▶️Resume game"
    override val resumeGameMessage = "You have a saved game. Do you want to resume it?"
    override val congratulations = "🎉 Congratulations!"
    override val completed = "You have completed the Sudoku correctly!"
    override val time = "Time:"
    override val backToMenu = "Back to menu"
    override val continue_ = "Continue"
    override val error = "❌ There are errors!"
    override val errorMessage = "The board is complete but has incorrect numbers. Review the cells!"
    override val review = "Review"
    override val timeUp = "⏰ Time's up!"
    override val timeUpMessage = "You couldn't complete the sudoku in time."

    override val dailySudoku = "Daily Sudoku"
    override val dailySudokuTitle = "Today's Sudoku"
    override val todayDifficulty = "Today"
    override val alreadyPlayedToday = "You've already played today's Sudoku!"
    override val comeBackTomorrow = "Come back tomorrow for a new challenge"
    override val currentStreak = "Current streak"
    override val days = "days"
    override val dailyBestTime = "Today's best time"
    
    override val gamePaused = "Game Paused"
    override val gamePausedMessage = "Press resume to continue playing"
    override val resume = "Resume"

    override val statisticsTitle = "📊 Statistics"
    override val gamesCompleted = "Games completed:"
    override val bestTime = "Best time:"
    override val bestRemainingTime = "Best remaining time:"
    override val totalGames = "Total games completed"

    override val theme = "Theme"
    override val lightTheme = "Light"
    override val darkTheme = "Dark"

    override val settingsTitle = "⚙️ Settings"
    override val language = "Language"
    override val catalan = "Catalan"
    override val spanish = "Spanish"
    override val english = "English"

    override val achievements = "Achievements"
    override val achievementsUnlocked = "unlocked"
    override val achievementUnlocked = "Achievement Unlocked!"
    override val progress = "Progress"
    override val awesome = "Awesome!"

    // Individuals
    override val achievementFirstWin = "First Victory"
    override val achievementFirstWinDesc = "Complete your first sudoku"

    override val achievement10Games = "Enthusiast"
    override val achievement10GamesDesc = "Complete 10 sudokus"

    override val achievement50Games = "Sudoku Addict"
    override val achievement50GamesDesc = "Complete 50 sudokus"

    override val achievement100Games = "Sudoku Master"
    override val achievement100GamesDesc = "Complete 100 sudokus"

    override val achievementSpeedEasy = "Easy Speedster"
    override val achievementSpeedEasyDesc = "Complete easy in less than 3 minutes"

    override val achievementSpeedHard = "Hard Flash"
    override val achievementSpeedHardDesc = "Complete hard in less than 10 minutes"

    override val achievementHardMaster = "Hard Tamer"
    override val achievementHardMasterDesc = "Complete 25 hard sudokus"

    override val achievementAttackSurvivor = "Extreme Survivor"
    override val achievementAttackSurvivorDesc = "Complete attack mode with less than 10s left"

    override val achievementNoHints = "Pure Mind"
    override val achievementNoHintsDesc = "Complete 5 sudokus without using hints"

    override val achievementNoErrors = "Perfectionist"
    override val achievementNoErrorsDesc = "Complete 10 sudokus without errors"

    override val achievementDailyStreak7 = "Daily Streak"
    override val achievementDailyStreak7Desc = "7 consecutive days of Daily Sudoku"

    override val achievementDailyPerfect = "Perfect Daily"
    override val achievementDailyPerfectDesc = "3 Daily Sudokus with no errors or hints"

    override val achievementZenMaster = "Zen Master"
    override val achievementZenMasterDesc = "Complete 50 Zen Sudokus"
}

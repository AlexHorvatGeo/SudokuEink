package com.ktacrack.sudokueink

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontStyle


@Composable
fun MainScreen(
    currentMode: String? = null,
    onModeSelected: (String) -> Unit = {},
    onGameModeSelected: (GameMode, Difficulty, Boolean) -> Unit = { _, _, _ -> },  // ← MODIFICAT: afegit Boolean
    onStatisticsClick: () -> Unit = {},
    onDailySudokuClick: () -> Unit = {},
    onAchievementsClick: () -> Unit = {},
    onCalibrationClick: () -> Unit = {},
    onBackToMainMenu: () -> Unit = {},
    onThemeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(LanguageManager.loadLanguage(context)) }
    var isDarkTheme by remember { mutableStateOf(ThemeManager.loadDarkMode(context)) }
    var isZenMode by remember { mutableStateOf(false) }  // ← NOU: estat Mode Zen
    val scale = AdaptiveSizes.getScaleFactor()
    var showAlreadyPlayedDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = currentMode != null) {
        isZenMode = false  // Reset quan tornem
        onBackToMainMenu()
    }

    key(currentLanguage, isDarkTheme, currentMode) {
        val strings = when (currentLanguage) {
            Language.CATALAN -> StringsCa
            Language.SPANISH -> StringsEs
            Language.ENGLISH -> StringsEn
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (currentMode == null) {
                // ✅ SELECTOR IDIOMA/TEMA (sempre dalt dreta)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding((16 * scale).dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Spacer(modifier = Modifier.height((46 * scale).dp))

                    // Botons idioma
                    Row(horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)) {
                        Button(
                            onClick = {
                                currentLanguage = Language.CATALAN
                                LanguageManager.saveLanguage(context, Language.CATALAN)
                            },
                            modifier = Modifier.size((60 * scale).dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentLanguage == Language.CATALAN) Color.Black else Color.White,
                                contentColor = if (currentLanguage == Language.CATALAN) Color.White else Color.Black
                            ),
                            border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("CA", fontSize = (34 * scale).sp)
                        }

                        Button(
                            onClick = {
                                currentLanguage = Language.SPANISH
                                LanguageManager.saveLanguage(context, Language.SPANISH)
                            },
                            modifier = Modifier.size((60 * scale).dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentLanguage == Language.SPANISH) Color.Black else Color.White,
                                contentColor = if (currentLanguage == Language.SPANISH) Color.White else Color.Black
                            ),
                            border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("ES", fontSize = (34 * scale).sp)
                        }

                        Button(
                            onClick = {
                                currentLanguage = Language.ENGLISH
                                LanguageManager.saveLanguage(context, Language.ENGLISH)
                            },
                            modifier = Modifier.size((60 * scale).dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentLanguage == Language.ENGLISH) Color.Black else Color.White,
                                contentColor = if (currentLanguage == Language.ENGLISH) Color.White else Color.Black
                            ),
                            border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("EN", fontSize = (34 * scale).sp)
                        }
                    }

                    Spacer(modifier = Modifier.height((12 * scale).dp))

                    // Botons tema
                    Row(horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)) {
                        Button(
                            onClick = {
                                isDarkTheme = false
                                ThemeManager.saveDarkMode(context, false)
                                onThemeChange(false)
                            },
                            modifier = Modifier.size(width = (80 * scale).dp, height = (50 * scale).dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isDarkTheme) Color.Black else Color.White,
                                contentColor = if (!isDarkTheme) Color.White else Color.Black
                            ),
                            border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("☀️", fontSize = (34 * scale).sp)
                        }

                        Button(
                            onClick = {
                                isDarkTheme = true
                                ThemeManager.saveDarkMode(context, true)
                                onThemeChange(true)
                            },
                            modifier = Modifier.size(width = (80 * scale).dp, height = (50 * scale).dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkTheme) Color.Black else Color.White,
                                contentColor = if (isDarkTheme) Color.White else Color.Black
                            ),
                            border = BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.onBackground),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("🌙", fontSize = (34 * scale).sp)
                        }
                    }
                }

                //  CONTINGUT CENTRAT (títol + botons)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding((16 * scale).dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.appTitle,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = (48 * scale).sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height((40 * scale).dp))

                    Button(
                        onClick = { onModeSelected("NORMAL") },
                        modifier = Modifier
                            .height((70 * scale).dp)
                            .fillMaxWidth(0.8f)
                    ) {
                        Text(strings.normalMode, fontSize = (34 * scale).sp)
                    }

                    Spacer(modifier = Modifier.height((20 * scale).dp))

                    Button(
                        onClick = { onModeSelected("ATTACK") },
                        modifier = Modifier
                            .height((70 * scale).dp)
                            .fillMaxWidth(0.8f)
                    ) {
                        Text(strings.attackMode, fontSize = (34 * scale).sp)
                    }

                    Spacer(modifier = Modifier.height((20 * scale).dp))

                    // Botó Daily
                    Button(
                        onClick = {
                            if (DailySudokuManager.hasPlayedToday(context)) {
                                showAlreadyPlayedDialog =
                                    true  //  Mostra diàleg sense canviar pantalla
                            } else {
                                onDailySudokuClick()  // Navega normalment
                            }
                        },
                        modifier = Modifier
                            //.height((80 * scale).dp)
                            .wrapContentHeight()
                            .fillMaxWidth(0.8f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800),
                            contentColor = Color.White
                    ),
                        border = BorderStroke((2 * scale).dp, Color(0xFFF57C00))
                    ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📅 ${strings.dailySudoku}", fontSize = (34 * scale).sp)
                        Text(
                            text = "${strings.todayDifficulty}: ${DailySudokuManager.getDailyDifficultyName(strings)}",
                            fontSize = (24 * scale).sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                    // Diàleg "Ja has jugat avui"
                    if (showAlreadyPlayedDialog) {
                        AlertDialog(
                            onDismissRequest = { showAlreadyPlayedDialog = false },
                            title = {
                                Text(
                                    text = strings.alreadyPlayedToday,
                                    fontSize = (32 * scale).sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Column {
                                    Text(
                                        text = "${strings.dailyBestTime}: ${formatTime(DailySudokuManager.getDailyBestTime(context))}",
                                        fontSize = (26 * scale).sp
                                    )
                                    Spacer(modifier = Modifier.height((8 * scale).dp))
                                    Text(
                                        text = "🔥 ${strings.currentStreak}: ${DailySudokuManager.getCurrentStreak(context)} ${strings.days}",
                                        fontSize = (24 * scale).sp,
                                        color = Color(0xFFFF9800),
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = (30 * scale).sp
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
                                            showAlreadyPlayedDialog = false
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .wrapContentHeight(),
                                    ) {
                                        Text(strings.back, fontSize = (20 * scale).sp)
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height((32 * scale).dp))

                    Button(
                        onClick = onStatisticsClick,
                        modifier = Modifier
                            .height((70 * scale).dp)
                            .fillMaxWidth(0.8f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        border = BorderStroke((2 * scale).dp, Color.Black)
                    ) {
                        Text(strings.statistics, fontSize = (34 * scale).sp)
                    }

                    Spacer(modifier = Modifier.height((20 * scale).dp))

                    Button(
                        onClick = onAchievementsClick,
                        modifier = Modifier
                            .height((70 * scale).dp)
                            .fillMaxWidth(0.8f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        border = BorderStroke((2 * scale).dp, Color.Black)
                    ) {
                        Text(strings.achievements, fontSize = (34 * scale).sp)
                    }

                    Spacer(modifier = Modifier.height((20 * scale).dp))

                    Button(
                        onClick = onCalibrationClick,
                        modifier = Modifier
                            .height((70 * scale).dp)
                            .fillMaxWidth(0.8f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        border = BorderStroke((2 * scale).dp, Color.Black)
                    ) {
                        Text(strings.calibrate, fontSize = (28 * scale).sp)
                    }
                }

                // ✅ TEXT "CREATED BY" (sempre baix)
                Text(
                    text = strings.createdBy,
                    fontSize = (20 * scale).sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = (8 * scale).dp)
                )


            } else {
                // ✅ SUBMENU DIFICULTATS AMB MODE ZEN
                val gameMode = try {
                    GameMode.valueOf(currentMode)
                } catch (e: Exception) {
                    GameMode.NORMAL
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding((16 * scale).dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height((46 * scale).dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Button(
                            onClick = {
                                isZenMode = false  // Reset Mode Zen quan tornem
                                onBackToMainMenu()
                            },
                            modifier = Modifier
                                .height((50 * scale).dp)
                                .width((160 * scale).dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(strings.back, fontSize = (24 * scale).sp)
                        }
                    }

                    Spacer(modifier = Modifier.weight(0.5f))

                    Text(
                        text = if (gameMode == GameMode.NORMAL) strings.normalMode else strings.attackMode,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = (48 * scale).sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height((40 * scale).dp))

                    // NOU: Toggle Mode Zen
                    if (gameMode == GameMode.NORMAL) {
                        ZenModeToggle(
                            isZenMode = isZenMode,
                            onToggle = { isZenMode = it },
                            scale = scale,
                            strings = strings
                        )

                        Spacer(modifier = Modifier.height((32 * scale).dp))
                    }

                    // Botons de dificultat
                    Button(
                        onClick = { onGameModeSelected(gameMode, Difficulty.EASY, isZenMode) },
                        modifier = Modifier
                            .height((70 * scale).dp)
                            .fillMaxWidth(0.8f)
                    ) {
                        Text(strings.difficultyEasy, fontSize = (34 * scale).sp)
                    }

                    Spacer(modifier = Modifier.height((20 * scale).dp))

                    Button(
                        onClick = { onGameModeSelected(gameMode, Difficulty.MEDIUM, isZenMode) },
                        modifier = Modifier
                            .height((70 * scale).dp)
                            .fillMaxWidth(0.8f)
                    ) {
                        Text(strings.difficultyMedium, fontSize = (34 * scale).sp)
                    }

                    Spacer(modifier = Modifier.height((20 * scale).dp))

                    Button(
                        onClick = { onGameModeSelected(gameMode, Difficulty.HARD, isZenMode) },
                        modifier = Modifier
                            .height((70 * scale).dp)
                            .fillMaxWidth(0.8f)
                    ) {
                        Text(strings.difficultyHard, fontSize = (34 * scale).sp)
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ✨ COMPONENT NOU: Toggle Mode Zen
@Composable
fun ZenModeToggle(
    isZenMode: Boolean,
    onToggle: (Boolean) -> Unit,
    scale: Float,
    strings: Strings
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .height((100 * scale).dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isZenMode)
                Color(0xFFE8F5E9)  // Verd suau quan actiu
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isZenMode)
            BorderStroke((3 * scale).dp, Color(0xFF4CAF50))
        else
            BorderStroke((2 * scale).dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = (20 * scale).dp,    // Esquerra
                    end = (26 * scale).dp,      // Dreta
                    top = (8 * scale).dp,       // Dalt (reduït)
                    bottom = (8 * scale).dp     // Baix (reduït)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🧘 ${strings.zenMode}",
                    fontSize = (28 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isZenMode) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = strings.zenModeDescription,
                    fontSize = (24 * scale).sp,
                    color = if (isZenMode) Color(0xFF388E3C) else Color.Gray
                )
            }

            Switch(
                checked = isZenMode,
                onCheckedChange = onToggle,
                modifier = Modifier.scale(scale * 1.5f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF4CAF50),
                    checkedTrackColor = Color(0xFF81C784),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.LightGray
                )
            )
        }
    }
}

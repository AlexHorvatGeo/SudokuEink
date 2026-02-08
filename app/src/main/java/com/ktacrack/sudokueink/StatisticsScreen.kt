package com.ktacrack.sudokueink

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatisticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val strings = rememberStrings()
    val stats = remember { StatisticsManager.loadStatistics(context) }

    // Factor d'escala adaptatiu
    val scale = AdaptiveSizes.getScaleFactor()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding((16 * scale).dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height((38 * scale).dp))

        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.Start)
                .height((50 * scale).dp)
                .width((160 * scale).dp)
        ) {
            Text(
                strings.back,
                fontSize = (20 * scale).sp
            )
        }

        Spacer(modifier = Modifier.height((36 * scale).dp))

        Text(
            text = strings.statisticsTitle,
            fontSize = (42 * scale).sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height((36 * scale).dp))

        // Estadístiques per dificultat
        StatisticCard(
            difficulty = strings.difficultyEasy,
            gamesCompleted = stats.gamesCompletedEasy,
            bestTime = stats.bestTimeEasy,
            strings = strings,
            scale = scale
        )

        Spacer(modifier = Modifier.height((16 * scale).dp))

        StatisticCard(
            difficulty = strings.difficultyMedium,
            gamesCompleted = stats.gamesCompletedMedium,
            bestTime = stats.bestTimeMedium,
            strings = strings,
            scale = scale
        )

        Spacer(modifier = Modifier.height((16 * scale).dp))

        StatisticCard(
            difficulty = strings.difficultyHard,
            gamesCompleted = stats.gamesCompletedHard,
            bestTime = stats.bestTimeHard,
            strings = strings,
            scale = scale
        )

        Spacer(modifier = Modifier.height((32 * scale).dp))

        // Total de partides
        val totalGames = stats.gamesCompletedEasy +
                stats.gamesCompletedMedium +
                stats.gamesCompletedHard

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding((8 * scale).dp)
                .border((2 * scale).dp, Color.Black),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding((16 * scale).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.totalGames,
                    fontSize = (28 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height((8 * scale).dp))
                Text(
                    text = "$totalGames",
                    fontSize = (26 * scale).sp
                )
            }
        }
    }
}

@Composable
fun StatisticCard(
    difficulty: String,
    gamesCompleted: Int,
    bestTime: Int?,
    strings: Strings,
    scale: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding((8 * scale).dp)
            .border((2 * scale).dp, Color.Black),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding((16 * scale).dp)
        ) {
            Text(
                text = difficulty,
                fontSize = (28 * scale).sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height((12 * scale).dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = strings.gamesCompleted,
                        fontSize = (20 * scale).sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "$gamesCompleted",
                        fontSize = (26 * scale).sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = strings.bestTime,
                        fontSize = (20 * scale).sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = bestTime?.let { formatTime(it) } ?: "--:--",
                        fontSize = (26 * scale).sp
                    )
                }
            }
        }
    }
}

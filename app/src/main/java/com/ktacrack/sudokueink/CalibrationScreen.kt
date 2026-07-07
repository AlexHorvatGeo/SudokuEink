package com.ktacrack.sudokueink

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Un pas de calibració: una seqüència de dígits a escriure en una sola tira,
// d'esquerra a dreta.
private data class CalStep(val labels: List<Int>)

// Passos: la fila sencera 1..9 escrita de cop, CINC vegades, per capturar
// variacions de la teva escriptura. Com que clusterToBitmap retalla i normalitza
// cada dígit per separat, no cal escriure files multi-dígit a part: cada dígit
// ja es captura aïllat, l'escriguis on l'escriguis dins la tira.
private const val REPS_PER_DIGIT = 5
private val CAL_STEPS: List<CalStep> = buildList {
    repeat(REPS_PER_DIGIT) { add(CalStep((1..9).toList())) }
}

// Rasteritza UN grup de traços (un dígit) retallant-lo a la seva pròpia caixa i
// centrant-lo en un llenç quadrat. Imprescindible perquè els dígits s'escriuen
// en una tira ampla: sense retallar, un dígit dibuixat a x alt cauria fora del
// bitmap quadrat de pathsToBitmap. Així cada mostra queda normalitzada igual que
// si s'hagués escrit en una cel·la del joc, independentment de la mida de la tira.
private fun clusterToBitmap(cluster: List<Path>): android.graphics.Bitmap {
    val bounds = StrokeUtils.unionBounds(cluster)
    val base = maxOf(bounds.width, bounds.height).coerceAtLeast(1f)
    val sw = recognitionStrokeWidth(base.toInt())
    val side = base + 2f * sw                       // marge perquè el traç no es talli
    val offsetX = (side - bounds.width) / 2f - bounds.left
    val offsetY = (side - bounds.height) / 2f - bounds.top
    val translated = cluster.map { p ->
        Path().apply { addPath(p); translate(Offset(offsetX, offsetY)) }
    }
    return pathsToBitmap(translated, side.toInt().coerceAtLeast(1), sw)
}

@Composable
fun CalibrationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val strings = rememberStrings()
    val scale = AdaptiveSizes.getScaleFactor()
    val configuration = LocalConfiguration.current
    val recognizer = remember { DigitRecognizer(context) }

    DisposableEffect(Unit) { onDispose { recognizer.close() } }

    var stepIndex by remember { mutableIntStateOf(0) }
    var sampleCount by remember { mutableIntStateOf(CalibrationStore.count(context)) }
    var feedback by remember { mutableStateOf<String?>(null) }

    // Estat de dibuix (mateixa captura que el joc)
    var paths by remember { mutableStateOf(listOf<Path>()) }
    var currentPath by remember { mutableStateOf(Path()) }

    val step = CAL_STEPS.getOrNull(stepIndex)

    fun resetInk() {
        paths = emptyList()
        currentPath = Path()
    }

    // Captura la tinta actual (una mostra per cada dígit escrit) i avança al pas
    // següent en un sol gest. Sempre desem: així les repeticions serveixen de
    // debò i el k-NN del joc té exemples de la teva escriptura.
    fun captureAndAdvance() {
        val s = step ?: return
        if (paths.isEmpty()) { resetInk(); stepIndex++; return }

        val clusters = StrokeUtils.readingOrder(StrokeUtils.clusterByOverlap(paths))

        // Guarda: si el nombre de dígits detectats no coincideix amb l'esperat,
        // no podem emparellar amb seguretat cada traç amb la seva etiqueta.
        // Millor no desar res mal etiquetat: demanem tornar-ho a escriure.
        if (clusters.size != s.labels.size) {
            feedback = "${strings.calibrateRewrite} (${clusters.size}/${s.labels.size})"
            resetInk()
            return
        }

        val newSamples = clusters.zip(s.labels).map { (cluster, label) ->
            val rec = recognizer.recognizeDetailed(clusterToBitmap(cluster))
            CalibrationSample(label, rec.features)
        }
        CalibrationStore.add(context, newSamples)
        sampleCount = CalibrationStore.count(context)
        feedback = null
        resetInk()
        stepIndex++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding((16 * scale).dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height((42 * scale).dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                modifier = Modifier.height((50 * scale).dp).width((140 * scale).dp),
                contentPadding = PaddingValues(0.dp)
            ) { Text(strings.back, fontSize = (22 * scale).sp) }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "${strings.calibrateSamplesStored}: $sampleCount",
                fontSize = (18 * scale).sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height((12 * scale).dp))

        Text(
            strings.calibrateTitle,
            fontSize = (30 * scale).sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height((8 * scale).dp))

        if (step == null) {
            // Fi de la calibració
            Spacer(modifier = Modifier.height((24 * scale).dp))
            Text(
                strings.calibrateIntro,
                fontSize = (20 * scale).sp,
                textAlign = TextAlign.Center,
                lineHeight = (26 * scale).sp
            )
            Spacer(modifier = Modifier.height((24 * scale).dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(0.7f).height((56 * scale).dp)
            ) { Text(strings.calibrateDone, fontSize = (24 * scale).sp) }

            Spacer(modifier = Modifier.height((16 * scale).dp))
            OutlinedButton(
                onClick = {
                    CalibrationStore.clear(context)
                    sampleCount = 0
                    stepIndex = 0
                    resetInk()
                    feedback = null
                },
                modifier = Modifier.fillMaxWidth(0.7f).height((50 * scale).dp)
            ) { Text(strings.calibrateClear, fontSize = (18 * scale).sp) }
        } else {
            // Instrucció: escriu tota la fila de dígits en una sola tira
            Text(
                "${strings.calibrateWriteRow} ${step.labels.joinToString("  ")}",
                fontSize = (24 * scale).sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height((16 * scale).dp))

            // Tira de dibuix ampla: escrius tots els dígits d'esquerra a dreta.
            // La mida NO afecta el reconeixement perquè clusterToBitmap retalla i
            // normalitza cada dígit per separat (com si fos en una cel·la del joc).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((110 * scale).dp)
                    .background(Color.White)
                    .border(BorderStroke((3 * scale).dp, Color.Black))
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .pointerInput(stepIndex) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val newPath = Path().apply { moveTo(down.position.x, down.position.y) }
                            currentPath = newPath
                            down.consume()
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    paths = paths + newPath
                                    currentPath = Path()
                                    change.consume()
                                    break
                                }
                                newPath.lineTo(change.position.x, change.position.y)
                                currentPath = Path().apply { addPath(newPath) }
                                change.consume()
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sw = 6f * scale
                    paths.forEach { drawPath(it, Color.Black, style = Stroke(width = sw)) }
                    drawPath(currentPath, Color.Black, style = Stroke(width = sw))
                }
            }

            Spacer(modifier = Modifier.height((12 * scale).dp))

            // El missatge de "torna-ho a escriure" (recompte de dígits erroni) és
            // un avís, no una confirmació: el mostrem en color d'alerta.
            if (feedback != null) {
                Text(feedback!!, fontSize = (20 * scale).sp, color = Color(0xFFC62828))
            } else {
                Spacer(modifier = Modifier.height((26 * scale).dp))
            }

            Spacer(modifier = Modifier.height((12 * scale).dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)
            ) {
                OutlinedButton(
                    onClick = { resetInk(); feedback = null },
                    modifier = Modifier.weight(1f).height((54 * scale).dp)
                ) { Text(strings.erase, fontSize = (20 * scale).sp) }

                // Un sol botó: desa les mostres i avança. Si no hi ha tinta,
                // simplement salta el pas.
                Button(
                    onClick = { captureAndAdvance() },
                    modifier = Modifier.weight(1f).height((54 * scale).dp)
                ) {
                    Text(
                        "${strings.calibrateNext} (${stepIndex + 1}/${CAL_STEPS.size})",
                        fontSize = (20 * scale).sp
                    )
                }
            }

            // Esborrar totes les mostres desades, disponible en qualsevol pas.
            TextButton(onClick = {
                CalibrationStore.clear(context)
                sampleCount = 0
                feedback = null
                resetInk()
            }) {
                Text(strings.calibrateClear, fontSize = (18 * scale).sp, color = Color(0xFFC62828))
            }
        }
    }
}

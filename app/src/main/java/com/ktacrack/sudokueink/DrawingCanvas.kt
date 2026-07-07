package com.ktacrack.sudokueink

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DrawingCanvas(
    onDigitRecognized: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scaleFactor = AdaptiveSizes.getScaleFactor()
    val strings = rememberStrings()

    var paths by remember { mutableStateOf(listOf<Path>()) }
    var currentPath by remember { mutableStateOf(Path()) }

    val recognizer = remember { DigitRecognizer(context) }

    DisposableEffect(Unit) {
        onDispose {
            recognizer.close()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding((16 * scaleFactor).dp)
    ) {
        Text(
            strings.drawNumber,
            fontSize = (24 * scaleFactor).sp
        )

        Spacer(modifier = Modifier.height((16 * scaleFactor).dp))

        // Canvas de dibuix escalat
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height((300 * scaleFactor).dp)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath = Path().apply {
                                moveTo(offset.x, offset.y)
                            }
                        },
                        onDrag = { change, _ ->
                            currentPath.lineTo(change.position.x, change.position.y)
                        },
                        onDragEnd = {
                            paths = paths + currentPath
                            currentPath = Path()
                        }
                    )
                }
        ) {
            // Fons NEGRE
            drawRect(Color.Black)

            // Calcular gruix del traç escalat
            val strokeWidth = 20f * scaleFactor

            // Dibuixar tots els paths en BLANC
            paths.forEach { path ->
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = strokeWidth)
                )
            }

            // Dibuixar path actual en BLANC
            drawPath(
                path = currentPath,
                color = Color.White,
                style = Stroke(width = strokeWidth)
            )
        }

        Spacer(modifier = Modifier.height((16 * scaleFactor).dp))

        // Botons escalats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
        ) {
            // Botó Esborrar
            Button(
                onClick = {
                    paths = emptyList()
                    currentPath = Path()
                },
                modifier = Modifier
                    .weight(1f)
                    .height((48 * scaleFactor).dp),
                contentPadding = PaddingValues((4 * scaleFactor).dp)
            ) {
                Text(
                    strings.erase,
                    fontSize = (18 * scaleFactor).sp
                )
            }

            // Botó Reconèixer
            Button(
                onClick = {
                    if (paths.isNotEmpty()) {
                        // Convertir canvas a bitmap amb la mida escalada
                        val canvasHeightPx = with(density) { (300 * scaleFactor).dp.toPx().toInt() }
                        val strokeWidth = recognitionStrokeWidth(canvasHeightPx)
                        val bitmap = pathsToBitmap(paths, canvasHeightPx, strokeWidth)
                        val digit = recognizer.recognizeDigit(bitmap)
                        onDigitRecognized(digit)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height((48 * scaleFactor).dp),
                contentPadding = PaddingValues((4 * scaleFactor).dp)
            ) {
                Text(
                    strings.recognize,
                    fontSize = (18 * scaleFactor).sp
                )
            }

            // Botó Cancel·lar
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height((48 * scaleFactor).dp),
                contentPadding = PaddingValues((4 * scaleFactor).dp)
            ) {
                Text(
                    strings.cancel,
                    fontSize = (18 * scaleFactor).sp
                )
            }
        }
    }
}

// Gruix del traç per al bitmap de reconeixement, proporcional a la mida del llenç.
// Clau: el model MNIST és molt sensible a la relació gruix/alçada del dígit. Si
// fixem el gruix en píxels absoluts, una cel·la de joc petita (~100px) el fa
// semblar molt més gruixut que la cel·la gran de calibració (~450px), i en
// reduir-lo a 20px el dígit s'omple i es reconeix malament. Fer-lo proporcional
// fa que el dígit normalitzat es vegi igual a totes les pantalles.
internal fun recognitionStrokeWidth(size: Int): Float = size * 0.06f

internal fun pathsToBitmap(paths: List<Path>, size: Int, strokeWidth: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.BLACK)

    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        this.strokeWidth = strokeWidth
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        isAntiAlias = true
    }

    paths.forEach { path ->
        canvas.drawPath(path.asAndroidPath(), paint)
    }

    return bitmap
}

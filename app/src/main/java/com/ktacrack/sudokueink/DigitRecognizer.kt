package com.ktacrack.sudokueink

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import androidx.core.graphics.scale
import androidx.core.graphics.get
import androidx.core.graphics.createBitmap

class DigitRecognizer(context: Context) {
    private var interpreter: Interpreter
    private val inputSize = 28

    // Mostres de cal·libració de l'usuari (vectors 28x28 etiquetats), carregades un cop.
    private val calibrationSamples: List<CalibrationSample> =
        CalibrationStore.load(context)

    init {
        val model = loadModelFile(context)
        interpreter = Interpreter(model)
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("mnist.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun recognizeDigit(bitmap: Bitmap): Int {
        try {
            // Normalitzar a l'estil MNIST: dígit de ~20px centrat en un llenç 28x28
            val scaledBitmap = centerAndCropDigit(bitmap)

            // Vector de característiques (28x28 en gris 0-255) per al model i el k-NN
            val features = toFeatureVector(scaledBitmap)

            // Array de sortida (10 probabilitats per 0-9)
            val result = Array(1) { FloatArray(10) }
            interpreter.run(featuresToByteBuffer(features), result)

            val sorted = result[0].indices.sortedByDescending { result[0][it] }
            val modelDigit = sorted[0]
            val modelConfidence = result[0][modelDigit]

            // k-NN de cal·libració: només substitueix el model quan aquest NO està
            // segur (baixa confiança) i el dibuix s'assembla molt a una mostra teva.
            if (modelConfidence < CONFIDENCE_THRESHOLD && calibrationSamples.isNotEmpty()) {
                val nearest = calibrationSamples.minByOrNull { distanceSq(features, it.features) }
                if (nearest != null && distanceSq(features, nearest.features) < DISTANCE_THRESHOLD) {
                    return nearest.label
                }
            }
            return modelDigit

        } catch (e: Exception) {
            println("ERROR reconeixement: ${e.message}")
            e.printStackTrace()
            return 0
        }
    }

    // Resultat detallat: usat per la pantalla de cal·libració per decidir si cal
    // capturar una mostra (model incorrecte o poc segur).
    data class Recognition(val digit: Int, val confidence: Float, val features: IntArray)

    fun recognizeDetailed(bitmap: Bitmap): Recognition {
        val scaledBitmap = centerAndCropDigit(bitmap)
        val features = toFeatureVector(scaledBitmap)
        val result = Array(1) { FloatArray(10) }
        interpreter.run(featuresToByteBuffer(features), result)
        val sorted = result[0].indices.sortedByDescending { result[0][it] }
        return Recognition(sorted[0], result[0][sorted[0]], features)
    }

    // Vector de característiques: 28x28 valors de gris (0-255), fila per fila
    private fun toFeatureVector(bitmap: Bitmap): IntArray {
        val out = IntArray(inputSize * inputSize)
        val px = IntArray(inputSize * inputSize)
        bitmap.getPixels(px, 0, inputSize, 0, 0, inputSize, inputSize)
        for (i in px.indices) {
            val p = px[i]
            out[i] = (Color.red(p) + Color.green(p) + Color.blue(p)) / 3
        }
        return out
    }

    private fun featuresToByteBuffer(features: IntArray): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(inputSize * inputSize)
        byteBuffer.order(ByteOrder.nativeOrder())
        for (v in features) byteBuffer.put(v.toByte())
        return byteBuffer
    }

    private fun distanceSq(a: IntArray, b: IntArray): Long {
        var sum = 0L
        for (i in a.indices) {
            val d = (a[i] - b[i]).toLong()
            sum += d * d
        }
        return sum
    }

    companion object {
        // El model es considera "poc segur" per sota d'aquesta confiança
        private const val CONFIDENCE_THRESHOLD = 0.85f
        // Distància màxima (suma de quadrats sobre 784 píxels 0-255) per confiar
        // en una mostra de cal·libració. Conservador: només coincidències molt properes.
        private const val DISTANCE_THRESHOLD = 784L * 90L * 90L
    }

    // Normalitza a l'estil MNIST: retalla el dígit, l'escala perquè el costat més
    // llarg sigui ~20px i el centra (per centre de massa) en un llenç 28x28 negre.
    // Això fa que un dígit gran (sol) i un de petit (en un grup) es vegin iguals.
    private fun centerAndCropDigit(bitmap: Bitmap): Bitmap {
        var minX = bitmap.width
        var maxX = 0
        var minY = bitmap.height
        var maxY = 0

        // Trobar els límits del dígit
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap[x, y]
                val brightness = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                if (brightness > 128) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        // Llenç de sortida 28x28 negre
        val out = createBitmap(inputSize, inputSize)
        val canvas = android.graphics.Canvas(out)
        canvas.drawColor(android.graphics.Color.BLACK)

        if (minX > maxX || minY > maxY) return out  // sense tinta

        val width = maxX - minX + 1
        val height = maxY - minY + 1

        // Escalar el costat més llarg a 20px, mantenint la proporció (marge de 4px)
        val target = 20f
        val factor = target / maxOf(width, height)
        val scaledW = maxOf(1, (width * factor).toInt())
        val scaledH = maxOf(1, (height * factor).toInt())

        val cropped = Bitmap.createBitmap(bitmap, minX, minY, width, height)
        val scaled = cropped.scale(scaledW, scaledH, true)

        // Centre de massa del dígit escalat (convenció MNIST) per situar-lo
        val sx = IntArray(scaledW * scaledH)
        scaled.getPixels(sx, 0, scaledW, 0, 0, scaledW, scaledH)
        var sumX = 0.0; var sumY = 0.0; var mass = 0.0
        for (y in 0 until scaledH) {
            for (x in 0 until scaledW) {
                val p = sx[y * scaledW + x]
                val b = (Color.red(p) + Color.green(p) + Color.blue(p)) / 3.0
                if (b > 30) { sumX += x * b; sumY += y * b; mass += b }
            }
        }
        val comX = if (mass > 0) (sumX / mass).toFloat() else scaledW / 2f
        val comY = if (mass > 0) (sumY / mass).toFloat() else scaledH / 2f

        // Situar de manera que el centre de massa caigui al centre del llenç 28x28
        val left = (inputSize / 2f - comX)
        val top = (inputSize / 2f - comY)
        canvas.drawBitmap(scaled, left, top, null)

        return out
    }

    fun close() {
        interpreter.close()
    }
}

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

    init {
        val model = loadModelFile(context)
        interpreter = Interpreter(model)

        // DEBUG: Veure dimensions
        val inputTensor = interpreter.getInputTensor(0)
        val outputTensor = interpreter.getOutputTensor(0)

        println("Input shape: ${inputTensor.shape().contentToString()}")
        println("Input type: ${inputTensor.dataType()}")
        println("Output shape: ${outputTensor.shape().contentToString()}")
        println("Output type: ${outputTensor.dataType()}")
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

            // Convertir a ByteBuffer
            val byteBuffer = convertBitmapToByteBuffer(scaledBitmap)

            // Array de sortida (10 probabilitats per 0-9)
            val result = Array(1) { FloatArray(10) }

            // Executar inferència
            interpreter.run(byteBuffer, result)

            // Retornar el dígit amb més probabilitat
            val sorted = result[0].indices.sortedByDescending { result[0][it] }
            return sorted[0]

        } catch (e: Exception) {
            println("ERROR reconeixement: ${e.message}")
            e.printStackTrace()
            return 0
        }
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

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        // El model espera UINT8 (0-255), no FLOAT32
        val byteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val pixel = intValues[i * inputSize + j]
                // Convertir a escala de grisos 0-255 (UINT8)
                val gray = ((Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3).toByte()
                byteBuffer.put(gray)
            }
        }

        return byteBuffer
    }


    fun close() {
        interpreter.close()
    }
}

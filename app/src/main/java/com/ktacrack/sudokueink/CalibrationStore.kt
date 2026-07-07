package com.ktacrack.sudokueink

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Una mostra de cal·libració: un dígit dibuixat per l'usuari, normalitzat a un
// vector de 28x28 (784 valors de gris 0-255) i etiquetat amb el dígit real.
@Serializable
data class CalibrationSample(
    val label: Int,
    val features: IntArray
)

object CalibrationStore {
    private const val PREFS_NAME = "sudoku_calibration"
    private const val KEY_SAMPLES = "samples"

    fun load(context: Context): List<CalibrationSample> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SAMPLES, null) ?: return emptyList()
        return try {
            Json.decodeFromString<List<CalibrationSample>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, samples: List<CalibrationSample>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SAMPLES, Json.encodeToString(samples)).apply()
    }

    // Afegeix mostres a les existents
    fun add(context: Context, newSamples: List<CalibrationSample>) {
        if (newSamples.isEmpty()) return
        save(context, load(context) + newSamples)
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SAMPLES).apply()
    }

    fun count(context: Context): Int = load(context).size
}

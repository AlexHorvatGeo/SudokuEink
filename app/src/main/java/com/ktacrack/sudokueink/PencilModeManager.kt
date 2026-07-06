package com.ktacrack.sudokueink

import android.content.Context

object PencilModeManager {
    private const val PREFS_NAME = "sudoku_pencil"
    private const val KEY_PENCIL_MODE = "pencil_mode"

    fun savePencilMode(context: Context, mode: PencilMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PENCIL_MODE, mode.name).apply()
    }

    fun loadPencilMode(context: Context): PencilMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_PENCIL_MODE, PencilMode.OFF.name) ?: PencilMode.OFF.name
        return runCatching { PencilMode.valueOf(name) }.getOrDefault(PencilMode.OFF)
    }
}

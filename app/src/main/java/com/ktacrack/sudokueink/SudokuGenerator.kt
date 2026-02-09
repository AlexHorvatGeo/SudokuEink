package com.ktacrack.sudokueink

import kotlin.random.Random

object SudokuGenerator {

    fun generate(difficulty: Difficulty): SudokuGame {
        // Generem una solució vàlida
        val solution = generateSolution()

        // Determinem quantes caselles deixem buides
        val cellsToRemove = when (difficulty) {
            Difficulty.EASY -> 30
            Difficulty.MEDIUM -> 40
            Difficulty.HARD -> 50
        }

        // Creem el tauler amb caselles buides (amb verificació)
        val board = createPuzzleWithUniqueCheck(solution, cellsToRemove)
        return SudokuGame(board, solution)
    }

    private fun generateSolution(): List<List<Int>> {
        // Solució base vàlida
        val base = listOf(
            listOf(5, 3, 4, 6, 7, 8, 9, 1, 2),
            listOf(6, 7, 2, 1, 9, 5, 3, 4, 8),
            listOf(1, 9, 8, 3, 4, 2, 5, 6, 7),
            listOf(8, 5, 9, 7, 6, 1, 4, 2, 3),
            listOf(4, 2, 6, 8, 5, 3, 7, 9, 1),
            listOf(7, 1, 3, 9, 2, 4, 8, 5, 6),
            listOf(9, 6, 1, 5, 3, 7, 2, 8, 4),
            listOf(2, 8, 7, 4, 1, 9, 6, 3, 5),
            listOf(3, 4, 5, 2, 8, 6, 1, 7, 9)
        )

        // Barregem files dins de cada banda de 3
        val shuffled = base.toMutableList()

        // Barregem files dins de la banda superior (0-2)
        val topBand = shuffled.subList(0, 3).shuffled()
        for (i in 0 until 3) shuffled[i] = topBand[i]

        // Barregem files dins de la banda central (3-5)
        val midBand = shuffled.subList(3, 6).shuffled()
        for (i in 3 until 6) shuffled[i] = midBand[i - 3]

        // Barregem files dins de la banda inferior (6-8)
        val botBand = shuffled.subList(6, 9).shuffled()
        for (i in 6 until 9) shuffled[i] = botBand[i - 6]

        // Barregem les bandes completes
        val bands = listOf(
            shuffled.subList(0, 3),
            shuffled.subList(3, 6),
            shuffled.subList(6, 9)
        ).shuffled()

        return bands.flatten()
    }

    // ✅ NOVA FUNCIÓ: Crea puzzle verificant que tingui solució única
    private fun createPuzzleWithUniqueCheck(
        solution: List<List<Int>>,
        cellsToRemove: Int
    ): List<List<SudokuCell>> {
        val board = solution.map { row ->
            row.map { value ->
                SudokuCell(value = value, isFixed = true)
            }.toMutableList()
        }.toMutableList()

        // Llista de totes les posicions
        val positions = (0 until 81).shuffled().toMutableList()
        var removed = 0
        var attemptIndex = 0

        while (removed < cellsToRemove && attemptIndex < positions.size) {
            val pos = positions[attemptIndex]
            val row = pos / 9
            val col = pos % 9

            // Guardar valor abans de treure'l
            val backup = board[row][col].value

            if (backup != 0) {
                // Intentar treure el número
                board[row][col] = SudokuCell(value = 0, isFixed = false)

                // ✅ COMPROVACIÓ CLAU: Verificar que només hi ha 1 solució
                if (hasUniqueSolution(board, solution)) {
                    removed++
                } else {
                    // Si té múltiples solucions, tornar a posar el número
                    board[row][col] = SudokuCell(value = backup, isFixed = true)
                }
            }

            attemptIndex++
        }

        // Convertim a llistes immutables
        return board.map { it.toList() }
    }

    // ✅ NOVA FUNCIÓ: Comprova si el puzzle té solució única
    private fun hasUniqueSolution(
        board: List<List<SudokuCell>>,
        expectedSolution: List<List<Int>>
    ): Boolean {
        // Convertir a format de solver
        val puzzle = board.map { row ->
            row.map { cell -> cell.value }.toMutableList()
        }.toMutableList()

        // Comptar solucions (màxim 2 per eficiència)
        val solutionCount = countSolutions(puzzle, 0, 0, 0)
        return solutionCount == 1
    }

    // ✅ NOVA FUNCIÓ: Compta quantes solucions té un puzzle (màxim 2)
    private fun countSolutions(
        board: List<MutableList<Int>>,
        row: Int,
        col: Int,
        count: Int
    ): Int {
        // Si ja hem trobat 2 solucions, parar (no cal seguir)
        if (count >= 2) return count

        // Trobar la següent casella buida
        var r = row
        var c = col
        var found = false

        outer@ for (i in r until 9) {
            for (j in (if (i == r) c else 0) until 9) {
                if (board[i][j] == 0) {
                    r = i
                    c = j
                    found = true
                    break@outer
                }
            }
        }

        // Si no hi ha més caselles buides, hem trobat una solució
        if (!found) return count + 1

        // Provar tots els números de 1 a 9
        var currentCount = count
        for (num in 1..9) {
            if (isValidMove(board, r, c, num)) {
                board[r][c] = num
                currentCount = countSolutions(board, r, c + 1, currentCount)
                board[r][c] = 0

                // Si ja hem trobat 2 solucions, parar
                if (currentCount >= 2) break
            }
        }

        return currentCount
    }

    // ✅ NOVA FUNCIÓ: Comprova si un número és vàlid en una posició
    private fun isValidMove(
        board: List<List<Int>>,
        row: Int,
        col: Int,
        num: Int
    ): Boolean {
        // Comprovar fila
        if (board[row].contains(num)) return false

        // Comprovar columna
        if ((0 until 9).any { board[it][col] == num }) return false

        // Comprovar caixa 3x3
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxCol until boxCol + 3) {
                if (board[r][c] == num) return false
            }
        }

        return true
    }
}

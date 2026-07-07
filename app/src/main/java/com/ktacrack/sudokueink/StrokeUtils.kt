package com.ktacrack.sudokueink

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path

// Utilitats de segmentació de traços compartides entre el joc i la calibració.
object StrokeUtils {

    // Combina les caixes d'un grup de traços en una de sola
    fun unionBounds(cluster: List<Path>): Rect =
        cluster.map { it.getBounds() }.reduce { a, b ->
            Rect(
                minOf(a.left, b.left), minOf(a.top, b.top),
                maxOf(a.right, b.right), maxOf(a.bottom, b.bottom)
            )
        }

    // Agrupa els traços per solapament 2D (union-find): dos traços van al mateix
    // grup si les seves caixes es creuen en X i en Y. Transitiu, així els traços
    // d'un dígit es fusionen estiguin on estiguin dins la cel·la.
    fun clusterByOverlap(paths: List<Path>): List<List<Path>> {
        if (paths.isEmpty()) return emptyList()
        val boxes = paths.map { it.getBounds() }
        val parent = IntArray(paths.size) { it }

        fun find(x: Int): Int {
            var r = x
            while (parent[r] != r) r = parent[r]
            var c = x
            while (parent[c] != c) { val n = parent[c]; parent[c] = r; c = n }
            return r
        }

        for (i in boxes.indices) {
            for (j in i + 1 until boxes.size) {
                val a = boxes[i]; val b = boxes[j]
                val xOverlap = minOf(a.right, b.right) - maxOf(a.left, b.left)
                val yOverlap = minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)
                if (xOverlap > 0f && yOverlap > 0f) parent[find(i)] = find(j)
            }
        }

        return paths.indices.groupBy { find(it) }.values
            .map { idxs -> idxs.map { paths[it] } }
    }

    // Ordena els grups en ordre de lectura: files de dalt a baix, i dins de cada
    // fila d'esquerra a dreta. Dos grups són a la mateixa fila si les seves caixes
    // se solapen verticalment. Coincideix amb com la persona escriu el patró.
    fun readingOrder(clusters: List<List<Path>>): List<List<Path>> {
        if (clusters.isEmpty()) return clusters
        val withBounds = clusters.map { it to unionBounds(it) }.sortedBy { it.second.top }

        val rows = mutableListOf<MutableList<Pair<List<Path>, Rect>>>()
        for (item in withBounds) {
            val row = rows.firstOrNull { r ->
                val rb = r.first().second
                val yOverlap = minOf(rb.bottom, item.second.bottom) - maxOf(rb.top, item.second.top)
                yOverlap > 0f
            }
            if (row != null) row.add(item) else rows.add(mutableListOf(item))
        }

        return rows.flatMap { row -> row.sortedBy { it.second.left }.map { it.first } }
    }
}

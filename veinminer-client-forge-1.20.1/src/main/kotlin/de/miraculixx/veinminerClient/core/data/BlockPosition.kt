package de.miraculixx.veinminerClient.core.data

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable
data class BlockPosition(val x: Int, val y: Int, val z: Int) {
    fun distance(other: BlockPosition): Double {
        val dx = (x - other.x).toDouble()
        val dy = (y - other.y).toDouble()
        val dz = (z - other.z).toDouble()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}

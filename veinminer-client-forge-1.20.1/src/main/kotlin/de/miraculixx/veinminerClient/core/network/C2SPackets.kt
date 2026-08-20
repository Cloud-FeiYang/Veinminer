package de.miraculixx.veinminerClient.core.network

import de.miraculixx.veinminerClient.core.pattern.PatternConfig
import de.miraculixx.veinminerClient.core.pattern.Surface
import kotlinx.serialization.Serializable

@Serializable
data class JoinInformation(
    val veinminerClientVersion: String,
)

@Serializable
data class KeyPress(
    val pressed: Boolean,
    val maxDepth: Int = UNLIMITED_DEPTH,
    val surface: Surface = Surface.UP,
    val patternId: String? = null,
) {
    companion object {
        const val UNLIMITED_DEPTH: Int = Int.MAX_VALUE
    }
}

@Serializable
data class ClientPatternSync(
    val patterns: List<PatternConfig>,
)

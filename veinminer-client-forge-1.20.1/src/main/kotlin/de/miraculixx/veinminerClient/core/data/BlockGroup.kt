package de.miraculixx.veinminerClient.core.data

import kotlinx.serialization.Serializable

@Serializable
data class BlockGroup<T>(
    val name: String,
    val blocks: MutableSet<T>,
    val tools: MutableSet<T> = mutableSetOf(),
    val override: VeinminerSettingsOverride? = null
)

data class FixedBlockGroup<T>(
    val blocks: Set<T>,
    val tools: Set<T>,
    val override: VeinminerSettingsOverride? = null
)

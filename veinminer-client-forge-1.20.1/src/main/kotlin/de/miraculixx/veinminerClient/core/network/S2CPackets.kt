package de.miraculixx.veinminerClient.core.network

import de.miraculixx.veinminerClient.core.data.BlockGroup
import de.miraculixx.veinminerClient.core.data.VeinminerSettings
import kotlinx.serialization.Serializable

@Serializable
data class ServerConfiguration(
    val outdated: Boolean,
    val settings: VeinminerSettings,
    val groups: List<BlockGroup<String>>,
    val veinBlocks: List<String>,
    val enchantmentActive: Boolean,
    val enchantmentKey: String?,
    val hostActive: Boolean,
    val hasUsePermission: Boolean,
)

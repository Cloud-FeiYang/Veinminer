package de.miraculixx.veinminer.config

import de.miraculixx.veinminer.data.BlockGroup
import de.miraculixx.veinminer.data.VeinminerSettings

interface ConfigBridge {
    val settings: VeinminerSettings
    val veinBlocksRaw: MutableSet<String>
    val groupsRaw: MutableSet<BlockGroup<String>>
    fun reload(fromDisc: Boolean)
    fun save()
}

enum class MaterialType {
    BLOCK,
    ITEM
}

data class ParsedData<T>(
    val parsed: Set<T>,
    val invalid: Set<String>
)

interface ConfigSerializer<T> {
    fun parseList(rawList: Set<String>, type: MaterialType): ParsedData<T>
}

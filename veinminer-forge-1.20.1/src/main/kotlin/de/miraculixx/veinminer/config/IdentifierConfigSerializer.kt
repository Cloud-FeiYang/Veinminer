package de.miraculixx.veinminer.config

import de.miraculixx.veinminer.command.ActiveHost
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraftforge.registries.ForgeRegistries

object IdentifierConfigSerializer : ConfigSerializer<ResourceLocation> {

    override fun parseList(rawList: Set<String>, type: MaterialType): ParsedData<ResourceLocation> {
        val parsed = mutableSetOf<ResourceLocation>()
        val invalid = mutableSetOf<String>()
        val logger = ActiveHost.host.logger

        rawList.forEach { raw ->
            val entries = parseEntry(raw, type)
            if (entries == null) {
                logger.warn("Failed to access registry! Cannot parse config entry: $raw")
            } else if (entries.isEmpty()) {
                invalid.add(raw)
                logger.warn("Invalid ${type.name.lowercase()} entry in config: $raw")
            } else {
                parsed.addAll(entries)
            }
        }
        return ParsedData(parsed, invalid)
    }

    private fun parseEntry(raw: String, type: MaterialType): Set<ResourceLocation>? {
        val isTag = raw.startsWith("#")
        val name = if (isTag) raw.substring(1) else raw
        val key = ResourceLocation.tryParse(name) ?: return emptySet()

        return if (isTag) {
            when (type) {
                MaterialType.ITEM -> {
                    val tagKey = TagKey.create(Registries.ITEM, key)
                    ForgeRegistries.ITEMS.tags()?.getTag(tagKey)
                        ?.mapNotNull { ForgeRegistries.ITEMS.getKey(it) }
                        ?.toSet()
                        ?: emptySet()
                }
                MaterialType.BLOCK -> {
                    val tagKey = TagKey.create(Registries.BLOCK, key)
                    ForgeRegistries.BLOCKS.tags()?.getTag(tagKey)
                        ?.mapNotNull { ForgeRegistries.BLOCKS.getKey(it) }
                        ?.toSet()
                        ?: emptySet()
                }
            }
        } else {
            val exists = when (type) {
                MaterialType.BLOCK -> ForgeRegistries.BLOCKS.containsKey(key)
                MaterialType.ITEM -> ForgeRegistries.ITEMS.containsKey(key)
            }
            if (exists) setOf(key) else emptySet()
        }
    }
}

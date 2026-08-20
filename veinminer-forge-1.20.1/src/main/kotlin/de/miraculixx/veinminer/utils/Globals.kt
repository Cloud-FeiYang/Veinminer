package de.miraculixx.veinminer.utils

import de.miraculixx.veinminer.config.ConfigBridge
import kotlinx.serialization.json.Json

const val cRed = 0xff5555
const val cGreen = 0x55ff55
const val cBase = 0xaaaaaa
const val cHighlight = 0x5CA0D4
const val cWhite = 0xffffff
const val cGold = 0xffaa00

const val permissionToggle = "veinminer.toggle"
const val permissionBlocks = "veinminer.blocks"
const val permissionSettings = "veinminer.settings"
const val permissionVeinmine = "veinminer.use"
const val permissionGroups = "veinminer.groups"
const val permissionReload = "veinminer.reload"

const val IDENTIFIER = "veinminer"

object ActiveConfig {
    @Volatile
    lateinit var bridge: ConfigBridge
}

val debug: Boolean
    get() = runCatching { ActiveConfig.bridge.settings.debug }.getOrNull() ?: false

val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

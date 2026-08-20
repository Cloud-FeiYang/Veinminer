package de.miraculixx.veinminer.extensions

import com.mojang.logging.LogUtils
import de.miraculixx.veinminer.utils.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

inline fun <reified T> Path.load(default: T, instance: Json = json): T {
    val logger = LogUtils.getLogger()
    return if (!exists()) {
        createParentDirectories()
        val string = instance.encodeToString(default)
        writeText(string)
        logger.info("Created ${this.fileName} default config")
        default
    } else {
        try {
            instance.decodeFromString<T>(readText())
        } catch (e: Exception) {
            logger.warn("Failed to load ${this.fileName} config: Reason: ${e.message}")
            default
        }
    }
}

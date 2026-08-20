package de.miraculixx.veinminer.command

import de.miraculixx.veinminer.Veinminer
import net.minecraft.DetectedVersion
import org.slf4j.Logger

object ForgeHost : VeinminerHost {
    override val versionVeinminer: String
        get() = Veinminer.VERSION
    override val versionMinecraft: String = DetectedVersion.tryDetectVersion().name
    override val platform: String = "Forge"
    override val logger: Logger = Veinminer.LOGGER

    override var active: Boolean
        get() = Veinminer.active
        set(value) {
            Veinminer.active = value
        }
}

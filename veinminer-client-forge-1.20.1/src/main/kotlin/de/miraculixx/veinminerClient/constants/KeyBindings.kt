package de.miraculixx.veinminerClient.constants

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import org.lwjgl.glfw.GLFW

object KeyBindings {
    var hold: KeyMapping? = null
    var toggle: KeyMapping? = null
    var config: KeyMapping? = null
}

object ForgeKeyBindings {
    fun register(event: RegisterKeyMappingsEvent) {
        val category = "key.categories.veinminer"
        val hold = KeyMapping("key.veinminer.hold", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, category)
        val toggle = KeyMapping("key.veinminer.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category)
        val config = KeyMapping("key.veinminer.config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category)

        event.register(hold)
        event.register(toggle)
        event.register(config)

        KeyBindings.hold = hold
        KeyBindings.toggle = toggle
        KeyBindings.config = config
    }
}

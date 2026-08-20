package de.miraculixx.veinminerClient.render

import de.miraculixx.veinminerClient.ClientLifecycle
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.client.gui.overlay.ForgeGui
import net.minecraftforge.client.gui.overlay.IGuiOverlay

object HUDProvider {
    lateinit var instance: HUDRenderer
}

abstract class HUDRenderer {
    private val AXE_ICON = icon("axe")
    private val PICKAXE_ICON = icon("pickaxe")
    private val SHOVEL_ICON = icon("shovel")
    private val HOE_ICON = icon("hoe")
    private val FORBIDDEN_ICON = icon("forbidden")
    private var target: ResourceLocation? = null

    fun renderCrosshair(graphics: GuiGraphics) {
        val tex = target ?: return

        val client = Minecraft.getInstance()
        val window = client.window

        graphics.blit(tex, (window.guiScaledWidth / 2) + 2, (window.guiScaledHeight / 2) - 10, 0f, 0f, 8, 8, 8, 8)
    }

    fun updateTarget(target: String?) {
        this.target = when (target) {
            null -> null
            "axe" -> AXE_ICON
            "shovel" -> SHOVEL_ICON
            "hoe" -> HOE_ICON
            "forbidden" -> FORBIDDEN_ICON
            else -> PICKAXE_ICON
        }
    }

    private fun icon(tool: String) = ResourceLocation(ClientLifecycle.MOD_ID, "textures/gui/sprites/tooltip/${tool}.png")
}

object ForgeHUDRenderer : HUDRenderer(), IGuiOverlay {
    override fun render(gui: ForgeGui, graphics: GuiGraphics, partialTick: Float, width: Int, height: Int) {
        renderCrosshair(graphics)
    }
}

package de.miraculixx.veinminer.command

import com.mojang.brigadier.CommandDispatcher
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.utils.ActiveConfig
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack

object ForgeVeinminerCommand {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, ctx: CommandBuildContext) {
        ActiveConfig.bridge = ConfigManager
        Permissions.install { src, _ ->
            val css = src as? CommandSourceStack ?: return@install false
            css.hasPermission(2)
        }
        dispatcher.register(VeinminerCommand.build(ctx))
    }
}

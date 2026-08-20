package de.miraculixx.veinminer.network

import de.miraculixx.veinminer.command.ActiveHost
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.event.EventState
import de.miraculixx.veinminer.utils.mcServer
import de.miraculixx.veinminer.utils.permissionVeinmine
import java.util.UUID

object ServerCallbacksImpl : ServerCallbacks {
    private val logger get() = ActiveHost.host.logger

    override fun onJoinAccepted(playerId: UUID, packet: JoinInformation) {
        val server = mcServer ?: return logger.warn("Not enough information to handle 'join (no server)' packet!")
        val settings = ConfigManager.settings
        if (!settings.client.allow) return

        NetworkRouter.registeredPlayers[playerId] = packet.veinminerClientVersion

        val player = server.playerList.getPlayer(playerId)
        val name = player?.scoreboardName ?: playerId.toString()
        logger.info("$name joined with Veinminer version ${packet.veinminerClientVersion}")

        val conf = ServerConfiguration(
            outdated = false,
            settings = settings,
            groups = ConfigManager.networkGroups,
            veinBlocks = ConfigManager.networkVeinBlocks,
            enchantmentActive = EventState.enchantmentActive,
            enchantmentKey = EventState.enchantmentKey.location().toString(),
            hostActive = ActiveHost.host.active,
            hasUsePermission = if (player != null) EventState.checkPermission(player, permissionVeinmine) else true,
        )
        NetworkRouter.sendConfiguration(playerId, conf)
    }

    override fun onKeyPress(playerId: UUID, packet: KeyPress) {
        if (ConfigManager.settings.debug) logger.info("$playerId pressed hotkey (${packet.pressed})")
    }

    override fun onPatterns(playerId: UUID, packet: ClientPatternSync) {
        if (ConfigManager.settings.debug) logger.info("$playerId sent ${packet.patterns.size} pattern configs")
    }
}

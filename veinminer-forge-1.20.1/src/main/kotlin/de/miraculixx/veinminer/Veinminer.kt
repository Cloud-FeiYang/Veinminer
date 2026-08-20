package de.miraculixx.veinminer

import com.mojang.logging.LogUtils
import de.miraculixx.veinminer.command.ActiveHost
import de.miraculixx.veinminer.command.ForgeHost
import de.miraculixx.veinminer.command.ForgeVeinminerCommand
import de.miraculixx.veinminer.command.VeinminerCommand
import de.miraculixx.veinminer.config.ConfigManager
import de.miraculixx.veinminer.event.EventState
import de.miraculixx.veinminer.event.VeinMinerEvent
import de.miraculixx.veinminer.network.ForgePlatformNetwork
import de.miraculixx.veinminer.network.NetworkRouter
import de.miraculixx.veinminer.network.ServerCallbacksImpl
import de.miraculixx.veinminer.network.ServerConfiguration
import de.miraculixx.veinminer.utils.cGreen
import de.miraculixx.veinminer.utils.cHighlight
import de.miraculixx.veinminer.utils.cRed
import de.miraculixx.veinminer.utils.mcServer
import de.miraculixx.veinminer.utils.permissionVeinmine
import net.minecraft.DetectedVersion
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import org.slf4j.Logger

@Mod(Veinminer.MOD_ID)
class Veinminer {
    companion object {
        const val MOD_ID = "veinminer"
        val LOGGER: Logger = LogUtils.getLogger()
        var VERSION: String = "unknown"
        var active = true
        var updateInfo: UpdateManager.VersionInfo? = null
    }

    init {
        VERSION = ModLoadingContext.get().activeContainer.modInfo.version.toString()
        LOGGER.info("Veinminer Version: $VERSION (forge 1.20.1)")
        val mcVersion = DetectedVersion.tryDetectVersion().name

        EventState.enchantmentActive = ModList.get().isLoaded("veinminer_enchantment")

        ActiveHost.host = ForgeHost
        EventState.configManager = ConfigManager
        EventState.checkPermission = { _, _ -> true }
        EventState.dropBlockExperience = { state, level, blockPos, blockEntity, breaker, tool, dropPos ->
            state.spawnAfterBreak(level, dropPos, tool, false)
            val fortune = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE, tool
            )
            val silkTouch = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH, tool
            )
            val exp = state.getExpDrop(level, level.random, blockPos, fortune, silkTouch)
            if (exp > 0) ExperienceOrb.award(level, Vec3.atCenterOf(dropPos), exp)
        }

        val gameBus = MinecraftForge.EVENT_BUS

        ForgePlatformNetwork.init()
        NetworkRouter.init(ForgePlatformNetwork, ServerCallbacksImpl)

        gameBus.addListener { event: BlockEvent.BreakEvent ->
            val player = event.player
            val world = player.level()
            if (world.isClientSide) return@addListener
            val proceed = VeinMinerEvent.onBlockBreakBefore(world, player, event.pos, event.state)
            if (!proceed) event.isCanceled = true
        }

        gameBus.addListener { event: RegisterCommandsEvent ->
            ForgeVeinminerCommand.register(event.dispatcher, event.buildContext)
        }

        gameBus.addListener { event: ServerStartingEvent ->
            mcServer = event.server
            ConfigManager.reload(true)
        }
        gameBus.addListener { _: ServerStoppedEvent ->
            mcServer = null
        }

        gameBus.addListener { event: PlayerEvent.PlayerLoggedOutEvent ->
            val player = event.entity
            NetworkRouter.onDisconnect(player.uuid)
        }

        gameBus.addListener { event: PlayerEvent.PlayerLoggedInEvent ->
            val player = event.entity as? ServerPlayer ?: return@addListener
            if (NetworkRouter.registeredPlayers.containsKey(player.uuid)) {
                val conf = ServerConfiguration(
                    outdated = false,
                    settings = ConfigManager.settings,
                    groups = ConfigManager.networkGroups,
                    veinBlocks = ConfigManager.networkVeinBlocks,
                    enchantmentActive = EventState.enchantmentActive,
                    enchantmentKey = EventState.enchantmentKey.location().toString(),
                    hostActive = ActiveHost.host.active,
                    hasUsePermission = EventState.checkPermission(player, permissionVeinmine),
                )
                NetworkRouter.sendConfiguration(player.uuid, conf)
            }
            val server = player.server
            val isOp = server.playerList.isOp(player.gameProfile)
            val canConfigure = isOp || !server.isDedicatedServer
            if (ConfigManager.firstInstall && canConfigure) {
                player.sendSystemMessage(
                    Component.literal("\nVeinminer was installed for the first time.")
                        .append("\n• By default only ores are veinmine-able")
                        .append("\n• Click ").append(
                            Component.literal("here").setStyle(
                                Style.EMPTY.withColor(TextColor.fromRgb(cHighlight))
                            )
                        )
                        .append(" to see how to configure more")
                        .withStyle { it.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/project/veinminer#config")) }
                )
            }

            val info = updateInfo ?: return@addListener
            if (canConfigure) {
                player.sendSystemMessage(
                    Component.literal("${info.module.modID} is outdated! ")
                        .append(" (Current: ")
                        .append(Component.literal(info.currentVersion).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(cRed))))
                        .append(", Latest: ")
                        .append(Component.literal(info.latestVersion).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(cGreen))))
                        .append(")")
                        .append("\nDownload: ").append(VeinminerCommand.link("Modrinth", "https://modrinth.com/mod/${info.module.modID}"))
                        .append(" | ").append(VeinminerCommand.link("CurseForge", "https://www.curseforge.com/minecraft/mc-mods/${info.module.cfID}"))
                )
            }
        }

        UpdateManager.startUpdateChecker(
            modules = listOf(UpdateManager.Module.VEINMINER, UpdateManager.Module.VEINMINER_CLIENT),
            platform = "forge",
            serverVersion = mcVersion,
            moduleVersionLookup = { ModList.get().getModContainerById(it.modID).orElse(null)?.modInfo?.version?.toString() },
        ) { info -> updateInfo = info }
    }
}

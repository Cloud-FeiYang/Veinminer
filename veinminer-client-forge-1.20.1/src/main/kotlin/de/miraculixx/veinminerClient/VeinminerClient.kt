package de.miraculixx.veinminerClient

import com.mojang.blaze3d.platform.InputConstants
import de.miraculixx.veinminer.extensions.mcCoroutineAsync
import de.miraculixx.veinminer.extensions.ticks
import de.miraculixx.veinminerClient.ClientLifecycle.MOD_ID
import de.miraculixx.veinminerClient.config.ClientPatternConfig
import de.miraculixx.veinminerClient.config.PatternConfigScreen
import de.miraculixx.veinminerClient.constants.ForgeKeyBindings
import de.miraculixx.veinminerClient.network.ForgeClientPlatformNetwork
import de.miraculixx.veinminerClient.network.NetworkManager
import de.miraculixx.veinminerClient.render.BlockHighlightingRenderer
import de.miraculixx.veinminerClient.render.ForgeHUDRenderer
import de.miraculixx.veinminerClient.render.ForgeShapeRouletteRenderer
import de.miraculixx.veinminerClient.render.HUDProvider
import net.minecraft.DetectedVersion
import net.minecraft.client.Minecraft
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.ConfigScreenHandler
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.client.event.InputEvent
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.fml.loading.FMLPaths

@Mod(MOD_ID)
class VeinminerClient {

    init {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT) {
            Runnable {
                val modBus = FMLJavaModLoadingContext.get().modEventBus
                val gameBus = MinecraftForge.EVENT_BUS
                val container = ModLoadingContext.get().activeContainer

                ClientLifecycle.veinminerAvailable = ModList.get().isLoaded("veinminer")

                HUDProvider.instance = ForgeHUDRenderer
                ClientPatternConfig.configure(FMLPaths.CONFIGDIR.get())
                ClientPatternConfig.load()
                NetworkManager.selectedPattern = ClientPatternConfig.enabledPatterns().first()

                ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory::class.java) {
                    ConfigScreenHandler.ConfigScreenFactory { _, parent -> PatternConfigScreen(parent) }
                }

                NetworkManager.init(ForgeClientPlatformNetwork)

                modBus.addListener { event: RegisterKeyMappingsEvent ->
                    ForgeKeyBindings.register(event)
                }

                modBus.addListener { event: RegisterGuiOverlaysEvent ->
                    event.registerAboveAll("target-info", ForgeHUDRenderer)
                    event.registerAboveAll("shape-roulette", ForgeShapeRouletteRenderer)
                }

                gameBus.addListener { event: TickEvent.ClientTickEvent ->
                    if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().level != null) {
                        KeyBindManager.tick()
                    }
                }

                gameBus.addListener { event: InputEvent.MouseScrollingEvent ->
                    if (!KeyBindManager.isPressed) return@addListener
                    if (!NetworkManager.isVeinminerActive) return@addListener
                    val v = event.scrollDelta
                    if (v == 0.0) return@addListener
                    val w = Minecraft.getInstance().window
                    val shift = InputConstants.isKeyDown(w.window, InputConstants.KEY_LSHIFT)
                        || InputConstants.isKeyDown(w.window, InputConstants.KEY_RSHIFT)
                    KeyBindManager.queueScroll(if (v > 0) 1 else -1, shift)
                    event.isCanceled = true
                }

                gameBus.addListener { _: ClientPlayerNetworkEvent.LoggingIn ->
                    ClientLifecycle.onJoin(Minecraft.getInstance(), container.modInfo.version.toString())
                }

                gameBus.addListener { _: ClientPlayerNetworkEvent.LoggingOut ->
                    ClientLifecycle.onDisconnect()
                }

                gameBus.addListener { event: RenderLevelStageEvent ->
                    if (event.stage != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return@addListener
                    val source = Minecraft.getInstance().renderBuffers().bufferSource()
                    val cameraPosition = event.camera.position
                    BlockHighlightingRenderer.render(event.poseStack, source, cameraPosition, false)
                    BlockHighlightingRenderer.render(event.poseStack, source, cameraPosition, true)
                }

                mcCoroutineAsync(1.ticks) {
                    ClientLifecycle.checkForUpdates(
                        "forge",
                        DetectedVersion.tryDetectVersion().name,
                        ModList.get().getModContainerById(MOD_ID).orElse(null)?.modInfo?.version?.toString()
                    )
                }
            }
        }
    }
}

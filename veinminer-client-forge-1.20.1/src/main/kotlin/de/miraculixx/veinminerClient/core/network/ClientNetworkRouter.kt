package de.miraculixx.veinminerClient.core.network

import com.mojang.logging.LogUtils
import org.slf4j.Logger
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface ClientPlatformNetwork {
    fun registerC2S(channel: String)
    fun registerS2C(channel: String, handler: (payload: ByteArray) -> Unit)
    fun sendC2S(channel: String, payload: ByteArray)
}

object ClientNetworkRouter {
    @Volatile
    private var platform: ClientPlatformNetwork? = null

    private val logger: Logger = LogUtils.getLogger()

    @Volatile
    private var loopbackPredicate: () -> Boolean = { false }

    @Volatile
    private var localPlayerId: () -> UUID? = { null }

    private val clientboundHandlers: MutableMap<String, (ByteArray) -> Unit> = ConcurrentHashMap()

    fun init(
        platform: ClientPlatformNetwork,
        callbacks: ClientCallbacks,
        loopbackPredicate: () -> Boolean,
        localPlayerId: () -> UUID?
    ) {
        this.platform = platform
        this.loopbackPredicate = loopbackPredicate
        this.localPlayerId = localPlayerId

        platform.registerC2S(NetworkManager.PACKET_JOIN_ID)
        platform.registerC2S(NetworkManager.PACKET_PATTERNS_ID)
        platform.registerC2S(NetworkManager.PACKET_KEY_PRESS_ID)

        platform.registerS2C(NetworkManager.PACKET_CONFIGURATION_ID) { bytes ->
            try {
                val config = PacketCodecs.CONFIGURATION.decode(bytes)
                logger.info("Successfully decoded ServerConfiguration: groups=${config.groups.size}, veinBlocks=${config.veinBlocks.size}")
                callbacks.onConfiguration(config)
            } catch (e: Exception) {
                logger.error("Failed to decode S2C 'configuration' (${bytes.size} bytes): ${e.message}", e)
            }
        }
    }

    fun registerClientboundHandler(channel: String, handler: (ByteArray) -> Unit) {
        clientboundHandlers[channel] = handler
    }

    fun dispatchClientbound(channel: String, payload: ByteArray) {
        clientboundHandlers[channel]?.invoke(payload)
    }

    fun sendJoin(version: String) {
        send(NetworkManager.PACKET_JOIN_ID, PacketCodecs.JOIN, JoinInformation(version), markLoopback = true)
    }

    fun sendKeyPress(packet: KeyPress) {
        send(NetworkManager.PACKET_KEY_PRESS_ID, PacketCodecs.KEY, packet)
    }

    fun sendPatterns(packet: ClientPatternSync) {
        send(NetworkManager.PACKET_PATTERNS_ID, PacketCodecs.PATTERNS, packet)
    }

    private fun <T> send(channel: String, codec: PacketCodec<T>, packet: T, markLoopback: Boolean = false) {
        val bytes = codec.encode(packet)
        val plat = platform
        if (plat == null) {
            logger.warn("Send '$channel' called before ClientNetworkRouter.init()")
            return
        }
        plat.sendC2S(channel, bytes)
    }

    fun onDisconnect() {
        LocalLoopback.reset()
    }
}

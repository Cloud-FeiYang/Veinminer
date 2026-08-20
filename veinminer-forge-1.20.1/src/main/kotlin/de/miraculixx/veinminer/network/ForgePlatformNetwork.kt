package de.miraculixx.veinminer.network

import de.miraculixx.veinminer.Veinminer
import de.miraculixx.veinminer.utils.mcServer
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkInstance
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.network.simple.SimpleChannel
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object ForgePlatformNetwork : PlatformNetwork {
    private const val PROTOCOL_VERSION = "2"

    private val CHANNEL: SimpleChannel by lazy {
        getOrCreateChannel()
    }

    private fun getOrCreateChannel(): SimpleChannel {
        val id = ResourceLocation(NetworkManager.PACKET_IDENTIFIER, "main")
        try {
            val field = NetworkRegistry::class.java.getDeclaredField("instances")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val instances = field.get(null) as Map<ResourceLocation, NetworkInstance>
            val existing = instances[id]
            if (existing != null) {
                return SimpleChannel(existing)
            }
        } catch (_: Throwable) {
        }

        return NetworkRegistry.newSimpleChannel(
            id,
            { PROTOCOL_VERSION },
            { it == PROTOCOL_VERSION || NetworkRegistry.ACCEPTVANILLA == it },
            { it == PROTOCOL_VERSION || NetworkRegistry.ACCEPTVANILLA == it }
        )
    }

    private val c2sHandlers: MutableMap<String, (UUID, ByteArray) -> Unit> = ConcurrentHashMap()
    private val packetId = AtomicInteger(0)

    private var registered = false

    fun init() {
        if (registered) return
        registered = true
        val c2sId = packetId.getAndIncrement()
        val s2cId = packetId.getAndIncrement()

        CHANNEL.registerMessage(
            c2sId,
            VeinminerForgePacket::class.java,
            VeinminerForgePacket::encode,
            VeinminerForgePacket::decode,
            { pkt, ctxRef ->
                val ctx = ctxRef.get()
                ctx.enqueueWork {
                    val sender = ctx.sender ?: return@enqueueWork
                    val handler = c2sHandlers[pkt.channel]
                    if (handler != null) {
                        try {
                            handler(sender.uuid, pkt.bytes)
                        } catch (e: Exception) {
                            Veinminer.LOGGER.warn("Failed to dispatch C2S '${pkt.channel}': ${e.message}")
                        }
                    }
                }
                ctx.packetHandled = true
            },
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        )

        CHANNEL.registerMessage(
            s2cId,
            VeinminerForgeS2CPacket::class.java,
            VeinminerForgeS2CPacket::encode,
            VeinminerForgeS2CPacket::decode,
            { pkt, ctxRef ->
                val ctx = ctxRef.get()
                ctx.enqueueWork {
                    ClientNetworkRouter.dispatchClientbound(pkt.channel, pkt.bytes)
                }
                ctx.packetHandled = true
            },
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        )
    }

    override fun registerC2S(channel: String, handler: (UUID, ByteArray) -> Unit) {
        c2sHandlers[channel] = handler
    }

    override fun registerS2C(channel: String) {}

    override fun sendS2C(playerId: UUID, channel: String, payload: ByteArray) {
        if (LocalLoopback.isLoopbackPlayer(playerId)) {
            ClientNetworkRouter.dispatchClientbound(channel, payload)
            return
        }
        val player = mcServer?.playerList?.getPlayer(playerId) as? ServerPlayer
        if (player == null) {
            Veinminer.LOGGER.warn("sendS2C '$channel' failed: Player $playerId not found in playerList")
            return
        }
        try {
            Veinminer.LOGGER.info("Sending S2C '$channel' (${payload.size} bytes) to ${player.scoreboardName}")
            CHANNEL.send(PacketDistributor.PLAYER.with { player }, VeinminerForgeS2CPacket(channel, payload))
        } catch (e: Exception) {
            Veinminer.LOGGER.error("Failed to send S2C '$channel' to ${player.scoreboardName}: ${e.message}", e)
        }
    }

    fun sendC2S(channel: String, bytes: ByteArray) {
        CHANNEL.sendToServer(VeinminerForgePacket(channel, bytes))
    }
}

data class VeinminerForgePacket(val channel: String, val bytes: ByteArray) {
    companion object {
        fun encode(pkt: VeinminerForgePacket, buf: FriendlyByteBuf) {
            buf.writeUtf(pkt.channel)
            buf.writeByteArray(pkt.bytes)
        }

        fun decode(buf: FriendlyByteBuf): VeinminerForgePacket {
            val channel = buf.readUtf()
            val bytes = buf.readByteArray()
            return VeinminerForgePacket(channel, bytes)
        }
    }
}

data class VeinminerForgeS2CPacket(val channel: String, val bytes: ByteArray) {
    companion object {
        fun encode(pkt: VeinminerForgeS2CPacket, buf: FriendlyByteBuf) {
            buf.writeUtf(pkt.channel)
            buf.writeByteArray(pkt.bytes)
        }

        fun decode(buf: FriendlyByteBuf): VeinminerForgeS2CPacket {
            val channel = buf.readUtf()
            val bytes = buf.readByteArray()
            return VeinminerForgeS2CPacket(channel, bytes)
        }
    }
}

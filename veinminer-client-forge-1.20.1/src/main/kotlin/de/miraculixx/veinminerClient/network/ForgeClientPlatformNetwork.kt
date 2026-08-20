package de.miraculixx.veinminerClient.network

import de.miraculixx.veinminer.network.ClientNetworkRouter
import de.miraculixx.veinminer.network.ClientPlatformNetwork
import de.miraculixx.veinminer.network.NetworkManager
import de.miraculixx.veinminerClient.ClientLifecycle
import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.simple.SimpleChannel
import java.util.Optional
import java.util.concurrent.atomic.AtomicInteger

object ForgeClientPlatformNetwork : ClientPlatformNetwork {
    private const val PROTOCOL_VERSION = "2"

    private val CHANNEL: SimpleChannel = NetworkRegistry.newSimpleChannel(
        ResourceLocation(NetworkManager.PACKET_IDENTIFIER, "main"),
        { PROTOCOL_VERSION },
        { it == PROTOCOL_VERSION || NetworkRegistry.ACCEPTVANILLA == it },
        { it == PROTOCOL_VERSION || NetworkRegistry.ACCEPTVANILLA == it }
    )

    private val packetId = AtomicInteger(0)
    private var registered = false

    private fun ensureRegistered() {
        if (registered) return
        registered = true
        val c2sId = packetId.getAndIncrement()
        val s2cId = packetId.getAndIncrement()

        CHANNEL.registerMessage(
            c2sId,
            ClientForgeC2SPacket::class.java,
            ClientForgeC2SPacket::encode,
            ClientForgeC2SPacket::decode,
            { _, ctxRef ->
                ctxRef.get().packetHandled = true
            },
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        )

        CHANNEL.registerMessage(
            s2cId,
            ClientForgeS2CPacket::class.java,
            ClientForgeS2CPacket::encode,
            ClientForgeS2CPacket::decode,
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

    override fun registerC2S(channel: String) {
        ensureRegistered()
    }

    override fun registerS2C(channel: String, handler: (payload: ByteArray) -> Unit) {
        ensureRegistered()
        ClientNetworkRouter.registerClientboundHandler(channel, handler)
    }

    override fun sendC2S(channel: String, payload: ByteArray) {
        ensureRegistered()
        try {
            CHANNEL.sendToServer(ClientForgeC2SPacket(channel, payload))
        } catch (e: Exception) {
            ClientLifecycle.LOGGER.warn("Failed to send C2S '$channel': ${e.message}")
        }
    }
}

data class ClientForgeC2SPacket(val channel: String, val bytes: ByteArray) {
    companion object {
        fun encode(pkt: ClientForgeC2SPacket, buf: FriendlyByteBuf) {
            buf.writeUtf(pkt.channel)
            buf.writeByteArray(pkt.bytes)
        }

        fun decode(buf: FriendlyByteBuf): ClientForgeC2SPacket {
            val channel = buf.readUtf()
            val bytes = buf.readByteArray()
            return ClientForgeC2SPacket(channel, bytes)
        }
    }
}

data class ClientForgeS2CPacket(val channel: String, val bytes: ByteArray) {
    companion object {
        fun encode(pkt: ClientForgeS2CPacket, buf: FriendlyByteBuf) {
            buf.writeUtf(pkt.channel)
            buf.writeByteArray(pkt.bytes)
        }

        fun decode(buf: FriendlyByteBuf): ClientForgeS2CPacket {
            val channel = buf.readUtf()
            val bytes = buf.readByteArray()
            return ClientForgeS2CPacket(channel, bytes)
        }
    }
}

package de.miraculixx.veinminer.network

import java.util.UUID

interface ServerCallbacks {
    fun onJoinAccepted(playerId: UUID, packet: JoinInformation)
    fun onPatterns(playerId: UUID, packet: ClientPatternSync)
    fun onKeyPress(playerId: UUID, packet: KeyPress)
}

interface ClientCallbacks {
    fun onConfiguration(packet: ServerConfiguration)
}

object LocalLoopback {
    @Volatile
    var loopbackPlayer: UUID? = null

    fun isLoopbackPlayer(uuid: UUID): Boolean = loopbackPlayer != null && uuid == loopbackPlayer

    fun reset() {
        loopbackPlayer = null
    }
}

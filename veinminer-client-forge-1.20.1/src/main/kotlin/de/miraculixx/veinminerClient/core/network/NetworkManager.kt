package de.miraculixx.veinminerClient.core.network

object NetworkManager {
    const val PACKET_IDENTIFIER = "veinminer"

    const val PACKET_JOIN_ID = "join" // c2s
    const val PACKET_PATTERNS_ID = "patterns" // c2s
    const val PACKET_KEY_PRESS_ID = "key" // c2s
    const val PACKET_CONFIGURATION_ID = "configuration" // s2c
}

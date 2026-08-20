package de.miraculixx.veinminer.command

import org.slf4j.Logger

interface VeinminerHost {
    val versionVeinminer: String
    val versionMinecraft: String
    val platform: String
    val logger: Logger
    var active: Boolean
}

object ActiveHost {
    @Volatile
    lateinit var host: VeinminerHost
}

object Permissions {
    @Volatile
    private var checker: (Any?, String) -> Boolean = { _, _ -> true }

    fun install(checker: (Any?, String) -> Boolean) {
        this.checker = checker
    }

    fun check(source: Any?, node: String): Boolean = checker(source, node)
}

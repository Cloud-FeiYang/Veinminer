package de.miraculixx.veinminer.event

import de.miraculixx.veinminer.config.BaseConfigManager
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

object EventState {
    @Volatile
    var enchantmentActive: Boolean = false

    val enchantmentKey: ResourceKey<Enchantment> =
        ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation("veinminer_enchantment", "veinminer"))

    lateinit var configManager: BaseConfigManager<ResourceLocation>

    @Volatile
    var checkPermission: (Player, String) -> Boolean = { _, _ -> true }

    @Volatile
    var dropBlockExperience: (BlockState, ServerLevel, BlockPos, BlockEntity?, Entity?, ItemStack, BlockPos) -> Unit =
        { state, level, _, _, _, tool, dropPos -> state.spawnAfterBreak(level, dropPos, tool, true) }
}

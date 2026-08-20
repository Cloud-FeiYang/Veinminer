@file:Suppress("unused")

package de.miraculixx.veinminer.event

import de.miraculixx.veinminer.command.ActiveHost
import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminer.data.FixedBlockGroup
import de.miraculixx.veinminer.data.VeinminerSettingsOverride
import de.miraculixx.veinminer.extensions.mcCoroutineAsync
import de.miraculixx.veinminer.extensions.mcCoroutineSync
import de.miraculixx.veinminer.extensions.ticks
import de.miraculixx.veinminer.network.NetworkRouter
import de.miraculixx.veinminer.pattern.BlockAwareness
import de.miraculixx.veinminer.pattern.NormalStrategy
import de.miraculixx.veinminer.pattern.Surface
import de.miraculixx.veinminer.pattern.VeinmineAction
import de.miraculixx.veinminer.pattern.Veinmining
import de.miraculixx.veinminer.pattern.isMatureAgeTarget
import de.miraculixx.veinminer.utils.mcServer
import de.miraculixx.veinminer.utils.permissionVeinmine
import de.miraculixx.veinminer.utils.toNMS
import de.miraculixx.veinminer.utils.toVeinminer
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.stats.Stats
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseFireBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID

object VeinMinerEvent {
    private val cooldown = mutableSetOf<UUID>()

    private fun ResourceLocation.groupedBlocks(): FixedBlockGroup<ResourceLocation> {
        val blocks = mutableSetOf<ResourceLocation>()
        val tools = mutableSetOf<ResourceLocation>()
        var override: VeinminerSettingsOverride? = null

        EventState.configManager.groups.forEach {
            if (it.blocks.contains(this)) {
                if (override == null) override = it.override
                blocks.addAll(it.blocks)
                tools.addAll(it.tools)
            }
        }
        return FixedBlockGroup(blocks.toSet(), tools.toSet(), override)
    }

    fun BlockState.key(): ResourceLocation = BuiltInRegistries.BLOCK.getKey(block)
    fun Block.key(): ResourceLocation = BuiltInRegistries.BLOCK.getKey(this)
    fun ItemStack.key(): ResourceLocation = BuiltInRegistries.ITEM.getKey(item)

    fun onBlockBreakBefore(world: Level, player: Player, pos: BlockPos, state: BlockState): Boolean {
        val info = allowedToVeinmine(world, player, pos, state) ?: return true

        val amount = info.veinmine(true)
        player.awardStat(Stats.BLOCK_MINED.get(state.block), (amount - 1).coerceAtLeast(0))

        val cooldownTime = info.settings.cooldown
        if (cooldownTime > 0) {
            cooldown.add(player.uuid)
            mcCoroutineAsync(cooldownTime.ticks) { cooldown.remove(player.uuid) }
        }
        return true
    }

    fun allowedToVeinmine(world: Level, player: Player, pos: BlockPos, state: BlockState): VeinmineAction<ItemStack, Player>? {
        if (!ActiveHost.host.active) return null
        if (player.isCreative) return null

        val uuid = player.uuid
        val hasClient = NetworkRouter.registeredPlayers.contains(uuid)
        val material = state.key().takeIf { !state.isAir } ?: return null
        if (!state.isMatureAgeTarget()) return null

        if (hasClient && !NetworkRouter.isReady(uuid)) return null

        val blockGroup = material.groupedBlocks()
        val isGroupBlock = blockGroup.blocks.isNotEmpty()
        val settings = EventState.configManager.settings.applyOverrides(hasClient, blockGroup.override)

        if (settings.permissionRestricted && !EventState.checkPermission(player, permissionVeinmine)) return null
        val hasClientBypass = settings.client.allBlocks && NetworkRouter.registeredPlayers.containsKey(player.uuid)
        val isWhitelisted = isGroupBlock || EventState.configManager.veinBlocks.contains(material)

        if (settings.client.require && !hasClient) return null
        if (!isWhitelisted && !hasClientBypass) return null
        if (settings.mustSneak && !player.isCrouching) return null
        if (cooldown.contains(player.uuid)) return null

        val mainHandItem = player.mainHandItem
        if (settings.needCorrectTool && (state.requiresCorrectToolForDrops() && !mainHandItem.isCorrectToolForDrops(state))) return null
        if (!hasClientBypass && isGroupBlock && !blockGroup.tools.isEmpty() && !blockGroup.tools.contains(mainHandItem.key())) return null
        if (settings.decreaseDurability && mainHandItem.remainingDurability() <= 1) return null

        if (EventState.enchantmentActive) {
            val enchLocation = EventState.enchantmentKey.location()
            val hasEnchantment = EnchantmentHelper.getEnchantments(mainHandItem).keys.any { enchantment ->
                BuiltInRegistries.ENCHANTMENT.getKey(enchantment) == enchLocation
            }
            if (!hasEnchantment) return null
        }

        val blocks = if (isGroupBlock) blockGroup.blocks else setOf(material)
        val face = NetworkRouter.lastSurface[player.uuid] ?: Surface.UP
        val sourceLocation = pos.toVeinminer()
        return VeinmineAction(sourceLocation, blocks, mainHandItem, player, sourceLocation, settings, face)
    }

    fun VeinmineAction<ItemStack, Player>.veinmine(shouldBreak: Boolean): Int {
        val iTool = tool
        val iPlayer = player
        val world = iPlayer.level()

        if (iTool.isEmpty) return 0
        val strategy = NetworkRouter.activeStrategy(iPlayer.uuid) ?: NormalStrategy
        val maxDepth = NetworkRouter.maxDepth(iPlayer.uuid)

        val blockAwareness = object : BlockAwareness {
            override fun getBlockType(pos: BlockPosition): ResourceLocation {
                return world.getBlockState(BlockPos(pos.x, pos.y, pos.z)).key()
            }

            override fun isActionTarget(pos: BlockPosition): Boolean {
                return world.getBlockState(BlockPos(pos.x, pos.y, pos.z)).isMatureAgeTarget()
            }

            override fun breakBlock(pos: BlockPosition, ticks: Int): Boolean {
                if (!shouldBreak) return false
                if (tool.remainingDurability() <= 1) return false
                if (settings.hungerPerBlock > 0.0 && iPlayer.foodData.foodLevel == 0 && iPlayer.foodData.saturationLevel == 0f) return false
                val blockPos = BlockPos(pos.x, pos.y, pos.z)
                scheduleBreak(blockPos, ticks)
                return true
            }
        }

        val hits = Veinmining.veinmine(this, blockAwareness, strategy, maxDepth, shouldBreak)
        return hits.size
    }

    private fun VeinmineAction<ItemStack, Player>.scheduleBreak(pos: BlockPos, delay: Int) {
        mcCoroutineSync(mcServer!!, delay) {
            val iPlayer = player
            val world = iPlayer.level()
            val state = world.getBlockState(pos)
            if (!targetTypes.contains(state.key())) return@mcCoroutineSync
            if (!state.isMatureAgeTarget()) return@mcCoroutineSync
            val iTool = tool
            if (settings.decreaseDurability && iTool.remainingDurability() <= 1) return@mcCoroutineSync
            state.destroyBlock(iTool, world, pos, iPlayer, sourceLocation.toNMS())
            if (settings.decreaseDurability) damageItem(iTool, iPlayer)
            if (settings.hungerPerBlock > 0.0) iPlayer.causeFoodExhaustion(settings.hungerPerBlock.toFloat())
        }
    }

    private fun BlockState.destroyBlock(item: ItemStack, world: Level, position: BlockPos, player: Player, initialSource: BlockPos) {
        val block = block
        if (block !== Blocks.AIR && (!requiresCorrectToolForDrops() || item.isCorrectToolForDrops(this))) {
            improvedDropResources(this, world, position, world.getBlockEntity(position), player, item, initialSource)
            if (block is BaseFireBlock) {
                world.levelEvent(1009, position, 0)
            } else {
                world.levelEvent(2001, position, Block.getId(this))
            }
        }
        val destroyed = world.removeBlock(position, false)
        if (destroyed) {
            block.destroy(world, position, this)
        }
    }

    private fun improvedDropResources(
        blockState: BlockState,
        world: Level,
        blockPos: BlockPos,
        blockEntity: BlockEntity?,
        breaker: Entity,
        tool: ItemStack,
        initialSource: BlockPos
    ) {
        val serverLevel = world as? ServerLevel ?: return
        val dropPos = if (EventState.configManager.settings.mergeItemDrops) initialSource else blockPos
        Block.getDrops(blockState, serverLevel, blockPos, blockEntity, breaker, tool).forEach { drop: ItemStack ->
            Block.popResource(world, dropPos, drop)
        }
        EventState.dropBlockExperience(blockState, serverLevel, blockPos, blockEntity, breaker, tool, dropPos)
    }

    private fun damageItem(item: ItemStack, player: Player) {
        if (item.isEmpty) return
        item.hurtAndBreak(1, player) { it.broadcastBreakEvent(EquipmentSlot.MAINHAND) }
    }

    private fun ItemStack.remainingDurability(): Int {
        if (isEmpty) return 0
        if (maxDamage <= 0) return Int.MAX_VALUE
        return maxDamage - damageValue
    }
}

package de.miraculixx.veinminerClient.render

import com.mojang.blaze3d.vertex.PoseStack
import de.miraculixx.veinminer.data.BlockPosition
import de.miraculixx.veinminerClient.ClientLifecycle
import de.miraculixx.veinminerClient.KeyBindManager
import de.miraculixx.veinminerClient.network.NetworkManager
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.VeinminerRenderTypeHelper
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Matrix4f

object BlockHighlightingRenderer {
    private var highlightingShape: VoxelShape = Shapes.empty()

    private val renderHighlighting: RenderType = RenderType.lines()
    private val renderHighlightingTranslucent: RenderType by lazy {
        VeinminerRenderTypeHelper.createTranslucentLines("${ClientLifecycle.MOD_ID}:highlight_translucent")
    }

    fun render(
        stack: PoseStack,
        source: MultiBufferSource.BufferSource,
        camPos: Vec3,
        isTranslucentPass: Boolean
    ) {
        val targetBlock = KeyBindManager.lastTarget
        if (highlightingShape.isEmpty || targetBlock == null) return

        stack.pushPose()
        stack.translate(targetBlock.x - camPos.x, targetBlock.y - camPos.y, targetBlock.z - camPos.z)

        val matrix = stack.last().pose()
        val drawTranslucent = NetworkManager.settings.client.translucentBlockHighlight

        if (!isTranslucentPass) {
            renderBlocks(source, renderHighlighting, matrix, highlightingShape, 255)
            source.endBatch(renderHighlighting)
        } else if (drawTranslucent) {
            renderBlocks(source, renderHighlightingTranslucent, matrix, highlightingShape, 50)
            source.endBatch(renderHighlightingTranslucent)
        }

        stack.popPose()
    }

    private fun renderBlocks(
        source: MultiBufferSource.BufferSource,
        renderer: RenderType,
        matrix: Matrix4f,
        shape: VoxelShape,
        transparency: Int
    ) {
        val buffer = source.getBuffer(renderer)
        shape.forAllEdges { x, y, z, dx, dy, dz ->
            val fx = x.toFloat()
            val fy = y.toFloat()
            val fz = z.toFloat()
            val fdx = dx.toFloat()
            val fdy = dy.toFloat()
            val fdz = dz.toFloat()
            val relX = fdx - fx
            val relY = fdy - fy
            val relZ = fdz - fz

            buffer.vertex(matrix, fx, fy, fz)
                .color(255, 255, 255, transparency)
                .normal(relX, relY, relZ)
                .endVertex()
            buffer.vertex(matrix, fdx, fdy, fdz)
                .color(255, 255, 255, transparency)
                .normal(relX, relY, relZ)
                .endVertex()
        }
        source.endLastBatch()
    }

    fun setShape(positions: List<BlockPosition>) {
        val source = KeyBindManager.lastTarget
        if (positions.isEmpty() || source == null) {
            highlightingShape = Shapes.empty()
            return
        }

        val baseBox = Shapes.box(-0.010, -0.010, -0.010, 1.010, 1.010, 1.010)
        highlightingShape = positions.fold(Shapes.empty()) { acc, pos ->
            val dx = (pos.x - source.x).toDouble()
            val dy = (pos.y - source.y).toDouble()
            val dz = (pos.z - source.z).toDouble()

            val movedBox = if (dx == 0.0 && dy == 0.0 && dz == 0.0) {
                baseBox
            } else {
                baseBox.move(dx, dy, dz)
            }

            Shapes.joinUnoptimized(acc, movedBox, BooleanOp.OR)
        }
    }
}

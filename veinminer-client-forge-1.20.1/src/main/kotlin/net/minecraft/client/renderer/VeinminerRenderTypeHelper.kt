package net.minecraft.client.renderer

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat

object VeinminerRenderTypeHelper {
    fun createTranslucentLines(name: String): RenderType {
        return RenderType.create(
            name,
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            1536,
            RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                .setTextureState(RenderStateShard.NO_TEXTURE)
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL)
                .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .setLineState(RenderStateShard.DEFAULT_LINE)
                .createCompositeState(false)
        )
    }
}

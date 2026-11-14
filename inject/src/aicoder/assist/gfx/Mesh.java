package aicoder.assist.gfx;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;

/**
 * A batch of quads handed to the game's GUI renderer as one element. Positions are in framebuffer pixels;
 * the pose scales them down to the game's GUI units. With a texture each vertex also carries a UV.
 */
record Mesh(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, float[] xy, float[] uv, int[] colors, int vertices,
            ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState {
    @Override
    public void buildVertices(VertexConsumer consumer) {
        for (int i = 0; i < vertices; i++) {
            VertexConsumer vertex = consumer.addVertexWith2DPose(pose, xy[i * 2], xy[i * 2 + 1]);
            if (uv != null) vertex.setUv(uv[i * 2], uv[i * 2 + 1]);
            vertex.setColor(colors[i]);
        }
    }
}

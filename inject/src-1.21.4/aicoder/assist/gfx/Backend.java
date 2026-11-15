package aicoder.assist.gfx;

import aicoder.assist.Client;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * The only part of the drawing code that knows how this game version draws GUI shapes.
 * This is the Backend for 1.21.4; it replaces the 1.21.11 one in src/ when that version is built.
 *
 * <p>Here: each batch is written straight into the GUI render type through GuiGraphics.drawSpecial,
 * which draws it at once, so batches come out in the order they were made.
 */
final class Backend {
    private Backend() {
    }

    private static GuiGraphics graphics;
    private static Matrix4f pose;
    private static double guiScale = 1;

    /** {@code scale} is how many framebuffer pixels one of the game's GUI units covers. */
    static void begin(GuiGraphics g, double scale) {
        graphics = g;
        guiScale = scale;
        pose = new Matrix4f(g.pose().last().pose()).scale((float) (1 / scale), (float) (1 / scale), 1);
    }

    static void end() {
        graphics = null;
    }

    /** A texture from ARGB pixels, filtered linearly. The returned handle goes to {@link #draw}. */
    static Object texture(String name, int width, int height, int[] argb) {
        NativeImage image = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) image.setPixel(x, y, argb[y * width + x]);
        }
        DynamicTexture texture = new DynamicTexture(image);
        texture.setFilter(true, false);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("aicoder-assist", name.toLowerCase(java.util.Locale.ROOT));
        Client.mc.getTextureManager().register(id, texture);
        return id;
    }

    static void close(Object texture) {
        Client.mc.getTextureManager().release((ResourceLocation) texture);
    }

    /** Positions are framebuffer pixels; {@code scissor} is {x0, y0, x1, y1} or null. */
    static void draw(Object texture, float[] xy, float[] uv, int[] colors, int count, double[] scissor, float x0, float y0, float x1, float y1) {
        if (graphics == null) return;

        if (scissor != null) {
            int left = (int) Math.floor(scissor[0] / guiScale), top = (int) Math.floor(scissor[1] / guiScale);
            int right = (int) Math.ceil(scissor[2] / guiScale), bottom = (int) Math.ceil(scissor[3] / guiScale);
            if (right <= left || bottom <= top) return;
            graphics.enableScissor(left, top, right, bottom);
        }
        RenderType type = texture == null ? RenderType.gui() : RenderType.guiTextured((ResourceLocation) texture);
        graphics.drawSpecial(source -> {
            VertexConsumer consumer = source.getBuffer(type);
            for (int i = 0; i < count; i++) {
                VertexConsumer vertex = consumer.addVertex(pose, xy[i * 2], xy[i * 2 + 1], 0);
                if (uv != null) vertex.setUv(uv[i * 2], uv[i * 2 + 1]);
                vertex.setColor(colors[i]);
            }
        });
        if (scissor != null) graphics.disableScissor();
    }
}

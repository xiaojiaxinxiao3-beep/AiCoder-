package aicoder.assist.gfx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * The only part of the drawing code that knows how this game version draws GUI shapes. This is the one
 * for 1.8.9: every batch is drawn at once through the game's Tessellator, in the old fixed-function
 * OpenGL the game itself uses for its GUI.
 */
final class Backend {
    private Backend() {
    }

    private static double guiScale = 1;
    private static boolean open;

    /** {@code scale} is how many framebuffer pixels one of the game's GUI units covers. */
    static void begin(Object graphics, double scale) {
        guiScale = scale;
        open = true;
    }

    static void end() {
        open = false;
    }

    /** A texture from ARGB pixels, filtered linearly. The returned handle goes to {@link #draw}. */
    static Object texture(String name, int width, int height, int[] argb) {
        DynamicTexture texture = new DynamicTexture(width, height);
        System.arraycopy(argb, 0, texture.getTextureData(), 0, width * height);
        texture.updateDynamicTexture();
        GlStateManager.bindTexture(texture.getGlTextureId());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        return texture;
    }

    static void close(Object texture) {
        ((DynamicTexture) texture).deleteGlTexture();
    }

    /** Positions are framebuffer pixels; {@code scissor} is {x0, y0, x1, y1} or null. */
    static void draw(Object texture, float[] xy, float[] uv, int[] colors, int count, double[] scissor, float x0, float y0, float x1, float y1) {
        if (!open || count == 0) return;
        Minecraft mc = Minecraft.getMinecraft();

        if (scissor != null) {
            int left = (int) Math.floor(scissor[0]), top = (int) Math.floor(scissor[1]);
            int right = (int) Math.ceil(scissor[2]), bottom = (int) Math.ceil(scissor[3]);
            if (right <= left || bottom <= top) return;
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(left, mc.displayHeight - bottom, right - left, bottom - top);
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(1 / guiScale, 1 / guiScale, 1);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.color(1, 1, 1, 1);
        if (texture == null) GlStateManager.disableTexture2D();
        else {
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(((DynamicTexture) texture).getGlTextureId());
        }

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(GL11.GL_QUADS, texture == null ? DefaultVertexFormats.POSITION_COLOR : DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < count; i++) {
            int c = colors[i];
            buffer.pos(xy[i * 2], xy[i * 2 + 1], 0);
            if (texture != null) buffer.tex(uv[i * 2], uv[i * 2 + 1]);
            buffer.color(c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF, c >>> 24).endVertex();
        }
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
        if (scissor != null) GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}

package aicoder.assist.gfx;

import aicoder.assist.Names;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.joml.Matrix3x2f;

import java.lang.reflect.Field;

/**
 * The only part of the drawing code that knows how this game version draws GUI shapes. There is one
 * Backend per supported version (this is the one for 1.21.11; others live in src-&lt;version&gt;/).
 *
 * <p>Here: each batch becomes a {@link Mesh} handed to the GuiRenderState inside GuiGraphics.
 */
final class Backend {
    private Backend() {
    }

    private static Field stateField;
    private static GuiGraphics graphics;
    private static GuiRenderState state;
    private static Matrix3x2f pose;
    private static double guiScale = 1;

    /** {@code scale} is how many framebuffer pixels one of the game's GUI units covers. */
    static void begin(GuiGraphics g, double scale) {
        graphics = g;
        guiScale = scale;
        pose = new Matrix3x2f(g.pose()).scale((float) (1 / scale));
        try {
            if (stateField == null) {
                stateField = GuiGraphics.class.getDeclaredField(Names.shared().get("GuiGraphics.guiRenderState"));
                stateField.setAccessible(true);
            }
            state = (GuiRenderState) stateField.get(g);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static void end() {
        graphics = null;
        state = null;
    }

    /** A texture from ARGB pixels, filtered linearly. The returned handle goes to {@link #draw}. */
    static Object texture(String name, int width, int height, int[] argb) {
        NativeImage image = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int c = argb[y * width + x];
                image.setPixelABGR(x, y, (c & 0xFF00FF00) | (c >> 16 & 0xFF) | (c & 0xFF) << 16);
            }
        }
        return new DynamicTexture(() -> "aicoder-assist-" + name, image);
    }

    static void close(Object texture) {
        ((DynamicTexture) texture).close();
    }

    /** Positions are framebuffer pixels; {@code scissor} is {x0, y0, x1, y1} or null. */
    static void draw(Object texture, float[] xy, float[] uv, int[] colors, int count, double[] scissor, float x0, float y0, float x1, float y1) {
        if (state == null) return;

        ScreenRectangle bounds = rectangle(x0, y0, x1, y1);
        ScreenRectangle clip = null;
        if (scissor != null) {
            clip = rectangle(scissor[0], scissor[1], scissor[2], scissor[3]);
            bounds = bounds.intersection(clip);
            if (bounds == null) return;
        }

        TextureSetup setup = texture == null ? TextureSetup.noTexture()
            : TextureSetup.singleTexture(((DynamicTexture) texture).getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        state.submitGuiElement(new Mesh(texture == null ? RenderPipelines.GUI : RenderPipelines.GUI_TEXTURED, setup, pose, xy, uv, colors, count,
            clip, bounds.transformMaxBounds(graphics.pose())));
    }

    /** Framebuffer pixels to the game's GUI units, rounded outwards. */
    private static ScreenRectangle rectangle(double x0, double y0, double x1, double y1) {
        int left = (int) Math.floor(x0 / guiScale), top = (int) Math.floor(y0 / guiScale);
        int right = (int) Math.ceil(x1 / guiScale), bottom = (int) Math.ceil(y1 / guiScale);
        return new ScreenRectangle(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }
}

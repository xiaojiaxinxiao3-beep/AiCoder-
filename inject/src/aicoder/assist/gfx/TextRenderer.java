package aicoder.assist.gfx;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackRange;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * Draws Comfortaa, the font the aurora themes use under Meteor, which the unmodified game does not have.
 * The sizes, metrics and scaling follow Meteor's own text renderer step for step, so text measured and
 * laid out by the theme code comes out the same here.
 */
public final class TextRenderer {
    /** One baked size of the font: a texture of glyphs and where each glyph sits in it. */
    private static final class Font {
        final int height;
        final float scale, ascent;
        final int size;
        final float[][] glyphs = new float[256][];
        final DynamicTexture texture;

        Font(ByteBuffer ttf, int height) {
            this.height = height;
            this.size = height <= 30 ? 512 : (height <= 56 ? 1024 : 2048);

            STBTTFontinfo info = STBTTFontinfo.create();
            STBTruetype.stbtt_InitFont(info, ttf);

            ByteBuffer bitmap = BufferUtils.createByteBuffer(size * size);
            STBTTPackedchar.Buffer[] data = {STBTTPackedchar.create(95), STBTTPackedchar.create(96)};
            int[] first = {32, 160};

            STBTTPackContext context = STBTTPackContext.create();
            STBTruetype.stbtt_PackBegin(context, bitmap, size, size, 0, 1);
            STBTTPackRange.Buffer ranges = STBTTPackRange.create(2);
            ranges.put(STBTTPackRange.create().set(height, 32, null, 95, data[0], (byte) 2, (byte) 2));
            ranges.put(STBTTPackRange.create().set(height, 160, null, 96, data[1], (byte) 2, (byte) 2));
            ranges.flip();
            STBTruetype.stbtt_PackFontRanges(context, ttf, 0, ranges);
            STBTruetype.stbtt_PackEnd(context);

            scale = STBTruetype.stbtt_ScaleForPixelHeight(info, height);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer up = stack.mallocInt(1);
                STBTruetype.stbtt_GetFontVMetrics(info, up, null, null);
                ascent = up.get(0);
            }

            for (int range = 0; range < 2; range++) {
                for (int i = 0; i < data[range].capacity(); i++) {
                    STBTTPackedchar c = data[range].get(i);
                    glyphs[first[range] + i] = new float[]{c.xoff(), c.yoff(), c.xoff2(), c.yoff2(),
                        c.x0() / (float) size, c.y0() / (float) size, c.x1() / (float) size, c.y1() / (float) size, c.xadvance()};
                }
            }

            // White pixels whose alpha is the glyph coverage
            NativeImage image = new NativeImage(size, size, false);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) image.setPixelABGR(x, y, ((bitmap.get(y * size + x) & 0xFF) << 24) | 0xFFFFFF);
            }
            texture = new DynamicTexture(() -> "aicoder-assist-font-" + height, image);
        }

        float[] glyph(char c) {
            float[] g = c < 256 ? glyphs[c] : null;
            return g != null ? g : glyphs[' '];
        }

        double width(String text) {
            double width = 0;
            for (int i = 0; i < text.length(); i++) width += glyph(text.charAt(i))[8];
            return width;
        }
    }

    private static final int[] HEIGHTS = {27, 41, 54, 68, 81};

    private final GuiRenderer renderer;
    private final Font[] fonts = new Font[HEIGHTS.length];
    private ByteBuffer ttf;

    private Font font;
    private double scale = 1;
    private boolean building;

    private float[] xy = new float[4096], uv = new float[4096];
    private int[] colors = new int[2048];
    private int vertices;
    private float minX, minY, maxX, maxY;

    TextRenderer(GuiRenderer renderer) {
        this.renderer = renderer;
    }

    private Font font(int index) {
        if (fonts[index] == null) {
            if (ttf == null) {
                try (InputStream in = TextRenderer.class.getResourceAsStream("/aicoder/assist/Comfortaa.ttf")) {
                    byte[] bytes = in.readAllBytes();
                    ttf = BufferUtils.createByteBuffer(bytes.length).put(bytes).flip();
                } catch (Exception e) {
                    throw new IllegalStateException("Font missing from the jar", e);
                }
            }
            fonts[index] = new Font(ttf, HEIGHTS[index]);
        }
        return fonts[index];
    }

    private Font current() {
        return font != null ? font : font(0);
    }

    public boolean isBuilding() {
        return building;
    }

    /** Width in framebuffer pixels at the scale given to {@link #begin}, or at scale 1 outside of it. */
    public double getWidth(String text) {
        return current().width(text) * scale / 1.5;
    }

    public double getHeight() {
        return (current().height + 1) * scale / 1.5;
    }

    public void begin(double scale) {
        double rounded = Math.floor(scale * 10) / 10;
        int index = rounded >= 3 ? 4 : rounded >= 2.5 ? 3 : rounded >= 2 ? 2 : rounded >= 1.5 ? 1 : 0;

        font = font(index);
        double fontScale = font.height / 27.0;
        this.scale = 1 + (scale - fontScale) / fontScale;
        building = true;
        vertices = 0;
        minX = minY = Float.MAX_VALUE;
        maxX = maxY = -Float.MAX_VALUE;
    }

    public double render(String text, double x, double y, Color color) {
        Font f = current();
        double s = scale / 1.5;
        int argb = color.argb();
        y += f.ascent * f.scale * s;

        for (int i = 0; i < text.length(); i++) {
            float[] g = f.glyph(text.charAt(i));
            if (g[2] > g[0]) {
                vertex(x + g[0] * s, y + g[1] * s, g[4], g[5], argb);
                vertex(x + g[0] * s, y + g[3] * s, g[4], g[7], argb);
                vertex(x + g[2] * s, y + g[3] * s, g[6], g[7], argb);
                vertex(x + g[2] * s, y + g[1] * s, g[6], g[5], argb);
            }
            x += g[8] * s;
        }
        return x;
    }

    private void vertex(double x, double y, float u, float v, int color) {
        if (vertices == colors.length) {
            colors = Arrays.copyOf(colors, colors.length * 2);
            xy = Arrays.copyOf(xy, xy.length * 2);
            uv = Arrays.copyOf(uv, uv.length * 2);
        }
        float fx = (float) x, fy = (float) y;
        xy[vertices * 2] = fx;
        xy[vertices * 2 + 1] = fy;
        uv[vertices * 2] = u;
        uv[vertices * 2 + 1] = v;
        colors[vertices++] = color;

        if (fx < minX) minX = fx;
        if (fy < minY) minY = fy;
        if (fx > maxX) maxX = fx;
        if (fy > maxY) maxY = fy;
    }

    public void end() {
        if (vertices > 0) {
            TextureSetup texture = TextureSetup.singleTexture(font.texture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderer.submit(RenderPipelines.GUI_TEXTURED, texture, Arrays.copyOf(xy, vertices * 2), Arrays.copyOf(uv, vertices * 2),
                Arrays.copyOf(colors, vertices), vertices, minX, minY, maxX, maxY);
        }
        vertices = 0;
        building = false;
        scale = 1;
        font = null;
    }
}

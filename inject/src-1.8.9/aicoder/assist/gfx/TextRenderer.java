package aicoder.assist.gfx;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Draws Comfortaa, the font the aurora themes use under Meteor, which the unmodified game does not have.
 * The sizes, metrics and scaling follow Meteor's own text renderer step for step, so text measured and
 * laid out by the theme code comes out the same here.
 */
public final class TextRenderer {
    /**
     * One baked size of the font: a texture of glyphs and where each glyph sits in it. 1.8.9 has no font
     * library of its own, so the glyphs are drawn with Java's; the size is chosen the way the newer
     * versions' library chooses it (ascent plus descent equal to the pixel height), so widths agree.
     */
    private static final class Font {
        final int height;
        final float scale = 1, ascent;
        final int size;
        final float[][] glyphs = new float[256][];
        final Object texture;

        Font(java.awt.Font base, int height) {
            this.height = height;
            this.size = height <= 30 ? 512 : (height <= 56 ? 1024 : 2048);

            FontRenderContext context = new FontRenderContext(null, true, true);
            java.awt.font.LineMetrics unit = base.deriveFont(1000f).getLineMetrics("Ag", context);
            java.awt.Font font = base.deriveFont(height * 1000f / (unit.getAscent() + unit.getDescent()));
            ascent = font.getLineMetrics("Ag", context).getAscent();

            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setColor(java.awt.Color.WHITE);

            int penX = 2, penY = 2, row = 0;
            for (int c = 32; c < 256; c++) {
                if (c >= 127 && c < 160) continue;
                GlyphVector vector = font.createGlyphVector(context, String.valueOf((char) c));
                Rectangle box = vector.getGlyphPixelBounds(0, context, 0, 0);
                float advance = (float) vector.getGlyphMetrics(0).getAdvanceX();
                int w = box.width + 2, h = box.height + 2;
                if (box.width <= 0 || box.height <= 0) {
                    glyphs[c] = new float[]{0, 0, 0, 0, 0, 0, 0, 0, advance};
                    continue;
                }
                if (penX + w + 2 > size) {
                    penX = 2;
                    penY += row + 2;
                    row = 0;
                }
                if (penY + h + 2 > size) break;
                // One pixel of room around the glyph, so the linear filter has clear pixels to blend with
                g.drawGlyphVector(vector, penX + 1 - box.x, penY + 1 - box.y);
                glyphs[c] = new float[]{box.x - 1, box.y - 1, box.x + box.width + 1, box.y + box.height + 1,
                    penX / (float) size, penY / (float) size, (penX + w) / (float) size, (penY + h) / (float) size, advance};
                penX += w + 2;
                row = Math.max(row, h);
            }
            g.dispose();

            // White pixels whose alpha is the glyph coverage
            int[] pixels = image.getRGB(0, 0, size, size, null, 0, size);
            for (int i = 0; i < pixels.length; i++) pixels[i] |= 0xFFFFFF;
            texture = Backend.texture("font-" + height, size, size, pixels);
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
    private java.awt.Font ttf;

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
                    ttf = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, in);
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
            renderer.submit(font.texture, Arrays.copyOf(xy, vertices * 2), Arrays.copyOf(uv, vertices * 2),
                Arrays.copyOf(colors, vertices), vertices, minX, minY, maxX, maxY);
        }
        vertices = 0;
        building = false;
        scale = 1;
        font = null;
    }
}

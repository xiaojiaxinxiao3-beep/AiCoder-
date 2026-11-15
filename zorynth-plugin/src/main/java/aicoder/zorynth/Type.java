package aicoder.zorynth;

import dev.zorynth.api.PluginApi;
import dev.zorynth.api.PluginApi.Canvas;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Text. With "Smooth Font" on, ASCII is drawn from the baked Comfortaa atlases (one image per colour,
 * loaded through UiHost.image); anything else, or any failure, falls back to the client's own font.
 */
final class Type {
    private static final int CELL_W = 48, CELL_H = 56, PAD = 4;
    private static final String DIR = "assets/aicoder_themes/font/";
    /** Canvas units per atlas pixel: a 56 px cell becomes 14 units tall, capitals about 7.5. */
    private static final double K = .25;

    private final PluginApi.UiHost host;
    private final PluginApi.Value<Boolean> enabled;
    private final Map<Integer, String> textures = new HashMap<>();
    private int[] advance;
    private boolean broken;

    Type(PluginApi.UiHost host, PluginApi.Value<Boolean> enabled) {
        this.host = host; this.enabled = enabled;
    }

    private boolean ready() {
        if (broken || !enabled.get()) return false;
        if (advance != null) return true;
        try (InputStream in = Type.class.getClassLoader().getResourceAsStream(DIR + "metrics.txt")) {
            String[] parts = new String(in.readAllBytes(), StandardCharsets.US_ASCII).trim().split("\\s+");
            int[] values = new int[95];
            for (int i = 0; i < 95; i++) values[i] = Integer.parseInt(parts[i]);
            advance = values;
            return true;
        } catch (Exception e) {
            broken = true;
            return false;
        }
    }

    private static boolean ascii(String text) {
        for (int i = 0; i < text.length(); i++) { char ch = text.charAt(i); if (ch < 32 || ch > 126) return false; }
        return true;
    }

    private String texture(int rgb) {
        if (textures.containsKey(rgb)) return textures.get(rgb);
        String texture = null;
        if (Type.class.getClassLoader().getResource(DIR + String.format("%06x.png", rgb)) != null) {
            try { texture = host.image(DIR + String.format("%06x.png", rgb)); } catch (Exception e) { texture = null; }
        }
        textures.put(rgb, texture);
        return texture;
    }

    int width(Canvas c, String text) {
        if (!ready() || !ascii(text)) return c.textWidth(text);
        int sum = 0;
        for (int i = 0; i < text.length(); i++) sum += advance[text.charAt(i) - 32];
        return (int) Math.ceil(sum * K);
    }

    /** Same anchor as Canvas.text: (x, y) is the top left of a line about 8 units tall. */
    void text(Canvas c, String text, int x, int y, int argb, boolean shadow) {
        String texture = ready() && ascii(text) && argb >>> 24 >= 0xC0 ? texture(argb & 0xFFFFFF) : null;
        if (texture == null) { c.text(text, x, y, argb, shadow); return; }
        c.push();
        c.translate(x - PAD * K, y - 3.2);
        c.scale(K, K);
        int pen = 0;
        for (int i = 0; i < text.length(); i++) {
            int k = text.charAt(i) - 32;
            if (k > 0) {
                float u = k % 16 / 16f, v = k / 16 / 6f;
                c.image(texture, pen, 0, CELL_W, CELL_H, u, v, u + 1 / 16f, v + 1 / 6f);
            }
            pen += advance[k];
        }
        c.pop();
    }

    String fit(Canvas c, String text, int width) {
        if (width <= 0) return "";
        if (width(c, text) <= width) return text;
        int n = text.length();
        while (n > 0 && width(c, text.substring(0, n) + "..") > width) n--;
        return n == 0 ? "" : text.substring(0, n) + "..";
    }
}

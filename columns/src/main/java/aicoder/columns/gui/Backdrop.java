package aicoder.columns.gui;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.Random;

import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

/** Full-screen effects that sit behind every window: drifting lights and the data rain. */
public final class Backdrop {
    private Backdrop() {
    }

    private static final int LIGHTS = 30;
    private static final Color[] LIGHT_COLORS = {Cyber.CYAN, Cyber.MAGENTA, Cyber.PURPLE, Cyber.CYAN};

    // x, y (0..1 of the screen), size (design px), alpha, flow seed, drift amplitude (design px)
    private static final double[][] LIGHT = new double[LIGHTS][6];

    private static final int RAIN_ROWS = 22;
    private static final int RAIN_MAX_COLUMNS = 96;
    private static final String KANA = "アイウエオカキクケコサシスセソタチツテトナニヌネノ";
    private static final String HEX = "0123456789ABCDEF";

    private static final String[][] RAIN_KANA = new String[RAIN_MAX_COLUMNS][RAIN_ROWS];
    private static final String[][] RAIN_HEX = new String[RAIN_MAX_COLUMNS][RAIN_ROWS];
    private static final double[] RAIN_JITTER = new double[RAIN_MAX_COLUMNS];
    private static final double[] RAIN_OFFSET = new double[RAIN_MAX_COLUMNS];

    static {
        Random random = new Random(0xA1C0DE);

        for (double[] light : LIGHT) {
            light[0] = random.nextDouble();
            light[1] = random.nextDouble();
            light[2] = 14 + random.nextDouble() * 46;
            light[3] = 0.18 + random.nextDouble() * 0.3;
            light[4] = random.nextDouble();
            light[5] = 20 + random.nextDouble() * 40;
        }

        String mixed = KANA + HEX;
        for (int i = 0; i < RAIN_MAX_COLUMNS; i++) {
            for (int k = 0; k < RAIN_ROWS; k++) {
                RAIN_KANA[i][k] = String.valueOf(mixed.charAt(random.nextInt(mixed.length())));
                RAIN_HEX[i][k] = String.valueOf(HEX.charAt(random.nextInt(HEX.length())));
            }
            RAIN_JITTER[i] = random.nextDouble() * 10;
            RAIN_OFFSET[i] = random.nextDouble();
        }
    }

    public static void render(GuiRenderer renderer, AiCoderGuiTheme theme) {
        double w = getWindowWidth();
        double h = getWindowHeight();
        double unit = theme.unit();
        double p = Cyber.phase();

        Cyber.nextFrame();

        // Own scissor so the textured lights and the rain are flushed before any window is drawn
        renderer.scissorStart(0, 0, w, h);
        dim(renderer, w, h, unit, theme.backdropDim.get());
        if (theme.lights.get()) lights(renderer, w, h, unit, p);
        if (theme.dataRain.get()) rain(renderer, w, h, unit, p);
        renderer.scissorEnd();
    }

    /** The mockup's stage: near-black violet, warmer bottom-left, cooler top-right, darker towards the edges. */
    private static void dim(GuiRenderer renderer, double w, double h, double unit, double amount) {
        if (amount <= 0) return;

        Color base = Cyber.rgba(7, 2, 12, amount);
        Color warm = Cyber.rgba(74, 12, 40, amount);
        Color cool = Cyber.rgba(4, 44, 58, amount);
        renderer.quad(0, 0, w, h, base, cool, base, warm);

        double edge = Math.min(160 * unit, Math.min(w, h) / 3);
        Color shade = Cyber.rgba(0, 0, 0, 0.6 * amount);
        Color none = Cyber.rgba(0, 0, 0, 0);
        renderer.quad(0, 0, w, edge, shade, shade, none, none);
        renderer.quad(0, h - edge, w, edge, none, none, shade, shade);
        renderer.quad(0, 0, edge, h, shade, none, none, shade);
        renderer.quad(w - edge, 0, edge, h, none, shade, shade, none);
    }

    private static void lights(GuiRenderer renderer, double w, double h, double unit, double p) {
        for (int i = 0; i < LIGHTS; i++) {
            double[] l = LIGHT[i];
            double size = l[2] * unit;
            double cx = l[0] * w + Cyber.flow(p, l[4]) * l[5] * unit;
            double cy = l[1] * h + Cyber.flow(p, l[4] + 0.4) * l[5] * 0.6 * unit;
            double alpha = l[3] * Cyber.clamp01(0.55 + 0.35 * Cyber.flow(p, l[4] + 0.2));
            Color color = LIGHT_COLORS[i % 4];

            // Concentric discs stand in for a blurred light
            for (int ring = 0; ring < 4; ring++) {
                double s = size * (1.6 - ring * 0.35);
                renderer.quad(cx - s / 2, cy - s / 2, s, s, GuiRenderer.CIRCLE, Cyber.a(color, alpha * 0.3));
            }
        }
    }

    private static void rain(GuiRenderer renderer, double w, double h, double unit, double p) {
        boolean vanilla = Cyber.vanillaText();
        String[][] glyphs = vanilla ? RAIN_KANA : RAIN_HEX;
        Cyber.TextBatch text = Cyber.text(renderer, unit);

        double pitch = 28 * unit;
        double rowH = 13 * unit;
        double travel = h + 300 * unit;
        int columns = Math.min(RAIN_MAX_COLUMNS, (int) Math.ceil(w / pitch));

        for (int i = 0; i < columns; i++) {
            int speed = 1 + (i * 7) % 3;
            double frac = (RAIN_OFFSET[i] + p * speed) % 1;
            double x = i * pitch + RAIN_JITTER[i] * unit;
            double top = frac * travel - 300 * unit;
            Color base = i % 3 == 0 ? Cyber.MAGENTA : Cyber.CYAN;

            for (int k = 0; k < RAIN_ROWS; k++) {
                double y = top + k * rowH;
                if (y < -rowH || y > h) continue;

                // Vanilla text cannot be drawn behind a window, so it has to stop at the window's edge
                if (vanilla && Cyber.occluded(x + 6 * unit, y + rowH / 2)) continue;

                // Fade in over the first 30% of the column and out over the last 30%
                double f = (k + 0.5) / RAIN_ROWS;
                double fade = f < 0.3 ? f / 0.3 : (f > 0.7 ? (1 - f) / 0.3 : 1);

                text.put(glyphs[i][k], x, y, 11, Cyber.a(base, 0.55 * 0.55 * fade));
            }
        }
    }
}

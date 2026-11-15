package aicoder.assist.gfx;

import aicoder.assist.Client;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The image behind the liquid glass. It is laid out to cover the whole screen, but only the parts
 * under a glass pane are drawn, each as a rounded piece with UVs taken from its place on screen.
 *
 * <p>One image comes with the client. Your own go in the folder {@link #folder()} as PNG or JPG;
 * the list is read again every time you cycle.
 */
public final class Wallpaper {
    private Wallpaper() {
    }

    private static final String BUNDLED = "Crimson Scythe";
    private static final String BUNDLED_PATH = "/aicoder/assist/wallpapers/crimson_scythe.jpg";
    private static final int MAX_SIDE = 2048;

    /** "" is no image, {@link #BUNDLED} the built-in one, anything else a file name in the folder. */
    public static String selected = BUNDLED;

    private static String loaded;
    private static Object texture;
    private static int imageW, imageH;
    private static double screenW, screenH, opacity;

    public static File folder() {
        return new File(Client.mc.gameDirectory, "aicoder-assist-wallpapers");
    }

    public static List<String> choices() {
        List<String> out = new ArrayList<>();
        out.add("");
        out.add(BUNDLED);
        File dir = folder();
        dir.mkdirs();
        String[] files = dir.list((d, name) -> {
            String n = name.toLowerCase(Locale.ROOT);
            return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg");
        });
        if (files != null) {
            Arrays.sort(files, String.CASE_INSENSITIVE_ORDER);
            out.addAll(Arrays.asList(files));
        }
        return out;
    }

    public static void cycle(int step) {
        List<String> all = choices();
        int at = Math.max(0, all.indexOf(selected));
        selected = all.get(Math.floorMod(at + step, all.size()));
    }

    public static String label() {
        if (selected.isEmpty()) return "NONE";
        String name = selected.contains(".") ? selected.substring(0, selected.lastIndexOf('.')) : selected;
        return (name.length() > 22 ? name.substring(0, 21) + "…" : name).toUpperCase(Locale.ROOT);
    }

    /** Once per frame, before any pane is drawn. */
    public static void frame(double width, double height, double imageOpacity) {
        screenW = width;
        screenH = height;
        opacity = imageOpacity;
        if (!selected.equals(loaded)) load();
    }

    private static void load() {
        loaded = selected;
        if (texture != null) {
            Backend.close(texture);
            texture = null;
        }
        if (selected.isEmpty()) return;

        try {
            BufferedImage source;
            if (selected.equals(BUNDLED)) {
                try (InputStream in = Wallpaper.class.getResourceAsStream(BUNDLED_PATH)) {
                    source = in == null ? null : javax.imageio.ImageIO.read(in);
                }
            } else {
                source = javax.imageio.ImageIO.read(new File(folder(), selected));
            }
            if (source == null) throw new IllegalStateException("not an image the game's Java can read");

            double scale = Math.min(1, (double) MAX_SIDE / Math.max(source.getWidth(), source.getHeight()));
            int w = Math.max(1, (int) Math.round(source.getWidth() * scale)), h = Math.max(1, (int) Math.round(source.getHeight() * scale));
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, w, h, null);
            g.dispose();

            int[] argb = image.getRGB(0, 0, w, h, null, 0, w);
            texture = Backend.texture("wallpaper", w, h, argb);
            imageW = w;
            imageH = h;
        } catch (Throwable t) {
            // A broken file, or a Java runtime without image support: carry on without an image
            System.out.println("[" + Client.NAME + "] cannot load background image " + selected + ": " + t);
            texture = null;
        }
    }

    /**
     * Draws the part of the image under one pane. Whatever was drawn before goes out first, so the
     * image lands above the pane's shadow and below everything the pane draws afterwards.
     */
    public static void renderIn(GuiRenderer g, double x, double y, double w, double h, double corner) {
        if (texture == null || opacity <= 0 || w <= 0 || h <= 0) return;
        corner = Math.max(0, Math.min(corner, Math.min(w, h) / 2));

        // Cover: fill the screen, crop what does not fit
        double scale = Math.max(screenW / imageW, screenH / imageH);
        double shownW = imageW * scale, shownH = imageH * scale, left = (screenW - shownW) / 2, top = (screenH - shownH) / 2;

        int rows = corner > 0 ? (int) Math.max(4, Math.ceil(corner / 2)) : 0;
        int quads = rows * 2 + 1;
        float[] xy = new float[quads * 8], uv = new float[quads * 8];
        int[] colors = new int[quads * 4];
        int n = 0;
        int color = (int) Math.round(255 * Math.min(1, opacity)) << 24 | 0xFFFFFF;

        for (int i = 0; i < quads; i++) {
            double y0, y1, inset;
            if (i < rows) {
                y0 = y + corner * i / rows;
                y1 = y + corner * (i + 1) / rows;
                inset = inset(corner, corner - (y0 + y1) / 2 + y);
            } else if (i == rows) {
                y0 = y + corner;
                y1 = y + h - corner;
                inset = 0;
            } else {
                int k = i - rows - 1;
                y0 = y + h - corner + corner * k / rows;
                y1 = y + h - corner + corner * (k + 1) / rows;
                inset = inset(corner, (y0 + y1) / 2 - (y + h - corner));
            }
            double x0 = x + inset, x1 = x + w - inset;
            double[][] points = {{x0, y0}, {x0, y1}, {x1, y1}, {x1, y0}};
            for (double[] p : points) {
                xy[n * 2] = (float) p[0];
                xy[n * 2 + 1] = (float) p[1];
                uv[n * 2] = (float) ((p[0] - left) / shownW);
                uv[n * 2 + 1] = (float) ((p[1] - top) / shownH);
                colors[n++] = color;
            }
        }

        g.scissorStart(x, y, w, h);
        g.submit(texture, xy, uv, colors, n, (float) x, (float) y, (float) (x + w), (float) (y + h));
        g.scissorEnd();
    }

    /** How far a row sits in from the side of a rounded corner, {@code d} being its distance from the corner's centre line. */
    private static double inset(double corner, double d) {
        d = Math.max(0, Math.min(corner, d));
        return corner - Math.sqrt(corner * corner - d * d);
    }
}

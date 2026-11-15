import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * Bakes Comfortaa into glyph atlases, one PNG per text colour (the Canvas cannot tint an image).
 * Usage: FontAtlas <font.ttf> <output dir> <RRGGBB>...
 * Grid of 16 x 6 cells for ASCII 32..126; metrics.txt holds the advance of each glyph in atlas pixels.
 */
public class FontAtlas {
    static final int CELL_W = 48, CELL_H = 56, SIZE = 40, BASE = 42;
    public static void main(String[] a) throws Exception {
        Font font = Font.createFont(Font.TRUETYPE_FONT, new File(a[0])).deriveFont(Font.BOLD, (float) SIZE);
        File out = new File(a[1]); out.mkdirs();
        StringBuilder metrics = new StringBuilder();
        for (int i = 2; i < a.length; i++) {
            int rgb = Integer.parseInt(a[i], 16);
            BufferedImage img = new BufferedImage(CELL_W * 16, CELL_H * 6, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setFont(font); g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            for (int ch = 32; ch < 127; ch++) {
                int k = ch - 32;
                g.drawString(String.valueOf((char) ch), k % 16 * CELL_W + 4, k / 16 * CELL_H + BASE);
                if (i == 2) metrics.append(fm.charWidth(ch)).append(ch == 126 ? "\n" : " ");
            }
            // Same colour everywhere, glyph coverage in alpha: no dark fringe whichever way the host filters.
            for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) img.setRGB(x, y, (img.getRGB(x, y) >>> 24) << 24 | rgb);
            javax.imageio.ImageIO.write(img, "png", new File(out, a[i].toLowerCase() + ".png"));
        }
        Files.writeString(new File(out, "metrics.txt").toPath(), metrics);
    }
}

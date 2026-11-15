package aicoder.assist.gfx;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Stands in for Meteor's GuiRenderer on top of the unmodified game: colored quads and triangles in
 * framebuffer pixels, scissors, and tasks posted to run after the current layer.
 *
 * <p>Like Meteor's, everything drawn inside one scissor goes out as one layer of shapes followed by the
 * posted tasks (which is where text is drawn), so text always lands on top of the shapes of its layer.
 */
public final class GuiRenderer {
    public final GuiTheme theme = new GuiTheme(this);

    private boolean drawing;

    private float[] xy = new float[1 << 14];
    private int[] colors = new int[1 << 13];
    private int vertices;
    private float minX, minY, maxX, maxY;

    private final Deque<double[]> scissors = new ArrayDeque<>();
    private List<Runnable> posts = new ArrayList<>();
    private final List<Runnable> absolutePosts = new ArrayList<>();

    /** Starts a frame. {@code guiScale} is how many framebuffer pixels one of the game's GUI units covers. */
    public void begin(GuiGraphics graphics, double guiScale) {
        Backend.begin(graphics, guiScale);
        drawing = true;
        scissors.clear();
        resetBounds();
    }

    public void end() {
        flush();
        for (int i = 0; i < absolutePosts.size(); i++) absolutePosts.get(i).run();
        absolutePosts.clear();
        flush();
        drawing = false;
        Backend.end();
    }

    // Shapes

    public void quad(double x, double y, double width, double height, Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        if (width <= 0 || height <= 0) return;
        vertex(x, y, topLeft);
        vertex(x, y + height, bottomLeft);
        vertex(x + width, y + height, bottomRight);
        vertex(x + width, y, topRight);
    }

    public void quad(double x, double y, double width, double height, Color left, Color right) {
        quad(x, y, width, height, left, right, right, left);
    }

    public void quad(double x, double y, double width, double height, Color color) {
        quad(x, y, width, height, color, color, color, color);
    }

    public void triangle(double x1, double y1, double x2, double y2, double x3, double y3, Color color) {
        vertex(x1, y1, color);
        vertex(x2, y2, color);
        vertex(x3, y3, color);
        vertex(x3, y3, color);
    }

    private void vertex(double x, double y, Color color) {
        if (vertices == colors.length) {
            colors = Arrays.copyOf(colors, colors.length * 2);
            xy = Arrays.copyOf(xy, xy.length * 2);
        }
        float fx = (float) x, fy = (float) y;
        xy[vertices * 2] = fx;
        xy[vertices * 2 + 1] = fy;
        colors[vertices++] = color.argb();

        if (fx < minX) minX = fx;
        if (fy < minY) minY = fy;
        if (fx > maxX) maxX = fx;
        if (fy > maxY) maxY = fy;
    }

    private void resetBounds() {
        minX = minY = Float.MAX_VALUE;
        maxX = maxY = -Float.MAX_VALUE;
    }

    // Layers

    /** Run when the current scissor ends (or the frame, if none is open), after its shapes have gone out. */
    public void post(Runnable task) {
        posts.add(task);
    }

    /** Run at the very end of the frame, on top of everything. */
    public void absolutePost(Runnable task) {
        absolutePosts.add(task);
    }

    public void scissorStart(double x, double y, double width, double height) {
        flush();

        double[] parent = scissors.peek();
        double x0 = x, y0 = y, x1 = x + width, y1 = y + height;
        if (parent != null) {
            x0 = Math.max(x0, parent[0]);
            y0 = Math.max(y0, parent[1]);
            x1 = Math.min(x1, parent[2]);
            y1 = Math.min(y1, parent[3]);
        }
        scissors.push(new double[]{x0, y0, Math.max(x0, x1), Math.max(y0, y1)});
    }

    public void scissorEnd() {
        flush();
        scissors.pop();
    }

    /** Sends the shapes gathered so far to the game as one element, then runs what was posted behind them. */
    private void flush() {
        if (vertices > 0) {
            submit(null, Arrays.copyOf(xy, vertices * 2), null, Arrays.copyOf(colors, vertices), vertices,
                minX, minY, maxX, maxY);
            vertices = 0;
            resetBounds();
        }

        if (!posts.isEmpty()) {
            List<Runnable> run = posts;
            posts = new ArrayList<>();
            for (Runnable task : run) task.run();
            // A posted task may itself have drawn shapes or posted more
            if (vertices > 0 || !posts.isEmpty()) flush();
        }
    }

    /** One batch of quads, plain or textured (a handle from {@link Backend#texture}), clipped to the open scissor. */
    void submit(Object texture, float[] xy, float[] uv, int[] colors, int count, float x0, float y0, float x1, float y1) {
        if (!drawing || count == 0) return;
        Backend.draw(texture, xy, uv, colors, count, scissors.peek(), x0, y0, x1, y1);
    }
}

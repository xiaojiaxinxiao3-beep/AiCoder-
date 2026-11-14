package aicoder.assist.gfx;

import aicoder.assist.Names;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;

import java.lang.reflect.Field;
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

    private static Field stateField;

    private GuiGraphics graphics;
    private GuiRenderState state;
    private Matrix3x2f pose;
    private double guiScale = 1;

    private float[] xy = new float[1 << 14];
    private int[] colors = new int[1 << 13];
    private int vertices;
    private float minX, minY, maxX, maxY;

    private final Deque<double[]> scissors = new ArrayDeque<>();
    private List<Runnable> posts = new ArrayList<>();
    private final List<Runnable> absolutePosts = new ArrayList<>();

    /** Starts a frame. {@code guiScale} is how many framebuffer pixels one of the game's GUI units covers. */
    public void begin(GuiGraphics graphics, double guiScale) {
        this.graphics = graphics;
        this.guiScale = guiScale;
        this.pose = new Matrix3x2f(graphics.pose()).scale((float) (1 / guiScale));
        this.state = stateOf(graphics);
        scissors.clear();
        resetBounds();
    }

    public void end() {
        flush();
        for (int i = 0; i < absolutePosts.size(); i++) absolutePosts.get(i).run();
        absolutePosts.clear();
        flush();
        graphics = null;
        state = null;
    }

    private static GuiRenderState stateOf(GuiGraphics graphics) {
        try {
            if (stateField == null) {
                stateField = GuiGraphics.class.getDeclaredField(Names.shared().get("GuiGraphics.guiRenderState"));
                stateField.setAccessible(true);
            }
            return (GuiRenderState) stateField.get(graphics);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
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
            submit(RenderPipelines.GUI, TextureSetup.noTexture(), Arrays.copyOf(xy, vertices * 2), null, Arrays.copyOf(colors, vertices), vertices,
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

    void submit(RenderPipeline pipeline, TextureSetup texture, float[] xy, float[] uv, int[] colors, int count, float x0, float y0, float x1, float y1) {
        if (state == null || count == 0) return;

        ScreenRectangle bounds = rectangle(x0, y0, x1, y1);
        ScreenRectangle scissor = null;
        double[] s = scissors.peek();
        if (s != null) {
            scissor = rectangle(s[0], s[1], s[2], s[3]);
            bounds = bounds.intersection(scissor);
            if (bounds == null) return;
        }

        state.submitGuiElement(new Mesh(pipeline, texture, pose, xy, uv, colors, count, scissor, bounds.transformMaxBounds(graphics.pose())));
    }

    /** Framebuffer pixels to the game's GUI units, rounded outwards. */
    private ScreenRectangle rectangle(double x0, double y0, double x1, double y1) {
        int left = (int) Math.floor(x0 / guiScale), top = (int) Math.floor(y0 / guiScale);
        int right = (int) Math.ceil(x1 / guiScale), bottom = (int) Math.ceil(y1 / guiScale);
        return new ScreenRectangle(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }
}

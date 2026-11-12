package aicoder.glass.gui;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.textures.FilterMode;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The picture seen through the GUI's glass. It is laid out to cover the screen but only shows inside
 * the panes, so the columns look like windows onto one image. Wallpapers come from two places: the ones packed into the addon, and any
 * PNG or JPG the player drops into {@code meteor-client/aicoder-wallpapers/} in the game folder.
 */
public final class Wallpaper {
    private Wallpaper() {
    }

    public static final String NONE = "None";
    public static final String DEFAULT = "Crimson Scythe";

    /** Display name, then file name under {@code assets/aicoder-glass/wallpapers/}. */
    private static final String[][] BUNDLED = {
        {"Crimson Scythe", "crimson_scythe.jpg"}
    };

    /** Shared by both liquid glass addons, so a picture added once shows up in each. */
    public static final File FOLDER = new File(MeteorClient.FOLDER, "aicoder-wallpapers");

    private static String loadedName;
    private static Texture texture;

    /** Everything the wallpaper setting can be set to. */
    public static String[] names() {
        List<String> names = new ArrayList<>();
        names.add(NONE);
        for (String[] bundled : BUNDLED) names.add(bundled[0]);

        File[] files = FOLDER.listFiles((dir, name) -> isImage(name));
        if (files != null) {
            Arrays.sort(files);
            for (File file : files) names.add(file.getName());
        }
        return names.toArray(new String[0]);
    }

    private static boolean isImage(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    /** Creates the folder on first use, so the player has somewhere obvious to put pictures. */
    public static void ensureFolder() {
        if (!FOLDER.exists()) FOLDER.mkdirs();
    }

    // Set once per frame by the theme; every pane drawn that frame shows its own part of the same picture
    private static Texture current;
    private static double opacity;
    private static double imageX, imageY, imageW, imageH;

    private static Renderer2D mesh;
    private static final Color TINT = new Color(255, 255, 255, 255);

    /**
     * Picks the wallpaper for this frame and lays it out as if it covered the whole screen (scaled to
     * cover, cropped evenly). Nothing is drawn here; panes reveal the part that falls behind them.
     */
    public static void frame(String name, double opacity, double screenW, double screenH) {
        current = get(name);
        Wallpaper.opacity = opacity;
        if (current == null) return;

        double scale = Math.max(screenW / current.getWidth(), screenH / current.getHeight());
        imageW = current.getWidth() * scale;
        imageH = current.getHeight() * scale;
        imageX = (screenW - imageW) / 2;
        imageY = (screenH - imageH) / 2;
    }

    /**
     * Shows the wallpaper inside one pane with rounded corners of radius {@code corner}. The picture is
     * drawn immediately, so whatever the pane draws afterwards (tint, sheen, rim) ends up on top of it.
     */
    public static void renderIn(GuiRenderer renderer, double x, double y, double w, double h, double corner) {
        if (current == null || opacity <= 0 || w <= 0 || h <= 0) return;

        Texture tex = current;
        double r = Math.max(0, Math.min(corner, Math.min(w, h) / 2));
        double alpha = opacity;

        // Own scissor: its queued task runs as soon as it ends, ahead of the pane's own geometry
        renderer.scissorStart(x, y, w, h);
        renderer.post(() -> {
            if (mesh == null) mesh = new Renderer2D(true);
            TINT.a = (int) Math.round(255 * alpha);

            mesh.begin();

            // Rounded corners out of thin horizontal strips; the pane's rim covers the stair-steps
            int steps = r < 1 ? 0 : (int) Math.max(4, Math.min(14, Math.ceil(r / 2)));
            for (int i = 0; i < steps; i++) {
                double y0 = r * i / steps, y1 = r * (i + 1) / steps;
                double d = r - (y0 + y1) / 2;
                double inset = r - Math.sqrt(Math.max(0, r * r - d * d));
                strip(x + inset, y + y0, w - inset * 2, y1 - y0);
                strip(x + inset, y + h - y1, w - inset * 2, y1 - y0);
            }
            strip(x, y + r, w, h - r * 2);

            mesh.end();
            mesh.render(tex.getTextureView(), tex.getSampler());
        });
        renderer.scissorEnd();
    }

    /** One piece of the picture, with the texture coordinates of exactly that piece of the screen. */
    private static void strip(double x, double y, double w, double h) {
        if (w <= 0 || h <= 0) return;

        double u0 = (x - imageX) / imageW, u1 = (x + w - imageX) / imageW;
        double v0 = (y - imageY) / imageH, v1 = (y + h - imageY) / imageH;
        mesh.texQuad(x, y, w, h, 0, u0, v0, u1, v1, TINT);
    }

    private static Texture get(String name) {
        if (name == null || name.equals(NONE)) {
            release();
            loadedName = name;
            return null;
        }

        if (!name.equals(loadedName)) {
            release();
            loadedName = name;
            texture = load(name);
        }
        return texture;
    }

    private static void release() {
        if (texture != null) {
            texture.close();
            texture = null;
        }
    }

    private static Texture load(String name) {
        try (InputStream in = open(name)) {
            if (in == null) return null;

            ByteBuffer data = TextureUtil.readResource(in);
            data.rewind();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer components = stack.mallocInt(1);

                ByteBuffer image = STBImage.stbi_load_from_memory(data, width, height, components, 4);
                if (image == null) return null;

                Texture loaded = new Texture(width.get(0), height.get(0), GpuFormat.RGBA8_UNORM, FilterMode.LINEAR, FilterMode.LINEAR);
                loaded.upload(image);
                STBImage.stbi_image_free(image);
                return loaded;
            } finally {
                MemoryUtil.memFree(data);
            }
        } catch (IOException | RuntimeException e) {
            // A broken or oversized picture should cost the wallpaper, not the GUI
            return null;
        }
    }

    private static InputStream open(String name) throws IOException {
        for (String[] bundled : BUNDLED) {
            if (bundled[0].equals(name)) {
                return Wallpaper.class.getResourceAsStream("/assets/aicoder-glass/wallpapers/" + bundled[1]);
            }
        }

        File file = new File(FOLDER, name);
        // The name comes from a saved setting; only accept a plain file inside the wallpaper folder
        if (!isImage(name) || !file.isFile() || !FOLDER.equals(file.getParentFile())) return null;
        return new FileInputStream(file);
    }
}

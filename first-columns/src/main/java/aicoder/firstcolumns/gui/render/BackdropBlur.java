package aicoder.firstcolumns.gui.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;

/**
 * Blurred copy of whatever is on screen, for the frosted glass.
 *
 * <p>Right before a batch with glass in it is drawn, the frame so far is run through dual kawase down / up
 * sampling passes (the same scheme and strength table as Meteor's Blur module, drawn by this addon's own
 * shader). The glass shader then samples the result, so only the area behind a window looks blurred.</p>
 */
public final class BackdropBlur {
    /** Iterations and sample offset per strength level, the same table Meteor's Blur module uses. */
    private static final int[] ITERATIONS = {1, 1, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5};
    private static final float[] OFFSETS = {1.25f, 2.25f, 2.0f, 3.0f, 4.25f, 2.5f, 3.25f, 4.25f, 5.5f, 3.25f, 4.0f, 5.0f, 6.0f, 7.25f, 8.25f, 4.5f, 5.25f, 6.25f, 7.25f, 8.5f};

    private static final int LEVELS = 6;

    private static final GpuTextureView[] fbos = new GpuTextureView[LEVELS];
    private static int width, height;

    private BackdropBlur() {
    }

    /** The texture the glass shader samples. Always valid on the render thread, blurred or not. */
    public static GpuTextureView texture() {
        ensureTargets();
        return fbos[0];
    }

    public static GpuSampler sampler() {
        return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
    }

    /** Blurs the current contents of the main render target into {@link #texture()}. Strength is 1..20. */
    public static void capture(int strength) {
        ensureTargets();

        int level = Math.max(1, Math.min(ITERATIONS.length, strength)) - 1;
        int iterations = ITERATIONS[level];
        float offset = OFFSETS[level];

        GpuTextureView screen = Minecraft.getInstance().gameRenderer.mainRenderTarget().getColorTextureView();

        Sdf.blurPass(fbos[0], screen, false, offset);
        for (int i = 0; i < iterations; i++) Sdf.blurPass(fbos[i + 1], fbos[i], false, offset);
        for (int i = iterations; i >= 1; i--) Sdf.blurPass(fbos[i - 1], fbos[i], true, offset);
    }

    private static void ensureTargets() {
        var window = Minecraft.getInstance().getWindow();
        int w = Math.max(1, window.getWidth()), h = Math.max(1, window.getHeight());
        if (fbos[0] != null && w == width && h == height) return;

        for (int i = 0; i < LEVELS; i++) {
            if (fbos[i] != null) fbos[i].close();

            int level = i;
            int lw = Math.max(1, w >> i), lh = Math.max(1, h >> i);
            var device = RenderSystem.getDevice();
            fbos[i] = device.createTextureView(device.createTexture("AiCoder Blur - " + level, 15, GpuFormat.RGBA8_UNORM, lw, lh, 1, 1));
        }

        width = w;
        height = h;
    }
}

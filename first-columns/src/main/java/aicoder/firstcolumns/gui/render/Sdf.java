package aicoder.firstcolumns.gui.render;

import aicoder.firstcolumns.AiCoderAddon;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import meteordevelopment.meteorclient.gui.renderer.Scissor;
import meteordevelopment.meteorclient.renderer.ExtendedRenderPipelineBuilder;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

/**
 * Shader drawn shapes for the GUI.
 *
 * <p>Each shape is one quad. Its geometry and colours are written into the vertices and the fragment
 * shader ({@code assets/aicoder-first-columns/shaders/shapes.fsh}) works out rounded corners, gradients, strokes,
 * glows, the flowing light, the frosted glass and the aurora per pixel from signed distance functions. All shapes
 * of a GUI batch share one mesh and one draw call, see {@code GuiRendererMixin} for when it is flushed.</p>
 *
 * <p>Usage: {@code Sdf.box(x, y, w, h, radius).fill(color).stroke(1, border).glow(8, light).draw();}.
 * The returned {@link Shape} is a reused scratch object, finish it with {@link Shape#draw()} before starting
 * the next one. Colours are packed ARGB ints, see {@link Palette}.</p>
 */
public final class Sdf {
    private static final int KIND_BOX = 0, KIND_RADIAL = 1, KIND_AURORA = 2, KIND_GLASS = 3, KIND_CHECK = 4, KIND_BLUR_DOWN = 5, KIND_BLUR_UP = 6;
    private static final int FLAG_FLOW_STROKE = 1, FLAG_FLOW_GLOW = 2;

    public static final VertexFormat FORMAT = VertexFormat.builder(0)
        .addAttribute("Position", GpuFormat.RG32_FLOAT)
        .addAttribute("Local", GpuFormat.RG32_FLOAT)
        .addAttribute("HalfSize", GpuFormat.RG32_FLOAT)
        .addAttribute("Shape", GpuFormat.RG32_FLOAT)
        .addAttribute("Edge", GpuFormat.RG32_FLOAT)
        .addAttribute("Anim", GpuFormat.RG32_FLOAT)
        .addAttribute("Extra", GpuFormat.RG32_FLOAT)
        .addAttribute("ColorA", GpuFormat.RGBA8_UNORM)
        .addAttribute("ColorB", GpuFormat.RGBA8_UNORM)
        .addAttribute("StrokeColor", GpuFormat.RGBA8_UNORM)
        .addAttribute("GlowColor", GpuFormat.RGBA8_UNORM)
        .addAttribute("FlowA", GpuFormat.RGBA8_UNORM)
        .addAttribute("FlowB", GpuFormat.RGBA8_UNORM)
        .build();

    /** Draws the shapes into the game's main render target, which has a depth buffer attached. */
    public static final RenderPipeline PIPELINE = builder("pipeline/shapes")
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .build();

    /**
     * The same shader for the blur passes, which draw into colour only textures. A pipeline that declares a
     * depth state cannot be used without a depth attachment, hence the second pipeline.
     */
    public static final RenderPipeline PASS_PIPELINE = builder("pipeline/shapes_pass")
        .withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_ALL))
        .build();

    private static RenderPipeline.Builder builder(String location) {
        return new ExtendedRenderPipelineBuilder()
            .withLocation(Identifier.fromNamespaceAndPath("aicoder-first-columns", location))
            .withBindGroupLayout(BindGroupLayout.builder()
                .withUniform("MeshData", UniformType.UNIFORM_BUFFER)
                .build())
            .withBindGroupLayout(BindGroupLayout.builder()
                .withUniform("u_Backdrop", UniformType.COMBINED_IMAGE_SAMPLER)
                .build())
            .withVertexBinding(0, FORMAT).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withVertexShader(Identifier.fromNamespaceAndPath("aicoder-first-columns", "shaders/shapes.vsh"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aicoder-first-columns", "shaders/shapes.fsh"))
            .withCull(false);
    }

    private static final Shape SHAPE = new Shape();
    private static final Shape PASS = new Shape();
    private static final Color TMP = new Color();

    private static MeshBuilder mesh;
    private static MeshBuilder passMesh;
    private static double alpha = 1;

    private static int flowA = 0xFF2FE08F, flowB = 0xFF2F8CFF;
    private static int blurStrength = 8;
    private static boolean glassQueued;

    private Sdf() {
    }

    /**
     * Hands the pipelines to Meteor so they are compiled together with Meteor's own pipelines on every resource
     * reload. Meteor keeps that list private and offers no way to add to it, hence the reflection.
     */
    @SuppressWarnings("unchecked")
    public static void register() {
        try {
            Field field = MeteorRenderPipelines.class.getDeclaredField("PIPELINES");
            field.setAccessible(true);
            List<RenderPipeline> pipelines = (List<RenderPipeline>) field.get(null);
            pipelines.add(PIPELINE);
            pipelines.add(PASS_PIPELINE);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("AiCoder client could not register its GUI shader with Meteor.", e);
        }
    }

    // Batch

    public static void begin() {
        if (mesh == null) mesh = new MeshBuilder(PIPELINE);
        if (!mesh.isBuilding()) {
            mesh.begin();
            glassQueued = false;
        }
        mesh.alpha = alpha;
    }

    public static void flush(Scissor scissor) {
        if (mesh == null || !mesh.isBuilding()) return;

        mesh.end();
        if (mesh.getIndicesCount() == 0) return;

        // Glass samples the frame as it is right now, blurred
        if (glassQueued) BackdropBlur.capture(blurStrength);
        glassQueued = false;

        if (scissor != null) scissor.push();

        try {
            MeshRenderer.begin()
                .attachments(Minecraft.getInstance().gameRenderer.mainRenderTarget())
                .pipeline(PIPELINE)
                .mesh(mesh)
                .sampler("u_Backdrop", BackdropBlur.texture(), BackdropBlur.sampler())
                .end();
        } finally {
            if (scissor != null) scissor.pop();
        }
    }

    /**
     * One pass of the backdrop blur: fills {@code target} with a blurred copy of {@code source}. It goes
     * through the same pipeline as the shapes, as a single quad covering the target.
     */
    static void blurPass(GpuTextureView target, GpuTextureView source, boolean up, float offset) {
        if (passMesh == null) passMesh = new MeshBuilder(PASS_PIPELINE);

        var window = Minecraft.getInstance().getWindow();
        double w = window.getWidth(), h = window.getHeight();

        Shape s = PASS.reset(up ? KIND_BLUR_UP : KIND_BLUR_DOWN, 0, 0, w, h, offset);
        s.strokeWidth = 0.5 / source.getWidth(0);
        s.spread = 0.5 / source.getHeight(0);

        passMesh.begin();
        passMesh.alpha = 1;
        s.emit(passMesh, 0);

        MeshRenderer.begin()
            .attachments(target, null)
            .pipeline(PASS_PIPELINE)
            .mesh(passMesh)
            .sampler("u_Backdrop", source, BackdropBlur.sampler())
            .end();
    }

    public static void setAlpha(double a) {
        alpha = a;
        if (mesh != null) mesh.alpha = a;
    }

    /** The two colours every drifting light, glow and the aurora are made of. */
    public static void setFlowColors(int first, int second) {
        flowA = first | 0xFF000000;
        flowB = second | 0xFF000000;
    }

    /** How strongly glass blurs what is behind it, 1..20. */
    public static void setBlurStrength(int strength) {
        blurStrength = strength;
    }

    // Shapes

    /** Rounded box. A radius of half the size gives a circle or a pill. */
    public static Shape box(double x, double y, double w, double h, double radius) {
        return SHAPE.reset(KIND_BOX, x, y, w, h, radius);
    }

    public static Shape circle(double cx, double cy, double radius) {
        return SHAPE.reset(KIND_BOX, cx - radius, cy - radius, radius * 2, radius * 2, radius);
    }

    /**
     * Frosted glass window. It always blurs what is behind it. Everything else is optional and layered on
     * top: a dark base, the flowing colours, a faint tint, the border and the outer glow.
     *
     * @param u            size of one design pixel on screen
     * @param base         colour laid straight over the blur, usually a translucent dark blue, 0 for none
     * @param flowOpacity  how strongly the flowing colours fill the pane, 0..1
     * @param tint         opacity of the diagonal tint between the two colours, 0..1
     * @param border       border colour; with {@code coloured} its hue comes from the flowing light instead
     * @param glow         strength of the outer glow, 0 for none
     * @param coloured     whether border takes the flowing colours
     */
    public static void glass(double x, double y, double w, double h, double radius, double u, int base, double flowOpacity, double tint, int border, double glow, boolean coloured, double seed) {
        Shape s = SHAPE.reset(KIND_GLASS, x, y, w, h, radius);
        s.colorA = base;
        s.colorB = Palette.rgba(0, 0, 0, 0.20);
        s.strokeWidth = Math.max(1, u);
        s.strokeColor = border;
        s.flags = coloured ? FLAG_FLOW_STROKE : 0;
        s.spread = 110 * u;
        s.glowColor = Palette.rgba(255, 255, 255, glow);
        s.flow = flowOpacity;
        s.angle = tint;
        s.seed = seed;
        s.draw();

        glassQueued = true;
    }

    /** The three aurora curtains across the whole screen. */
    public static void aurora(double screenW, double screenH, double opacity) {
        Shape s = SHAPE.reset(KIND_AURORA, 0, 0, screenW, screenH, 0);
        s.colorA = Palette.rgba(255, 255, 255, opacity);
        s.draw();
    }

    /** Check mark filling a square, designed on an 18 pixel box. */
    public static void check(double x, double y, double size, int color) {
        Shape s = SHAPE.reset(KIND_CHECK, x, y, size, size, 0);
        s.colorA = color;
        s.draw();
    }

    public static final class Shape {
        private int kind, flags;
        private double x, y, w, h, radius;
        private int colorA, colorB, strokeColor, glowColor;
        private double strokeWidth, spread, angle, flow, seed;

        private Shape reset(int kind, double x, double y, double w, double h, double radius) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.radius = radius;

            flags = 0;
            colorA = colorB = strokeColor = glowColor = 0;
            strokeWidth = spread = angle = flow = seed = 0;
            return this;
        }

        public Shape fill(int color) {
            colorA = colorB = color;
            return this;
        }

        /** Linear gradient. 0 degrees runs left to right, 90 top to bottom. */
        public Shape gradient(int from, int to, double degrees) {
            colorA = from;
            colorB = to;
            angle = Math.toRadians(degrees);
            return this;
        }

        /** Radial gradient from the upper middle to the rim. */
        public Shape radial(int center, int rim) {
            kind = KIND_RADIAL;
            colorA = center;
            colorB = rim;
            return this;
        }

        /** Line just inside the edge. */
        public Shape stroke(double width, int color) {
            strokeWidth = width;
            strokeColor = color;
            return this;
        }

        /** Soft light outside the edge, like a CSS box-shadow without offset. */
        public Shape glow(double spread, int color) {
            this.spread = spread;
            glowColor = color;
            return this;
        }

        /** Lays the drifting green / cyan / blue light under the fill. */
        public Shape flowFill(double opacity) {
            flow = opacity;
            return this;
        }

        /** Colours the stroke with the drifting light, keeping the stroke's alpha. */
        public Shape flowStroke() {
            flags |= FLAG_FLOW_STROKE;
            return this;
        }

        /** Colours the glow with the drifting light, keeping the glow's alpha. */
        public Shape flowGlow() {
            flags |= FLAG_FLOW_GLOW;
            return this;
        }

        /** Offsets the drifting light so neighbours do not move in sync. Blue first starts its colour ramp at blue. */
        public Shape seed(double seed, boolean blueFirst) {
            this.seed = (seed - Math.floor(seed)) + (blueFirst ? 10 : 0);
            return this;
        }

        public void draw() {
            MeshBuilder m = mesh;
            if (m == null || !m.isBuilding()) {
                // Outside of a GUI batch there is nowhere to draw to
                return;
            }

            emit(m, spread + 1.5);
        }

        private void emit(MeshBuilder m, double pad) {
            double hw = w / 2, hh = h / 2;
            double cx = x + hw, cy = y + hh;
            double time = Anim.seconds() % 32400;

            m.ensureQuadCapacity();
            int i1 = vertex(m, cx, cy, -hw - pad, -hh - pad, hw, hh, time);
            int i2 = vertex(m, cx, cy, -hw - pad, hh + pad, hw, hh, time);
            int i3 = vertex(m, cx, cy, hw + pad, hh + pad, hw, hh, time);
            int i4 = vertex(m, cx, cy, hw + pad, -hh - pad, hw, hh, time);
            m.quad(i1, i2, i3, i4);
        }

        private int vertex(MeshBuilder m, double cx, double cy, double lx, double ly, double hw, double hh, double time) {
            return m.vec2(cx + lx, cy + ly)
                .vec2(lx, ly)
                .vec2(hw, hh)
                .vec2(radius, kind + 10 * flags)
                .vec2(strokeWidth, spread)
                .vec2(time, seed)
                .vec2(angle, flow)
                .color(col(colorA))
                .color(col(colorB))
                .color(col(strokeColor))
                .color(col(glowColor))
                .color(col(flowA))
                .color(col(flowB))
                .next();
        }
    }

    private static Color col(int argb) {
        return TMP.set((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
    }

    static {
        AiCoderAddon.LOG.debug("AiCoder GUI shader pipeline created");
    }
}

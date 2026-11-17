#version 330
#extension GL_ARB_separate_shader_objects : require

// Every shape of the AiCoder GUI, evaluated per pixel from signed distance functions:
// rounded boxes and circles with gradient fills, strokes and soft outer glows, the
// flowing edge light, the frosted glass window, the aurora curtains and the check mark.

layout (location = 0) in vec4 v_Box;     // local.xy, halfSize.xy
layout (location = 1) in vec4 v_Shape;   // radius, kind + 10 * flags, stroke width, glow spread
layout (location = 2) in vec4 v_Anim;    // time, seed, gradient angle, flow amount
layout (location = 3) in vec4 v_ColorA;
layout (location = 4) in vec4 v_ColorB;
layout (location = 5) in vec4 v_Stroke;
layout (location = 6) in vec4 v_Glow;
layout (location = 7) in vec4 v_FlowA;   // the two user colours
layout (location = 8) in vec4 v_FlowB;
layout (location = 9) in vec2 v_Screen;  // 0..1 across the screen

uniform sampler2D u_Backdrop;            // blurred copy of the frame behind the GUI

layout (location = 0) out vec4 color;

const float TAU = 6.28318530718;

const int KIND_BOX = 0;      // linear gradient from colour A to colour B
const int KIND_RADIAL = 1;   // radial gradient, A in the upper middle to B at the rim
const int KIND_AURORA = 2;   // full screen aurora curtains
const int KIND_GLASS = 3;    // the window: blurred backdrop, glow, flowing colour, tint, highlight
const int KIND_CHECK = 4;    // check mark
const int KIND_BLUR_DOWN = 5; // one down sampling pass of the backdrop blur
const int KIND_BLUR_UP = 6;   // one up sampling pass of the backdrop blur

const int FLAG_FLOW_STROKE = 1;
const int FLAG_FLOW_GLOW = 2;

// Length of one loop of the flowing light and the aurora, in seconds
const float FLOW_PERIOD = 9.0;

// A few stacked sine waves: loops, but reads as irregular drifting. Nothing spins.
float flow(float p, float s) {
    return 0.54 * sin(TAU * (p + s))
         + 0.30 * sin(TAU * (2.0 * p + s * 1.7) + 1.1)
         + 0.16 * sin(TAU * (3.0 * p - s * 0.6) + 2.3);
}

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

float sdSegment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a, ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}

// Straight alpha "top over bottom"
vec4 over(vec4 top, vec4 bottom) {
    float a = top.a + bottom.a * (1.0 - top.a);
    vec3 c = (top.rgb * top.a + bottom.rgb * bottom.a * (1.0 - top.a)) / max(a, 0.00001);
    return vec4(c, a);
}

// Five evenly spaced stops: a, b, c, a, b
vec3 gradient5(float u, vec3 a, vec3 b, vec3 c) {
    u = clamp(u, 0.0, 1.0) * 4.0;
    if (u < 1.0) return mix(a, b, u);
    if (u < 2.0) return mix(b, c, u - 1.0);
    if (u < 3.0) return mix(c, a, u - 2.0);
    return mix(a, b, u - 3.0);
}

// The design's oversized diagonal gradient drifting behind a box. The colours wander
// along the edges instead of circling. It runs between the two user colours; a seed of
// 10 or more starts the ramp at the second one.
vec3 flowColor(vec2 local, vec2 halfSize, float time, float seed) {
    bool blueFirst = seed >= 10.0;
    float s = blueFirst ? seed - 10.0 : seed;
    float p = fract(time / FLOW_PERIOD);

    float fx = clamp(0.5 + flow(p, s) * 0.55, 0.0, 1.0);
    float fy = clamp(0.5 + flow(p, s + 0.5) * 0.55, 0.0, 1.0);

    vec2 size = halfSize * 2.0;
    vec2 q = local + halfSize;
    float u = ((q.x + fx * 2.0 * size.x) * 0.866 + (q.y + fy * 2.0 * size.y) * 0.5) / (3.0 * (size.x * 0.866 + size.y * 0.5));

    vec3 a = v_FlowA.rgb, b = v_FlowB.rgb, m = mix(a, b, 0.5);
    return blueFirst ? gradient5(u, b, a, m) : gradient5(u, a, m, b);
}

float glowFalloff(float d, float spread) {
    if (spread <= 0.0) return 0.0;
    float t = 1.0 - clamp(d / spread, 0.0, 1.0);
    return t * t * t;
}

// Three tall curtains on a 960 x 600 stage that sway, lean a little and breathe in brightness.
vec4 aurora(vec2 local, vec2 halfSize, float time) {
    const float X[3] = float[3](70.0, 420.0, 690.0);
    const float SEED[3] = float[3](0.13, 0.57, 0.81);
    vec3 TINT[3] = vec3[3](mix(v_FlowA.rgb, v_FlowB.rgb, 0.4), v_FlowA.rgb, v_FlowB.rgb);

    vec2 stage = (local + halfSize) / (halfSize * 2.0) * vec2(960.0, 600.0);
    float p = fract(time / FLOW_PERIOD);
    vec4 result = vec4(0.0);

    for (int i = 0; i < 3; i++) {
        float sway = flow(p, SEED[i]);
        float stretch = 1.0 + 0.11 * flow(p, SEED[i] + 0.5);
        float opacity = max(0.04, 0.22 + 0.15 * flow(p, SEED[i] + 0.3));

        // 900 tall, starts 160 above the stage, scales around 30% of its height
        float originY = -160.0 + 270.0;
        float v = (stage.y - (originY - 270.0 * stretch)) / (900.0 * stretch);
        float vertical = smoothstep(0.0, 1.0, v < 0.44 ? v / 0.44 : (0.86 - v) / 0.42);

        float centerX = X[i] + 170.0 + sway * 120.0 + (stage.y - originY) * tan(radians(sway * 13.0));
        float dx = (stage.x - centerX) / 125.0;
        float horizontal = exp(-dx * dx);

        result = over(vec4(TINT[i], 0.5 * opacity * 1.6 * vertical * horizontal), result);
    }

    return result;
}

void main() {
    vec2 local = v_Box.xy;
    vec2 halfSize = v_Box.zw;
    float radius = v_Shape.x;
    float strokeWidth = v_Shape.z;
    float spread = v_Shape.w;
    float time = v_Anim.x;
    float seed = v_Anim.y;
    float angle = v_Anim.z;
    float flowAmount = v_Anim.w;

    int code = int(v_Shape.y + 0.5);
    int flags = code / 10;
    int kind = code - flags * 10;

    // Dual kawase blur passes. The quad covers the whole target, u_Backdrop is the previous level,
    // the stroke / glow slots carry half a texel of that level and the radius slot the sample offset.
    if (kind == KIND_BLUR_DOWN) {
        vec2 o = v_Shape.zw * v_Shape.x;
        vec3 c = texture(u_Backdrop, v_Screen).rgb * 4.0
            + texture(u_Backdrop, v_Screen - o).rgb
            + texture(u_Backdrop, v_Screen + o).rgb
            + texture(u_Backdrop, v_Screen + vec2(o.x, -o.y)).rgb
            + texture(u_Backdrop, v_Screen - vec2(o.x, -o.y)).rgb;

        color = vec4(c / 8.0, 1.0);
        return;
    }

    if (kind == KIND_BLUR_UP) {
        vec2 o = v_Shape.zw * v_Shape.x;
        vec3 c = texture(u_Backdrop, v_Screen + vec2(-o.x * 2.0, 0.0)).rgb
            + texture(u_Backdrop, v_Screen + vec2(-o.x, o.y)).rgb * 2.0
            + texture(u_Backdrop, v_Screen + vec2(0.0, o.y * 2.0)).rgb
            + texture(u_Backdrop, v_Screen + o).rgb * 2.0
            + texture(u_Backdrop, v_Screen + vec2(o.x * 2.0, 0.0)).rgb
            + texture(u_Backdrop, v_Screen + vec2(o.x, -o.y)).rgb * 2.0
            + texture(u_Backdrop, v_Screen + vec2(0.0, -o.y * 2.0)).rgb
            + texture(u_Backdrop, v_Screen - o).rgb * 2.0;

        color = vec4(c / 12.0, 1.0);
        return;
    }

    if (kind == KIND_AURORA) {
        color = aurora(local, halfSize, time);
        color.a *= v_ColorA.a;
        if (color.a < 0.002) discard;
        return;
    }

    if (kind == KIND_CHECK) {
        float k = halfSize.x * 2.0 / 18.0;
        vec2 p = (local + halfSize) / k;
        float d = min(sdSegment(p, vec2(4.4, 9.4), vec2(7.7, 12.6)), sdSegment(p, vec2(7.7, 12.6), vec2(13.6, 5.6))) * k;

        color = vec4(v_ColorA.rgb, v_ColorA.a * clamp(1.15 * k - d + 0.5, 0.0, 1.0));
        if (color.a < 0.002) discard;
        return;
    }

    radius = min(radius, min(halfSize.x, halfSize.y));
    float d = sdRoundBox(local, halfSize, radius);
    float inside = clamp(0.5 - d, 0.0, 1.0);
    vec3 flowing = flowColor(local, halfSize, time, seed);

    // Fill
    vec4 fill;

    if (kind == KIND_RADIAL) {
        float t = clamp(length(local - vec2(0.0, -halfSize.y * 0.2)) / (halfSize.x * 1.2), 0.0, 1.0);
        fill = mix(v_ColorA, v_ColorB, t);
    } else if (kind == KIND_GLASS) {
        // Frosted glass: the blurred frame behind the window, a little more saturated
        vec3 backdrop = texture(u_Backdrop, v_Screen).rgb;
        float luma = dot(backdrop, vec3(0.299, 0.587, 0.114));
        backdrop = clamp(mix(vec3(luma), backdrop, 1.35), 0.0, 1.0);

        // On top of it, all optional: dark base, flowing colour, faint diagonal tint.
        // With all three at zero the pane is clear and only blurs.
        float g = clamp(((local.x + halfSize.x) * 0.5 + (local.y + halfSize.y) * 0.866) / (halfSize.x + halfSize.y * 1.732), 0.0, 1.0);
        vec4 tint = vec4(mix(v_FlowA.rgb, v_FlowB.rgb, g), angle);

        fill = over(tint, over(vec4(flowing, flowAmount), over(v_ColorA, vec4(backdrop, 1.0))));

        // Inner highlight along the top
        float u = max(1.0, strokeWidth);
        float top = local.y + halfSize.y;
        float band = clamp(top - u + 0.5, 0.0, 1.0) * clamp(2.0 * u - top + 0.5, 0.0, 1.0) * step(abs(local.x), halfSize.x - radius);
        fill = over(vec4(1.0, 1.0, 1.0, v_ColorB.a * band), fill);
    } else {
        vec2 dir = vec2(cos(angle), sin(angle));
        float extent = abs(dir.x) * halfSize.x + abs(dir.y) * halfSize.y;
        float t = clamp(dot(local, dir) / max(extent, 0.0001) * 0.5 + 0.5, 0.0, 1.0);
        fill = mix(v_ColorA, v_ColorB, t);

        if (flowAmount > 0.0) fill = over(fill, vec4(flowing, flowAmount));
    }

    // Stroke, drawn just inside the edge
    vec4 stroke = (flags & FLAG_FLOW_STROKE) != 0 ? vec4(flowing, v_Stroke.a) : v_Stroke;
    float strokeMask = strokeWidth > 0.0 ? clamp(d + strokeWidth + 0.5, 0.0, 1.0) : 0.0;

    vec4 body = over(vec4(stroke.rgb, stroke.a * strokeMask), fill);
    body.a *= inside;

    // Glow, outside only, like a CSS box-shadow
    vec4 glow;

    if (kind == KIND_GLASS) {
        vec4 wide = vec4(v_FlowB.rgb, 0.07 * glowFalloff(d, spread));
        vec4 near = vec4(mix(v_FlowA.rgb, v_FlowB.rgb, 0.5), 0.10 * glowFalloff(d, spread * 0.5));
        vec4 edge = vec4(flowing, 0.55 * glowFalloff(d, spread * 9.0 / 110.0));
        glow = over(edge, over(near, wide));
        glow.a *= v_Glow.a;
    } else {
        glow = (flags & FLAG_FLOW_GLOW) != 0 ? vec4(flowing, v_Glow.a) : v_Glow;
        glow.a *= glowFalloff(d, spread);
    }

    glow.a *= 1.0 - inside;

    color = over(body, glow);
    if (color.a < 0.002) discard;
}

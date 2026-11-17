#version 330
#extension GL_ARB_separate_shader_objects : require

// One quad per shape. Everything the fragment shader needs travels with the vertices,
// so every shape of a GUI batch is drawn with a single draw call.

layout (location = 0) in vec2 Position;
layout (location = 1) in vec2 Local;     // pixel position relative to the centre of the shape
layout (location = 2) in vec2 HalfSize;  // half of the shape's size in pixels
layout (location = 3) in vec2 Shape;     // corner radius, kind + 10 * flags
layout (location = 4) in vec2 Edge;      // stroke width, glow spread
layout (location = 5) in vec2 Anim;      // time in seconds, seed
layout (location = 6) in vec2 Extra;     // gradient angle in radians, flowing colour amount
layout (location = 7) in vec4 ColorA;
layout (location = 8) in vec4 ColorB;
layout (location = 9) in vec4 StrokeColor;
layout (location = 10) in vec4 GlowColor;
layout (location = 11) in vec4 FlowA;     // the two colours of the drifting light
layout (location = 12) in vec4 FlowB;

layout (std140) uniform MeshData {
    mat4 u_Proj;
    mat4 u_ModelView;
};

layout (location = 0) out vec4 v_Box;
layout (location = 1) out vec4 v_Shape;
layout (location = 2) out vec4 v_Anim;
layout (location = 3) out vec4 v_ColorA;
layout (location = 4) out vec4 v_ColorB;
layout (location = 5) out vec4 v_Stroke;
layout (location = 6) out vec4 v_Glow;
layout (location = 7) out vec4 v_FlowA;
layout (location = 8) out vec4 v_FlowB;
layout (location = 9) out vec2 v_Screen;

void main() {
    gl_Position = u_Proj * u_ModelView * vec4(Position, 0.0, 1.0);

    v_Box = vec4(Local, HalfSize);
    v_Shape = vec4(Shape, Edge);
    v_Anim = vec4(Anim, Extra);
    v_ColorA = ColorA;
    v_ColorB = ColorB;
    v_Stroke = StrokeColor;
    v_Glow = GlowColor;
    v_FlowA = FlowA;
    v_FlowB = FlowB;

    // Same mapping Meteor's blur passes use, so the backdrop lines up on every backend
    v_Screen = gl_Position.xy / gl_Position.w * 0.5 + 0.5;
}

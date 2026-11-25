shader_type spatial;
render_mode cull_back;

uniform vec4 deep_color : source_color = vec4(0.055, 0.23, 0.29, 1.0);
uniform vec4 shallow_color : source_color = vec4(0.26, 0.53, 0.53, 1.0);
uniform float wave_speed = 1.0;
varying vec2 surface_position;

void vertex() {
    surface_position = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xz;
}

void fragment() {
    float t = TIME * wave_speed;
    float a = sin(surface_position.x * 5.0 + surface_position.y * 3.4 + t * 1.7);
    float b = sin(surface_position.x * -2.7 + surface_position.y * 8.1 - t * 1.3);
    float c = sin(surface_position.x * 10.0 + surface_position.y * 4.0 + t * 2.1);
    float ripple = clamp(0.48 + 0.19 * a + 0.19 * b + 0.08 * c, 0.0, 1.0);
    ALBEDO = mix(deep_color.rgb, shallow_color.rgb, ripple);
    ROUGHNESS = mix(0.13, 0.30, ripple);
    SPECULAR = 0.75;
    METALLIC = 0.05;
}

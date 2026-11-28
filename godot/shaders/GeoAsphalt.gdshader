shader_type spatial;
render_mode cull_disabled;
varying vec2 ground_position;

void vertex() {
    ground_position = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xz;
}

float grain(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void fragment() {
    // World-space texture: no per-chunk seams or external downloads.
    vec2 p = ground_position;
    float coarse = grain(floor(p * 3.0));
    float fine = grain(floor(p * 35.0));
    // Fade tiny grains at a distance to reduce shimmering.
    float visible_grain = 1.0 - smoothstep(0.02, 0.12, length(fwidth(p)));
    float variation = (coarse - 0.5) * 0.022 + (fine - 0.5) * 0.055 * visible_grain;
    ALBEDO = vec3(0.35, 0.36, 0.37) + vec3(variation);
    ROUGHNESS = 0.94;
}

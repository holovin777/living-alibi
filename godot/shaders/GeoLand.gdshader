shader_type spatial;
render_mode cull_disabled;
uniform vec4 soil : source_color = vec4(0.30,0.24,0.15,1.0);
uniform vec4 crop : source_color = vec4(0.48,0.55,0.27,1.0);
uniform float rows = 0.0;
varying vec2 world_pos;
void vertex(){
    world_pos=(MODEL_MATRIX*vec4(VERTEX,1.0)).xz;
}
void fragment(){
    float stripe=0.5+0.5*sin((world_pos.x*0.86+world_pos.y*0.50)*6.283185/1.5);
    float fade=1.0-smoothstep(0.1,1.0,length(fwidth(world_pos)));
    float bands=mix(0.5,stripe,fade*rows);
    float patch=sin(world_pos.x*0.13)*sin(world_pos.y*0.11);
    ALBEDO=mix(soil.rgb,crop.rgb,clamp(bands*0.85+patch*0.08,0.0,1.0));
    ROUGHNESS=0.96;
}

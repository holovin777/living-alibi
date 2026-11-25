extends RefCounted
## Original, seamless procedural textures for the Compatibility renderer.
## Created once per scene, shared by every loaded district.
const SIZE := 256

static func texture(kind: String) -> ImageTexture:
    var img := Image.create(SIZE,SIZE,false,Image.FORMAT_RGB8)
    var rng := RandomNumberGenerator.new()
    rng.seed = 7319
    for y in range(SIZE):
        for x in range(SIZE):
            var u := float(x) / SIZE
            var v := float(y) / SIZE
            var grain := rng.randf_range(-0.07,0.07)
            var broad := sin(u*TAU*4.0)*sin(v*TAU*2.0)*0.05
            var value := 0.86+grain+broad
            if kind == "grass":
                # Fine interleaved blades and larger irregular tufts.
                var blades := sin(TAU*(u*47.0+v*13.0)+sin(v*TAU*5.0)*2.0)
                var clumps := sin(u*TAU*7.0)*cos(v*TAU*9.0)
                value = 0.79 + 0.065*blades + 0.085*clumps + broad + grain
            elif kind == "asphalt":
                # Mineral aggregate, mottled patches and occasional pale grit.
                value = 0.76 + broad + grain*1.35
                if rng.randf() < 0.045:
                    value += 0.16
            elif kind == "paving":
                # Staggered stonework with darker mortar and lit beveled edges.
                var row := floori(float(y)/32.0)
                var sx := (x + (32 if row%2 == 1 else 0))%64
                var sy := y%32
                var tile_variation := sin(float(row*17+floori(float(x+(32 if row%2 == 1 else 0))/64.0)*29))*0.065
                value = 0.85 + tile_variation + grain*0.6 + broad
                if sx < 2 or sy < 2:
                    value = 0.48 + grain
                elif sx == 2 or sy == 2:
                    value += 0.10
                elif sx >= 62 or sy >= 30:
                    value -= 0.11
            value = clampf(value,0.0,1.0)
            img.set_pixel(x,y,Color(value,value,value))
    img.generate_mipmaps()
    return ImageTexture.create_from_image(img)

static func surface(mat: StandardMaterial3D, tex: Texture2D, scale: float) -> void:
    mat.albedo_texture = tex
    mat.uv1_triplanar = true
    mat.uv1_scale = Vector3.ONE*scale
    mat.roughness = 0.94

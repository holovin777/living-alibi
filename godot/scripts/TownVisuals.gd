extends RefCounted
## Original, lightweight procedural textures for the Compatibility renderer.
## Created once per scene, shared by every loaded district.
static func texture(kind: String) -> ImageTexture:
    var img := Image.create(128,128,false,Image.FORMAT_RGB8)
    var rng := RandomNumberGenerator.new()
    rng.seed=7319
    for y in range(128):
        for x in range(128):
            var grain := rng.randf_range(-0.045,0.045)
            var broad := sin(float(x)*TAU/32.0)*sin(float(y)*TAU/64.0)*0.035
            var value := clampf(0.86+grain+broad,0.0,1.0)
            if kind=="paving":
                var shifted := x+(16 if (y/16)%2==1 else 0)
                if y%16==0 or shifted%32==0:
                    value=0.55+grain
            img.set_pixel(x,y,Color(value,value,value))
    img.generate_mipmaps()
    return ImageTexture.create_from_image(img)

static func surface(mat: StandardMaterial3D, tex: Texture2D, scale: float) -> void:
    mat.albedo_texture=tex
    mat.uv1_triplanar=true
    mat.uv1_scale=Vector3.ONE*scale
    mat.roughness=0.94

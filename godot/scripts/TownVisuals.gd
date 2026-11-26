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
            elif kind == "stucco":
                # Fine mineral plaster, with broad uneven weathering.
                value = 0.90 + grain*0.52 + broad*1.4 + sin(u*TAU*2.0+v*TAU*3.0)*0.025
            elif kind == "terracotta":
                # Repeating curved clay tiles with staggered seams.
                var row := floori(float(y)/32.0)
                var sx := (x + (16 if row%2 == 1 else 0))%32
                var sy := y%32
                value = 0.76 + 0.14*sin(float(sx)*PI/31.0) + grain*0.5
                if sy < 2 or sx < 1:
                    value -= 0.18
                elif sy == 2:
                    value += 0.08
            elif kind == "wood":
                var streak := sin((u*36.0+sin(v*TAU*3.0)*0.35)*TAU)
                value = 0.81+0.07*streak+grain*0.45+broad*0.5
            value = clampf(value,0.0,1.0)
            img.set_pixel(x,y,Color(value,value,value))
    img.generate_mipmaps()
    return ImageTexture.create_from_image(img)

## Piazza Ducale-inspired original pebble mosaic, mapped once across the plaza.
## Pebbles are shaded in the texture rather than thousands of separate meshes.
static func ducale_paving() -> ImageTexture:
    const N := 768
    var img := Image.create(N,N,false,Image.FORMAT_RGB8)
    for y in range(N):
        for x in range(N):
            var px := (float(x)+0.5)/N*24.0-12.0
            var pz := (float(y)+0.5)/N*34.0-17.0
            var row := floori(pz*19.0)
            var shifted := px*17.0+float(posmod(row,2))*0.5
            var col := floori(shifted)
            var dx := (shifted-floorf(shifted)-0.5)*2.0
            var dz := (pz*19.0-floorf(pz*19.0)-0.5)*2.0
            var seed_value := sin(float(col*73+row*139))*43758.5453
            var variation := seed_value-floorf(seed_value)
            var r := sqrt(dx*dx+dz*dz)
            var pebble := Color("#77746c").lerp(Color("#aaa294"),variation)
            if r>0.94:
                pebble=Color("#4c4944")
            else:
                pebble=pebble.lightened(maxf(0.0,-dx-dz)*0.085)
            var radius := Vector2(px,pz).length()
            var inlay := absf(px)<0.16 or absf(absf(px)-9.8)<0.14 or absf(absf(pz)-15.6)<0.14
            inlay=inlay or absf(radius-3.9)<0.14 or absf(radius-5.2)<0.14
            for center in [-10.7,10.7]:
                var medallion := Vector2(px,pz-center)
                var distance := medallion.length()
                inlay=inlay or absf(distance-3.1)<0.12 or absf(distance-2.4)<0.10
                if distance<2.3:
                    var angle := atan2(medallion.y,medallion.x)
                    inlay=inlay or absf(distance-(1.45+0.65*cos(angle*8.0)))<0.08
            if inlay:
                pebble=Color("#c8c1ae").lerp(Color("#aaa397"),variation*0.45)
            img.set_pixel(x,y,pebble)
    img.generate_mipmaps()
    return ImageTexture.create_from_image(img)

static func surface(mat: StandardMaterial3D, tex: Texture2D, scale: float) -> void:
    mat.albedo_texture = tex
    mat.uv1_triplanar = true
    mat.uv1_scale = Vector3.ONE*scale
    mat.roughness = 0.94

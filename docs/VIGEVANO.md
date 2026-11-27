# Vigevano geographic preview

Run from the repository root:

    bash play-vigevano.sh

First launch requests one central Vigevano extract from the public Overpass API, then caches the OSM XML and converted JSON in data/. A saved XML file can be imported offline:

    python3 tools/import_vigevano.py --input data/vigevano.osm --output data/vigevano.json

Use --refresh only if map data needs updating. Do not repeatedly download. The bounding box is 45.3100,8.8480,45.3240,8.8700 (roughly 1.56 x 1.72 km), an initial central extract, not the municipality. Coordinates are locally projected into meters around Piazza Ducale. Building footprints become extruded walls and triangulated roofs; missing heights are estimated; road segments and rail traces are drawn from source coordinates. Geometry is streamed in a 3x3 window of 128 m chunks. Walk with WASD, click to capture mouse, Escape release, Space jump. This is an independent preview, not yet linked to Java simulation, vehicle routing, the SQLite world or NPCs. Original bash play.sh is unchanged. Building relations, courtyards, exact facades, slope, bridges/tunnels and real landmark models are not implemented. Street names are retained in data but not yet displayed. Map data are © OpenStreetMap contributors, licensed ODbL: https://www.openstreetmap.org/copyright — preserve attribution and ODbL obligations when distributing derived map databases. The test fixture is synthetic and not actual map data.

Schema 2 adds a road graph keyed by real OSM node IDs. Existing XML is re-converted automatically (no redownload). The M key opens a clickable full-extract map; select a street to calculate a walking path, and the HUD shows the nearest named road and route distance. The graph also represents basic motor access and directed one-way roads, but driving is not yet connected. Routing uses nodes and straight segments; turn restrictions, conditional access, accurate sidewalks, and pedestrian area interiors are not modeled. The Java world still runs separately.

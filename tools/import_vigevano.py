#!/usr/bin/env python3
"""Download/convert a small OSM XML extract into local, meter-scale city chunks."""
import argparse
import json
import math
from pathlib import Path
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET

ORIGIN = (45.3173, 8.8579)
BBOX = (45.3100, 8.8480, 45.3240, 8.8700)
CHUNK = 128
SOURCE = "© OpenStreetMap contributors — https://www.openstreetmap.org/copyright"
SCHEMA = 2
DETAIL_VERSION = 1
ROAD_WIDTH = {"motorway": 12, "trunk": 10, "primary": 8, "secondary": 7,
              "tertiary": 6, "residential": 5, "service": 3, "living_street": 4,
              "pedestrian": 5, "footway": 1.8, "path": 1.5, "cycleway": 2.0,
              "unclassified": 4.5, "track": 3, "steps": 1.5}


def project(lat, lon):
    return [round((lon-ORIGIN[1])*111320*math.cos(math.radians(ORIGIN[0])), 2),
            round((ORIGIN[0]-lat)*111132, 2)]


def download(path):
    bbox = ",".join(map(str, BBOX))
    query = f'[out:xml][timeout:90];(way["building"]({bbox});way["highway"]({bbox});way["railway"]({bbox});node["natural"="tree"]({bbox}););out body;>;out skel qt;'
    req = urllib.request.Request("https://overpass-api.de/api/interpreter",
                                 data=urllib.parse.urlencode({"data": query}).encode(),
                                 headers={"User-Agent": "LivingAlibi/0.1 local OSM import"})
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(".tmp")
    try:
        with urllib.request.urlopen(req, timeout=120) as response, tmp.open("wb") as out:
            while block := response.read(65536):
                out.write(block)
        if ET.parse(tmp).getroot().tag != "osm":
            raise ValueError("Overpass did not return OSM XML")
        tmp.replace(path)
    finally:
        tmp.unlink(missing_ok=True)


def height(tags):
    try:
        h = float(tags.get("height", "").strip().removesuffix("m").strip())
        if math.isfinite(h) and 2.5 <= h <= 100:
            return h
    except ValueError:
        pass
    try:
        levels = float(tags.get("building:levels", "2"))
        if math.isfinite(levels):
            return min(75, max(3, levels*3.2))
    except ValueError:
        pass
    return 6.4


def access(tags):
    """Basic access flags. Detailed conditional access/turn restrictions are not parsed."""
    kind = tags.get("highway")
    public = tags.get("access") not in ("no", "private")
    foot = tags.get("foot")
    motor = tags.get("motor_vehicle", tags.get("motorcar", tags.get("vehicle")))
    walk = kind not in ("motorway", "trunk") and public
    drive = kind in ("motorway", "trunk", "primary", "secondary", "tertiary", "residential",
                     "service", "living_street", "unclassified", "track") and public
    if foot in ("yes", "designated", "permissive"): walk = True
    if foot in ("no", "private"): walk = False
    if motor in ("yes", "designated", "permissive"): drive = True
    if motor in ("no", "private"): drive = False
    direction = tags.get("oneway", "yes" if tags.get("junction") == "roundabout" else "no")
    return walk, drive, direction


def convert(source, output):
    root = ET.parse(source).getroot()
    if root.tag != "osm":
        raise ValueError("Expected an OSM XML document")
    nodes = {n.attrib["id"]: project(float(n.attrib["lat"]), float(n.attrib["lon"]))
             for n in root.findall("node") if "lat" in n.attrib and "lon" in n.attrib}
    chunks = {}
    counts = {"buildings": 0, "roads": 0, "rail_segments": 0, "trees": 0, "skipped": 0}
    spawn = None
    best = float("inf")
    graph_nodes = {}
    graph_edges = []

    def add(kind, feature, point):
        key = f"{math.floor(point[0]/CHUNK)},{math.floor(point[1]/CHUNK)}"
        chunks.setdefault(key, {"buildings": [], "roads": [], "rail": [], "trees": []})[kind].append(feature)

    for n in root.findall("node"):
        if n.attrib["id"] in nodes and any(t.attrib.get("k") == "natural" and t.attrib.get("v") == "tree" for t in n.findall("tag")):
            point = nodes[n.attrib["id"]]
            add("trees", {"id": n.attrib["id"], "point": point}, point)
            counts["trees"] += 1

    for way in root.findall("way"):
        tags = {t.attrib["k"]: t.attrib["v"] for t in way.findall("tag")}
        refs = [n.attrib["ref"] for n in way.findall("nd")]
        if len(refs) < 2 or any(r not in nodes for r in refs):
            counts["skipped"] += 1
            continue
        points = [nodes[r] for r in refs]
        name = tags.get("name", "")
        if tags.get("building") not in (None, "no") and refs[0] == refs[-1] and len(refs) >= 4:
            points = points[:-1]
            if len({tuple(p) for p in points}) < 3:
                continue
            center = [sum(p[i] for p in points)/len(points) for i in (0, 1)]
            add("buildings", {"id": way.attrib["id"], "points": points, "height": height(tags),
                              "name": name, "kind": tags["building"]}, center)
            counts["buildings"] += 1
        if tags.get("highway") in ROAD_WIDTH:
            walk, drive, direction = access(tags)
            if walk or drive:
                for a_id, b_id in zip(refs, refs[1:]):
                    distance = math.dist(nodes[a_id], nodes[b_id])
                    if a_id == b_id or distance < .05:
                        continue
                    graph_nodes[a_id], graph_nodes[b_id] = nodes[a_id], nodes[b_id]
                    graph_edges.append({"a": a_id, "b": b_id, "meters": round(distance, 2),
                                        "name": name, "walk": walk,
                                        "drive_ab": drive and direction != "-1",
                                        "drive_ba": drive and direction not in ("yes", "1", "true")})
        if tags.get("highway") in ROAD_WIDTH or tags.get("railway") in ("rail", "light_rail"):
            rail = tags.get("railway") in ("rail", "light_rail")
            for a, b in zip(points, points[1:]):
                distance = math.dist(a, b)
                if distance < .05:
                    continue
                # Subdivide long edges so the streaming window does not lose distant geometry.
                pieces = max(1, math.ceil(distance/32))
                for i in range(pieces):
                    aa = [a[k]+(b[k]-a[k])*i/pieces for k in (0,1)]
                    bb = [a[k]+(b[k]-a[k])*(i+1)/pieces for k in (0,1)]
                    center = [(aa[k]+bb[k])/2 for k in (0,1)]
                    add("rail" if rail else "roads", {"id": way.attrib["id"], "a": aa, "b": bb,
                        "name": name, "width": 1.8 if rail else ROAD_WIDTH[tags["highway"]],
                        "kind": tags.get("highway", ""), "sidewalk": tags.get("sidewalk", ""),
                        "oneway": tags.get("oneway", "no")}, center)
                    counts["rail_segments" if rail else "roads"] += 1
                    if not rail and access(tags)[0] and tags.get("highway") != "steps":
                        d = math.hypot(*center)
                        if d < best:
                            spawn, best = center, d
    if not counts["buildings"] or not counts["roads"] or spawn is None:
        raise ValueError("Extract must contain buildings and walkable roads")
    result = {"schema": SCHEMA, "detail_version": DETAIL_VERSION, "graph": {"nodes": graph_nodes, "edges": graph_edges}, "origin": list(ORIGIN), "bounds": list(BBOX),
              "chunk_size": CHUNK, "attribution": SOURCE, "counts": counts,
              "spawn": spawn, "chunks": chunks}
    output.parent.mkdir(parents=True, exist_ok=True)
    tmp = output.with_suffix(".tmp")
    tmp.write_text(json.dumps(result, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    tmp.replace(output)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, default=Path("data/vigevano.osm"))
    parser.add_argument("--output", type=Path, default=Path("data/vigevano.json"))
    parser.add_argument("--download", action="store_true", help="Fetch the central Vigevano extract once")
    args = parser.parse_args()
    if args.download:
        download(args.input)
    result = convert(args.input, args.output)
    print(f"Imported {result['counts']} across {len(result['chunks'])} chunks -> {args.output}")


if __name__ == "__main__":
    main()

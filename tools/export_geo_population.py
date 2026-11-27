#!/usr/bin/env python3
"""Export stable walk graph and building-to-street anchors for the Java geo world."""
import argparse
import json
import math
from collections import defaultdict
from pathlib import Path

def export(source, destination):
    city = json.loads(source.read_text(encoding="utf-8"))
    graph = city["graph"]
    edges = sorted({tuple(sorted((str(e["a"]), str(e["b"])))) for e in graph["edges"] if e.get("walk")})
    ids = sorted({n for edge in edges for n in edge})
    points = {n: graph["nodes"][n] for n in ids}
    if len(points) < 2:
        raise ValueError("No usable public walking network")
    buckets = defaultdict(list)
    cell = 64
    for n, (x, y) in points.items():
        buckets[math.floor(x/cell), math.floor(y/cell)].append(n)
    rows = ["N\t%s\t%.2f\t%.2f" % (n, *points[n]) for n in ids]
    rows.extend("E\t%s\t%s" % edge for edge in edges)
    # Separate motor nodes: preserve the pedestrian graph and its components.
    motor = sorted({(str(e[a]),str(e[b])) for e in graph["edges"]
                    for a,b,flag in (("a","b","drive_ab"),("b","a","drive_ba")) if e.get(flag)})
    motor_ids = sorted({n for edge in motor for n in edge})
    rows.extend("M\t%s\t%.2f\t%.2f" % (n,*graph["nodes"][n]) for n in motor_ids)
    rows.extend("D\t%s\t%s" % edge for edge in motor)
    seen = set()
    for chunk in city["chunks"].values():
        for b in chunk["buildings"]:
            bid = str(b["id"])
            if bid in seen:
                continue
            seen.add(bid)
            polygon = b["points"]
            x = sum(p[0] for p in polygon)/len(polygon)
            y = sum(p[1] for p in polygon)/len(polygon)
            cx, cy = math.floor(x/cell), math.floor(y/cell)
            candidates = [n for i in range(cx-2,cx+3) for j in range(cy-2,cy+3)
                          for n in buckets[i,j]]
            if not candidates:
                continue
            n = min(candidates, key=lambda n: ((points[n][0]-x)**2+(points[n][1]-y)**2,n))
            if math.dist(points[n],(x,y)) > 120:
                continue
            rows.append("B\t%s\t%s" % (bid,n))
    destination.parent.mkdir(parents=True,exist_ok=True)
    tmp = destination.with_suffix(".tmp")
    tmp.write_text("\n".join(sorted(rows))+"\n",encoding="utf-8")
    tmp.replace(destination)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source",type=Path)
    parser.add_argument("destination",type=Path)
    args = parser.parse_args()
    export(args.source,args.destination)

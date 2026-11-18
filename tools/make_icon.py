"""Generate Breezy Expressive launcher icon (option C: sun + two curled gusts).

Writes vector drawables (foreground, monochrome, background) and legacy webp
bitmaps into app/src/res_fork. Usage: python make_icon.py <res_fork dir> <preview png>
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw
from shapely.geometry import LineString, Point, Polygon, MultiPolygon
from shapely.ops import unary_union

RES = Path(sys.argv[1])
PREVIEW = Path(sys.argv[2])

BG = "#4A90E2"
SUN = "#FFC93C"
WIND = "#FFFFFF"

S, DY = 0.48, 9  # sketch -> 108 viewport


def P(x, y):
    return (54 + S * x, 54 + S * (y - DY))


def cubic(p0, p1, p2, p3, n=24):
    pts = []
    for i in range(n + 1):
        t = i / n
        a = (1 - t) ** 3
        b = 3 * (1 - t) ** 2 * t
        c = 3 * (1 - t) * t ** 2
        d = t ** 3
        pts.append((a * p0[0] + b * p1[0] + c * p2[0] + d * p3[0],
                    a * p0[1] + b * p1[1] + c * p2[1] + d * p3[1]))
    return pts


def gust(start, h_end, c1, c2):
    """Straight line then two cubic segments forming a curl."""
    pts = [P(*start), P(h_end, start[1])]
    cur = P(h_end, start[1])
    for (a, b, c) in (c1, c2):
        seg = cubic(cur, P(*a), P(*b), P(*c))
        pts += seg[1:]
        cur = seg[-1]
    return LineString(pts)


g1 = gust((-50, 14), 18, ((34, 14), (38, -6), (24, -9)), ((15, -11), (12, -1), (18, 2)))
g2 = gust((-38, 36), 30, ((46, 36), (48, 56), (33, 57)), ((24, 58), (22, 48), (29, 46)))

W = 9 * S / 2      # wind half width
GAP = 17 * S / 2   # gap half width around wind

wind = unary_union([g1.buffer(W, quad_segs=16), g2.buffer(W, quad_segs=16)])
cut = unary_union([g1.buffer(GAP, quad_segs=16), g2.buffer(GAP, quad_segs=16)])
sun = Point(P(-4, -10)).buffer(31 * S, quad_segs=32).difference(cut)


def polys(geom):
    if isinstance(geom, Polygon):
        return [geom]
    if isinstance(geom, MultiPolygon):
        return list(geom.geoms)
    return [g for g in geom.geoms if isinstance(g, Polygon)]


def ring_d(coords):
    c = list(coords)
    return "M" + "L".join(f"{x:.2f},{y:.2f}" for x, y in c[:-1]) + "Z"


def path_d(geom):
    out = []
    for p in polys(geom):
        out.append(ring_d(p.exterior.coords))
        for i in p.interiors:
            out.append(ring_d(i.coords))
    return "".join(out)


HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n'
        '<!-- Breezy Expressive launcher icon: sun with two curled gusts. Generated. -->\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n    android:height="108dp"\n'
        '    android:viewportWidth="108"\n    android:viewportHeight="108">\n')


def vec(paths):
    body = "".join(
        f'    <path\n        android:fillColor="{c}"\n        android:pathData="{d}" />\n'
        for c, d in paths)
    return HEAD + body + "</vector>\n"


(RES / "drawable-v24/ic_launcher_foreground.xml").write_text(
    vec([(SUN, path_d(sun)), (WIND, path_d(wind))]), encoding="utf-8", newline="\n")
(RES / "drawable/ic_launcher_monochrome.xml").write_text(
    vec([("#FFFFFF", path_d(unary_union([sun, wind])))]), encoding="utf-8", newline="\n")
(RES / "drawable/ic_launcher_background.xml").write_text(
    vec([(BG, "M0,0h108v108h-108z")]), encoding="utf-8", newline="\n")


def render(px, mono=False, crop=18):
    """Render the visible 72-unit area (adaptive inner area) as a circle, px wide."""
    k = 8
    big = px * k
    sc = big / (108 - 2 * crop)
    im = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    bg = "#D3E3FD" if mono else BG
    fg_sun = "#0B3D91" if mono else SUN
    fg_wind = "#0B3D91" if mono else WIND
    d.ellipse((0, 0, big - 1, big - 1), fill=bg)

    def fill(geom, col):
        for p in polys(geom):
            d.polygon([((x - crop) * sc, (y - crop) * sc) for x, y in p.exterior.coords], fill=col)
            for i in p.interiors:
                d.polygon([((x - crop) * sc, (y - crop) * sc) for x, y in i.coords], fill=bg)

    fill(sun, fg_sun)
    fill(wind, fg_wind)
    return im.resize((px, px), Image.LANCZOS)


for dens, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    for name in ("ic_launcher", "ic_launcher_round"):
        render(px).save(RES / f"mipmap-{dens}/{name}.webp", "WEBP", lossless=True)
for name in ("ic_launcher", "ic_launcher_round"):
    render(192).save(RES / f"drawable/{name}.webp", "WEBP", lossless=True)

prev = Image.new("RGBA", (440, 220), (0, 0, 0, 0))
prev.paste(render(200), (10, 10))
prev.paste(render(200, mono=True), (230, 10))
prev.save(PREVIEW)
print("ok")

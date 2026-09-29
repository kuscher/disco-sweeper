#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Renders the launcher icon to docs/images/icon.png for the README.

Reads the adaptive icon's own vector drawables (res/drawable/ic_launcher_bg.xml and
ic_launcher_fg.xml, with their gradient files), turns them into one SVG cropped to the 72 dp
safe zone in a circle mask (as a launcher with round icons shows it) and rasterises it with
rsvg-convert. The icon itself is only ever edited in res/; this just draws it.

    tools/icon.py [SIZE]      # default 512
"""
import pathlib
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res/drawable"
A = "{http://schemas.android.com/apk/res/android}"


def colour(argb):
    """#aarrggbb or #rrggbb -> (#rrggbb, opacity)."""
    h = argb.lstrip("#")
    if len(h) == 8:
        return "#" + h[2:], int(h[:2], 16) / 255
    return "#" + h, 1.0


class Svg:
    def __init__(self):
        self.defs = []
        self.ids = 0

    def new_id(self, prefix):
        self.ids += 1
        return f"{prefix}{self.ids}"

    def gradient(self, name):
        g = ET.parse(RES / f"{name}.xml").getroot()
        gid = self.new_id("g")
        stops = ""
        for item in g.findall("item"):
            c, o = colour(item.get(A + "color"))
            stops += f'<stop offset="{item.get(A + "offset")}" stop-color="{c}" stop-opacity="{o:.3f}"/>'
        if g.get(A + "type") == "radial":
            self.defs.append(f'<radialGradient id="{gid}" gradientUnits="userSpaceOnUse" cx="{g.get(A + "centerX")}" '
                             f'cy="{g.get(A + "centerY")}" r="{g.get(A + "gradientRadius")}">{stops}</radialGradient>')
        else:
            self.defs.append(f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="{g.get(A + "startX")}" '
                             f'y1="{g.get(A + "startY")}" x2="{g.get(A + "endX")}" y2="{g.get(A + "endY")}">{stops}</linearGradient>')
        return f"url(#{gid})"

    def paint(self, value):
        if value is None:
            return "none", 1.0
        if value.startswith("@drawable/"):
            return self.gradient(value.split("/", 1)[1]), 1.0
        return colour(value)

    def body(self, node):
        out = ""
        for child in node:
            if child.tag == "path":
                fill, fo = self.paint(child.get(A + "fillColor"))
                stroke, so = self.paint(child.get(A + "strokeColor"))
                rule = "evenodd" if child.get(A + "fillType") == "evenOdd" else "nonzero"
                out += (f'<path d="{child.get(A + "pathData")}" fill="{fill}" fill-opacity="{fo:.3f}" fill-rule="{rule}" '
                        f'stroke="{stroke}" stroke-opacity="{so:.3f}" stroke-width="{child.get(A + "strokeWidth", "0")}"/>')
            elif child.tag == "group":
                clip = child.find("clip-path")
                inner = self.body([c for c in child if c.tag != "clip-path"])
                if clip is not None:
                    cid = self.new_id("c")
                    self.defs.append(f'<clipPath id="{cid}"><path d="{clip.get(A + "pathData")}"/></clipPath>')
                    out += f'<g clip-path="url(#{cid})">{inner}</g>'
                else:
                    out += f"<g>{inner}</g>"
        return out


def main():
    size = int(sys.argv[1]) if len(sys.argv) > 1 else 512
    svg = Svg()
    layers = "".join(svg.body(ET.parse(RES / f"{n}.xml").getroot()) for n in ("ic_launcher_bg", "ic_launcher_fg"))
    doc = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="18 18 72 72">'
           f'<defs>{"".join(svg.defs)}<clipPath id="mask"><circle cx="54" cy="54" r="36"/></clipPath></defs>'
           f'<g clip-path="url(#mask)">{layers}</g></svg>')
    out = ROOT / "docs/images/icon.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile("w", suffix=".svg", delete=False) as f:
        f.write(doc)
    subprocess.run(["rsvg-convert", "-w", str(size), "-h", str(size), "-o", str(out), f.name], check=True)
    pathlib.Path(f.name).unlink()
    print(out.relative_to(ROOT))


if __name__ == "__main__":
    main()

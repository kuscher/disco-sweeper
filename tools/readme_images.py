#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Frames the app's own renders (./ds debug shot NAME, ./ds pull NAME) for the README.

The renders come from the app's view tree, so they never show anything else on the screen. This
trims the empty caption strip at the top, rounds the corners like a desktop window, adds a soft
shadow and sets one or two windows side by side on a light backdrop.

    tools/readme_images.py OUT.png RENDER.png [RENDER.png]
"""
import sys
from PIL import Image, ImageDraw, ImageFilter

RADIUS = 18
PAD = 40
GAP = 36
BACKDROP = (242, 238, 248, 255)


def caption_rows(im):
    """The rows at the top the system caption covers: one flat colour, drawn by nothing."""
    first = im.getpixel((0, 0))
    for y in range(im.height // 4):
        row = im.crop((0, y, im.width, y + 1))
        if row.getcolors(4) is None or any(c != first for _, c in row.getcolors(4)):
            return y
    return 0


def window(path):
    im = Image.open(path).convert("RGBA")
    im = im.crop((0, caption_rows(im), im.width, im.height))
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), RADIUS, fill=255)
    im.putalpha(mask)
    return im


def main():
    out, paths = sys.argv[1], sys.argv[2:]
    wins = [window(p) for p in paths]
    w = sum(x.width for x in wins) + GAP * (len(wins) - 1) + 2 * PAD
    h = max(x.height for x in wins) + 2 * PAD
    canvas = Image.new("RGBA", (w, h), BACKDROP)
    shadow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    x = PAD
    for win in wins:
        y = PAD + (h - 2 * PAD - win.height) // 2
        box = Image.new("RGBA", win.size, (40, 20, 70, 90))
        box.putalpha(win.getchannel("A").point(lambda a: a * 90 // 255))
        shadow.paste(box, (x, y + 10), box)
        x += win.width + GAP
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(16)))
    x = PAD
    for win in wins:
        y = PAD + (h - 2 * PAD - win.height) // 2
        canvas.alpha_composite(win, (x, y))
        x += win.width + GAP
    canvas.convert("RGB").save(out, optimize=True)
    print(out, canvas.size)


if __name__ == "__main__":
    main()

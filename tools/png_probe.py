#!/usr/bin/env python3
"""Pure-stdlib PNG reader + bottom-band probe, used to verify the Android inset fix
without a vision model.

Usage: python tools/png_probe.py <image.png> [--band 150]
Reports: size, per-band dominant colors, and whether the Ink/Accent button colors
appear inside the bottom `band` pixels (the system navigation-bar zone).
"""
from __future__ import annotations

import struct
import sys
import zlib
from collections import Counter

INK = (0x26, 0x23, 0x1D)
ACCENT = (0xC2, 0x54, 0x2B)
PAPER = (0xF7, 0xF2, 0xE8)


def read_png(path: str):
    data = open(path, "rb").read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "not a PNG"
    pos = 8
    idat = b""
    width = height = bit_depth = color_type = None
    while pos < len(data):
        (length,) = struct.unpack(">I", data[pos:pos + 4])
        chunk_type = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if chunk_type == b"IHDR":
            width, height, bit_depth, color_type, comp, filt, interlace = struct.unpack(">IIBBBBB", chunk)
            assert bit_depth == 8, f"bit depth {bit_depth} unsupported"
            assert interlace == 0, "interlaced PNG unsupported"
        elif chunk_type == b"IDAT":
            idat += chunk
        elif chunk_type == b"IEND":
            break
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color_type]
    raw = zlib.decompress(idat)
    stride = width * channels
    out = bytearray(height * stride)
    prev = bytearray(stride)
    pos = 0
    for y in range(height):
        f = raw[pos]
        pos += 1
        line = bytearray(raw[pos:pos + stride])
        pos += stride
        if f == 1:
            for i in range(channels, stride):
                line[i] = (line[i] + line[i - channels]) & 0xFF
        elif f == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 0xFF
        elif f == 3:
            for i in range(stride):
                a = line[i - channels] if i >= channels else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 0xFF
        elif f == 4:
            for i in range(stride):
                a = line[i - channels] if i >= channels else 0
                b = prev[i]
                c = prev[i - channels] if i >= channels else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 0xFF
        out[y * stride:(y + 1) * stride] = line
        prev = line
    return width, height, channels, out


def px(buf, width, channels, x, y):
    i = (y * width + x) * channels
    return (buf[i], buf[i + 1], buf[i + 2])


def near(c, target, tol=26):
    return abs(c[0] - target[0]) <= tol and abs(c[1] - target[1]) <= tol and abs(c[2] - target[2]) <= tol


def main() -> int:
    path = sys.argv[1]
    band = 150
    if "--band" in sys.argv:
        band = int(sys.argv[sys.argv.index("--band") + 1])
    width, height, channels, buf = read_png(path)
    print(f"{path}: {width}x{height} channels={channels}")
    rows = {}
    for name, y in (("top", 2), ("mid", height // 2), ("above-band", height - band - 4),
                    ("band-top", height - band), ("band-mid", height - band // 2), ("bottom-8", height - 8)):
        c = Counter(px(buf, width, channels, x, y) for x in range(0, width, max(1, width // 400)))
        rows[name] = c.most_common(3)
        top = ", ".join(f"#{r:02X}{g:02X}{b:02X} x{n}" for (r, g, b), n in rows[name])
        print(f"  y={y:>4} {name:<11}: {top}")
    # probe the band for button colors
    ink_hits = accent_hits = 0
    for y in range(height - band, height):
        for x in range(0, width, 3):
            c = px(buf, width, channels, x, y)
            if near(c, INK):
                ink_hits += 1
            elif near(c, ACCENT):
                accent_hits += 1
    total = (band * (width // 3 + 1))
    print(f"  bottom {band}px: ink-like px={ink_hits} ({100.0 * ink_hits / total:.2f}%), "
          f"accent-like px={accent_hits} ({100.0 * accent_hits / total:.2f}%)")
    # locate the lowest WIDE run of button-colored pixels (a full-width button row)
    for name, target in (("ink", INK), ("accent", ACCENT)):
        lowest = None
        for y in range(height - 1, -1, -1):
            run = best = 0
            for x in range(0, width, 2):
                if near(px(buf, width, channels, x, y), target):
                    run += 2
                    best = max(best, run)
                else:
                    run = 0
            if best >= width * 0.4:
                lowest = y
                break
        if lowest is None:
            print(f"  {name} button: no wide row found")
        else:
            print(f"  {name} button: lowest wide row y={lowest}  "
                  f"({height - 1 - lowest} px above screen bottom)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

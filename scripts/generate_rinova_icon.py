#!/usr/bin/env python3
"""
Rinova Professional Vector & Mipmap Generator
Tema: Technician Shield + Wireless Debugging Signal + Precision Diagnostic Core
Palet: Electric Cyan (#00E5FF), Deep Teal (#00838F), Dark Slate (#0B0F14), White (#FFFFFF)
"""

import os
import sys
import zlib
import struct
import math

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

def create_png(width, height, draw_func):
    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0)  # Filter: None
        for x in range(width):
            r, g, b, a = draw_func(x, y, width, height)
            raw_data.extend([r, g, b, a])

    compressed = zlib.compress(bytes(raw_data), level=9)

    def chunk(tag, data):
        length = len(data)
        crc = zlib.crc32(tag + data) & 0xffffffff
        return struct.pack(">I", length) + tag + data + struct.pack(">I", crc)

    png_header = b"\x89PNG\r\n\x1a\n"
    ihdr_data = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    ihdr = chunk(b"IHDR", ihdr_data)
    idat = chunk(b"IDAT", compressed)
    iend = chunk(b"IEND", b"")

    return png_header + ihdr + idat + iend

def rinova_icon_painter(is_round=False):
    def painter(x, y, w, h):
        cx, cy = w / 2.0, h / 2.0
        r_max = w / 2.0
        dx = x - cx
        dy = y - cy
        dist = math.sqrt(dx * dx + dy * dy)

        corner_radius = r_max if is_round else r_max * 0.44

        # Masking shape
        if is_round:
            if dist > (r_max - 1.0):
                return 0, 0, 0, 0
        else:
            qx = max(0.0, abs(dx) - (cx - corner_radius))
            qy = max(0.0, abs(dy) - (cy - corner_radius))
            corner_dist = math.sqrt(qx * qx + qy * qy)
            if corner_dist > corner_radius - 0.5:
                return 0, 0, 0, 0

        # Cyber dark slate gradient background
        norm_y = y / float(h)
        # Background: #0B0F14 (11, 15, 20) to #00363D (0, 54, 61)
        bg_r = int(11 * (1.0 - norm_y * 0.4) + 0 * (norm_y * 0.4))
        bg_g = int(15 * (1.0 - norm_y * 0.4) + 40 * (norm_y * 0.4))
        bg_b = int(20 * (1.0 - norm_y * 0.4) + 50 * (norm_y * 0.4))

        # Radial center glow
        center_glow = max(0.0, 1.0 - (dist / (r_max * 0.85)))
        bg_r = min(255, int(bg_r + 0 * center_glow))
        bg_g = min(255, int(bg_g + 80 * center_glow))
        bg_b = min(255, int(bg_b + 95 * center_glow))

        # Foreground Symbol Drawing (Normalized to 0..1 scale)
        nx = dx / r_max
        ny = dy / r_max

        # 1. Outer Security Shield (Polygon)
        # Shield points: top (-0.55, -0.45) -> (0, -0.6) -> (0.55, -0.45) -> (0.45, 0.25) -> (0, 0.65) -> (-0.45, 0.25)
        # Simplified: Check distance from shield border
        is_shield_edge = False
        shield_top = -0.58
        shield_bottom = 0.62
        shield_width_at_y = 0.0
        if shield_top <= ny <= shield_bottom:
            if ny < 0.1:
                # Upper wider portion
                shield_width_at_y = 0.54 - abs(ny - 0.0) * 0.08
            else:
                # Lower tapering to point
                t = (ny - 0.1) / (shield_bottom - 0.1)
                shield_width_at_y = 0.50 * (1.0 - t * 0.95)
            
            border_dist = abs(abs(nx) - shield_width_at_y)
            if border_dist < 0.05 and ny > shield_top:
                is_shield_edge = True
            if abs(ny - shield_top) < 0.05 and abs(nx) <= shield_width_at_y:
                is_shield_edge = True

        if is_shield_edge:
            # Shield rim in vibrant Cyan
            return 0, 229, 255, 255

        # 2. Wireless Debugging Arcs inside shield
        # Arc 1: radius ~0.38, angles -60 to -120 deg (upper)
        arc_radius_1 = 0.38
        arc_dist_1 = abs(dist / r_max - arc_radius_1)
        if arc_dist_1 < 0.045 and ny < -0.05 and abs(nx) < 0.32:
            return 255, 255, 255, 255  # White crisp arc

        # Arc 2: radius ~0.24
        arc_radius_2 = 0.24
        arc_dist_2 = abs(dist / r_max - arc_radius_2)
        if arc_dist_2 < 0.04 and ny < 0.02 and abs(nx) < 0.20:
            return 128, 222, 234, 255  # Cyan teal arc

        # 3. Precision Diagnostic Lightning Core
        # Center lightning bolt
        # Points: (0.04, -0.05) -> (-0.08, 0.14) -> (-0.01, 0.14) -> (-0.05, 0.35) -> (0.10, 0.10) -> (0.02, 0.10)
        in_bolt = False
        if -0.05 <= ny <= 0.35 and -0.10 <= nx <= 0.12:
            # Upper segment
            if -0.05 <= ny <= 0.14:
                expected_x = 0.04 - (ny + 0.05) * 0.6
                if abs(nx - expected_x) < 0.045:
                    in_bolt = True
            # Lower segment
            elif 0.14 < ny <= 0.35:
                expected_x = 0.02 - (ny - 0.14) * 0.3
                if abs(nx - expected_x) < 0.045:
                    in_bolt = True

        if in_bolt:
            return 255, 255, 255, 255

        return bg_r, bg_g, bg_b, 255

    return painter

def generate_rinova_icons(res_dir):
    res_dir = os.path.abspath(res_dir)
    drawable_dir = os.path.join(res_dir, "drawable")
    anydpi_dir = os.path.join(res_dir, "mipmap-anydpi-v26")
    os.makedirs(drawable_dir, exist_ok=True)
    os.makedirs(anydpi_dir, exist_ok=True)

    # 1. Background Vector XML
    bg_xml = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Dark Slate Background -->
    <path
        android:fillColor="#0D1117"
        android:pathData="M0,0h108v108h-108z" />
    <!-- Cyber Radial Glow -->
    <path
        android:fillColor="#00363D"
        android:pathData="M54,14 A40,40 0 1 0 54,94 A40,40 0 1 0 54,14 Z"
        android:fillAlpha="0.45" />
</vector>
"""
    with open(os.path.join(drawable_dir, "ic_launcher_background.xml"), "w") as fp:
        fp.write(bg_xml)

    # 2. Foreground Vector XML (Technician Shield + Wireless + Lightning)
    fg_xml = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:scaleX="0.84"
        android:scaleY="0.84"
        android:translateX="8.64"
        android:translateY="8.64">
        <!-- Security Shield Outline (Electric Cyan) -->
        <path
            android:fillColor="#00000000"
            android:strokeColor="#00E5FF"
            android:strokeWidth="4.5"
            android:strokeLineCap="round"
            android:strokeLineJoin="round"
            android:pathData="M 54,22 L 78,32 C 78,60 54,82 54,86 C 54,82 30,60 30,32 Z" />
        <!-- Wireless Debugging Arc 1 (Crisp White) -->
        <path
            android:fillColor="#00000000"
            android:strokeColor="#FFFFFF"
            android:strokeWidth="4"
            android:strokeLineCap="round"
            android:pathData="M 38,46 A 20,20 0 0 1 70,46" />
        <!-- Wireless Debugging Arc 2 (Cyan Glow) -->
        <path
            android:fillColor="#00000000"
            android:strokeColor="#80DEEA"
            android:strokeWidth="3.5"
            android:strokeLineCap="round"
            android:pathData="M 44,53 A 12,12 0 0 1 64,53" />
        <!-- Diagnostic Lightning Pulse (Solid White) -->
        <path
            android:fillColor="#FFFFFF"
            android:pathData="M 56,48 L 47,63 L 53,63 L 49,76 L 62,59 L 55,59 Z" />
    </group>
</vector>
"""
    with open(os.path.join(drawable_dir, "ic_launcher_foreground.xml"), "w") as fp:
        fp.write(fg_xml)

    # 3. Adaptive XMLs
    adaptive_xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"""
    with open(os.path.join(anydpi_dir, "ic_launcher.xml"), "w") as fp:
        fp.write(adaptive_xml)
    with open(os.path.join(anydpi_dir, "ic_launcher_round.xml"), "w") as fp:
        fp.write(adaptive_xml)

    # 4. Generate Raster PNGs for all densities
    painter_sq = rinova_icon_painter(is_round=False)
    painter_rd = rinova_icon_painter(is_round=True)

    for folder_name, size in DENSITIES.items():
        folder_path = os.path.join(res_dir, folder_name)
        os.makedirs(folder_path, exist_ok=True)

        png_sq = create_png(size, size, painter_sq)
        with open(os.path.join(folder_path, "ic_launcher.png"), "wb") as fp:
            fp.write(png_sq)

        png_rd = create_png(size, size, painter_rd)
        with open(os.path.join(folder_path, "ic_launcher_round.png"), "wb") as fp:
            fp.write(png_rd)

    print(f"Ikon profesional Rinova berhasil dibuat di: {res_dir}")

if __name__ == "__main__":
    generate_rinova_icons("app/src/main/res")

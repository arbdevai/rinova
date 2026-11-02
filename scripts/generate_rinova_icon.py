#!/usr/bin/env python3
"""
Rinova Pure Vector-Equivalent Mipmap Rasterizer
Tema: Jeruk Orange X Dark Charcoal (Smartphone Diagnostic Hardware Scanner)
100% Pure Mathematical Vector Rendering, Zero Bitmap Collages.
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

def rinova_painter(is_round=False):
    def painter(x, y, w, h):
        cx, cy = w / 2.0, h / 2.0
        r_max = w / 2.0
        dx = x - cx
        dy = y - cy
        dist = math.sqrt(dx * dx + dy * dy)

        corner_radius = r_max if is_round else r_max * 0.44

        # Masking shape
        if is_round:
            if dist > (r_max - 0.5):
                return 0, 0, 0, 0
        else:
            qx = max(0.0, abs(dx) - (cx - corner_radius))
            qy = max(0.0, abs(dy) - (cy - corner_radius))
            corner_dist = math.sqrt(qx * qx + qy * qy)
            if corner_dist > corner_radius - 0.5:
                return 0, 0, 0, 0

        # Background: Matte Dark Charcoal (#14171C to #1A1F26)
        norm_y = y / float(h)
        bg_r = int(20 * (1.0 - norm_y) + 26 * norm_y)
        bg_g = int(23 * (1.0 - norm_y) + 31 * norm_y)
        bg_b = int(28 * (1.0 - norm_y) + 38 * norm_y)

        # Subtle radial center elevation
        center_glow = max(0.0, 1.0 - (dist / (r_max * 0.95)))
        bg_r = min(255, int(bg_r + 14 * center_glow))
        bg_g = min(255, int(bg_g + 16 * center_glow))
        bg_b = min(255, int(bg_b + 20 * center_glow))

        # Normalized coordinates (-1.0 to 1.0)
        nx = dx / r_max
        ny = dy / r_max

        # 1. Subtle Outer Telemetry Ring (Citrus Orange Accent at radius 0.76)
        ring_r = 0.76
        ring_dist = abs(dist / r_max - ring_r)
        if ring_dist < 0.02:
            alpha = (1.0 - ring_dist / 0.02) * 0.28
            # #FF7A00: (255, 122, 0)
            return int(255 * alpha + bg_r * (1 - alpha)), int(122 * alpha + bg_g * (1 - alpha)), int(0 * alpha + bg_b * (1 - alpha)), 255

        # 2. Smartphone Chassis:
        # Width: nx in [-0.34, 0.34], Height: ny in [-0.50, 0.50], Corner radius = 0.12
        phone_w = 0.34
        phone_h = 0.50
        phone_cr = 0.12
        pqx = max(0.0, abs(nx) - (phone_w - phone_cr))
        pqy = max(0.0, abs(ny) - (phone_h - phone_cr))
        pdist = math.sqrt(pqx * pqx + pqy * pqy)

        is_chassis_edge = False
        is_chassis_body = False

        if abs(nx) <= phone_w and abs(ny) <= phone_h:
            if pdist <= phone_cr:
                is_chassis_body = True
                if pdist >= (phone_cr - 0.055) or (pqx < 0.001 and abs(abs(ny) - phone_h) < 0.055) or (pqy < 0.001 and abs(abs(nx) - phone_w) < 0.055):
                    is_chassis_edge = True

        # Speaker notch (top)
        if -0.09 <= nx <= 0.09 and -0.44 <= ny <= -0.40:
            return 156, 163, 175, 255

        # Home bar (bottom)
        if -0.09 <= nx <= 0.09 and 0.41 <= ny <= 0.44:
            return 107, 114, 128, 255

        # 3. Precision Diagnostic Scanner Laser (Horizontal Beam at ny ~ 0.0)
        # Laser line: nx in [-0.54, 0.54], |ny| < 0.04
        if abs(ny) < 0.045 and abs(nx) <= 0.54:
            t_laser = 1.0 - (abs(ny) / 0.045)
            # Core beam: white at center, orange glow
            if abs(ny) < 0.018 and abs(nx) <= 0.32:
                return 255, 255, 255, 255
            # Orange laser beam (#FF7A00 to #FFA048)
            lr = int(255 * t_laser + bg_r * (1 - t_laser))
            lg = int(140 * t_laser + bg_g * (1 - t_laser))
            lb = int(0 * t_laser + bg_b * (1 - t_laser))
            return lr, lg, lb, 255

        # 4. Diagnostic Hardware Micro-Core (Citrus Orange square at center)
        # Core: nx in [-0.15, 0.15], ny in [-0.15, 0.15]
        if abs(nx) <= 0.15 and abs(ny) <= 0.15:
            # Diamond center node
            if (abs(nx) + abs(ny)) <= 0.08:
                return 255, 251, 235, 255  # Bright Amber White
            return 255, 122, 0, 255  # Citrus Orange (#FF7A00)

        # 5. Crosshair Reticle Brackets (Citrus Orange)
        # Corner brackets at [±0.18 to ±0.27], [±0.18 to ±0.27]
        is_bracket = False
        ax = abs(nx)
        ay = abs(ny)
        if (0.18 <= ax <= 0.27 and 0.22 <= ay <= 0.27) or (0.22 <= ax <= 0.27 and 0.18 <= ay <= 0.27):
            is_bracket = True

        if is_bracket:
            return 255, 160, 72, 255  # Bright Tangerine Orange (#FFA048)

        # Chassis edge outline
        if is_chassis_edge:
            return 229, 231, 235, 255  # Crisp Silver White (#E5E7EB)

        # Chassis inner body (matte dark slate)
        if is_chassis_body:
            return 27, 32, 40, 255

        return bg_r, bg_g, bg_b, 255

    return painter

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    res_dir = os.path.join(script_dir, "..", "app", "src", "main", "res")

    print(f"Rasterizing Pure Vector-Equivalent Icons into: {res_dir}")

    # Output Playstore 512x512
    playstore_png = create_png(512, 512, rinova_painter(is_round=False))
    with open(os.path.join(res_dir, "playstore-icon.png"), "wb") as f:
        f.write(playstore_png)
    print("  ✓ Playstore 512x512 px")

    # Generate Mipmap densities
    for folder, size in DENSITIES.items():
        out_folder = os.path.join(res_dir, folder)
        os.makedirs(out_folder, exist_ok=True)

        std_png = create_png(size, size, rinova_painter(is_round=False))
        with open(os.path.join(out_folder, "ic_launcher.png"), "wb") as f:
            f.write(std_png)

        round_png = create_png(size, size, rinova_painter(is_round=True))
        with open(os.path.join(out_folder, "ic_launcher_round.png"), "wb") as f:
            f.write(round_png)

        print(f"  ✓ {folder}: {size}x{size} px (standard & round)")

    print("Raster generation complete!")

if __name__ == "__main__":
    main()

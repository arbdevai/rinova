#!/usr/bin/env python3
"""
Install Generated Rinova Production App Icon into Android Project
Processes 1024x1024 master artwork into:
1. Standard legacy launcher icons (ic_launcher.png)
2. Round launcher icons (ic_launcher_round.png)
3. Adaptive icon foregrounds (ic_launcher_foreground.png)
4. Playstore master 512x512
"""

import os
from PIL import Image, ImageDraw

SRC_IMAGE = "/home/coder/.gemini/antigravity-cli/brain/8f55f776-e5e3-4ddd-977e-2d3b163c584a/rinova_app_icon_v2_1793617962866.jpg"

LEGACY_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

ADAPTIVE_SIZES = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}

def create_round_icon(img):
    w, h = img.size
    mask = Image.new('L', (w, h), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, w, h), fill=255)
    round_img = img.copy()
    round_img.putalpha(mask)
    return round_img

def create_squircle_icon(img, corner_ratio=0.22):
    w, h = img.size
    radius = int(w * corner_ratio)
    mask = Image.new('L', (w, h), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle((0, 0, w, h), radius=radius, fill=255)
    squircle_img = img.copy()
    squircle_img.putalpha(mask)
    return squircle_img

def create_adaptive_foreground(img):
    w, h = img.size
    fg = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    # Safe zone for adaptive icon is inner 66-72%
    inner_size = int(w * 0.72)
    scaled_inner = img.resize((inner_size, inner_size), Image.Resampling.LANCZOS)
    offset = (w - inner_size) // 2
    fg.paste(scaled_inner, (offset, offset))
    return fg

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    res_dir = os.path.join(script_dir, "..", "app", "src", "main", "res")

    print(f"Loading master icon from: {SRC_IMAGE}")
    master = Image.open(SRC_IMAGE).convert('RGBA')

    # Prepare master variants
    master_squircle = create_squircle_icon(master)
    master_round = create_round_icon(master)
    master_foreground = create_adaptive_foreground(master)

    # 1. Output Playstore 512x512
    playstore_path = os.path.join(res_dir, "playstore-icon.png")
    master.resize((512, 512), Image.Resampling.LANCZOS).save(playstore_path, "PNG")
    print(f"  ✓ Playstore 512x512 saved to: {playstore_path}")

    # 2. Output legacy and adaptive icons for each density
    for folder, size in LEGACY_SIZES.items():
        out_folder = os.path.join(res_dir, folder)
        os.makedirs(out_folder, exist_ok=True)

        # Standard launcher icon
        std_img = master_squircle.resize((size, size), Image.Resampling.LANCZOS)
        std_img.save(os.path.join(out_folder, "ic_launcher.png"), "PNG")

        # Round launcher icon
        rnd_img = master_round.resize((size, size), Image.Resampling.LANCZOS)
        rnd_img.save(os.path.join(out_folder, "ic_launcher_round.png"), "PNG")

        # Adaptive foreground
        fg_size = ADAPTIVE_SIZES[folder]
        fg_img = master_foreground.resize((fg_size, fg_size), Image.Resampling.LANCZOS)
        fg_img.save(os.path.join(out_folder, "ic_launcher_foreground.png"), "PNG")

        print(f"  ✓ {folder}: legacy {size}x{size}px, adaptive fg {fg_size}x{fg_size}px")

    print("All production app icons installed successfully!")

if __name__ == "__main__":
    main()

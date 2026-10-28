import os
import struct
import math
from pathlib import Path
from PIL import Image

BASE_DIR = Path(__file__).resolve().parent
SRC_DIR = BASE_DIR / "artwork" / "starter-cat"
ASSET_DIR = BASE_DIR / "app" / "src" / "main" / "assets" / "cat"
RAW_DIR = BASE_DIR / "app" / "src" / "main" / "res" / "raw"

COLOR_MAP = {
    "gray": "gray",
    "blue": "blue",
    "pink": "pink",
    "yellow": "yellow",
    "white": "white",
    "black": "black",
}

CROWN_MAP = {
    "blue-crown": "blue_crown",
    "pink-crown": "pink_crown",
}

# Target mappings: source relative path and destination filename.
MAPPINGS = [
    # Base colors.
    ("coat-colors/gray.jpg", "gray.jpg"),
    ("coat-colors/blue.jpg", "blue.jpg"),
    ("coat-colors/pink.jpg", "pink.jpg"),
    ("coat-colors/yellow.jpg", "yellow.jpg"),
    ("coat-colors/white.jpg", "white.jpg"),
    ("coat-colors/black.jpg", "black.jpg"),
    # Dresses.
    ("dresses/gray-red-dress.jpg", "gray_dress.jpg"),
    ("dresses/blue-red-dress.jpg", "blue_dress.jpg"),
    ("dresses/pink-red-dress.jpg", "pink_dress.jpg"),
    ("dresses/yellow-red-dress.jpg", "yellow_dress.jpg"),
    ("dresses/white-red-dress.jpg", "white_dress.jpg"),
    ("dresses/black-red-dress.jpg", "black_dress.jpg"),
    # Crowns.
    ("crowns/gray-red-dress-blue-crown.jpg", "gray_dress_blue_crown.jpg"),
    ("crowns/blue-red-dress-blue-crown.jpg", "blue_dress_blue_crown.jpg"),
    ("crowns/blue-red-dress-pink-crown.jpg", "blue_dress_pink_crown.jpg"),
    ("crowns/pink-red-dress-blue-crown.jpg", "pink_dress_blue_crown.jpg"),
    ("crowns/pink-red-dress-pink-crown.jpg", "pink_dress_pink_crown.jpg"),
    ("crowns/yellow-red-dress-blue-crown.jpg", "yellow_dress_blue_crown.jpg"),
    ("crowns/yellow-red-dress-pink-crown.jpg", "yellow_dress_pink_crown.jpg"),
    ("crowns/black-red-dress-blue-crown.jpg", "black_dress_blue_crown.jpg"),
    ("crowns/black-red-dress-pink-crown.jpg", "black_dress_pink_crown.jpg"),
]


def sample_background_color(img: Image.Image) -> tuple:
    """ Sample the outer edges, avoiding corner watermarks and the central subject; return mean RGB."""
    w, h = img.size
    strip = 18          # Sampling strip distance from the edge.
    corner_skip = 160   # Corner exclusion region.
    pixels = []

    def add(px, py):
        # Skip corners.
        in_corner = (
            (px < corner_skip and py < corner_skip) or
            (px > w - corner_skip and py > h - corner_skip) or
            (px < corner_skip and py > h - corner_skip) or
            (px > w - corner_skip and py < corner_skip)
        )
        if not in_corner:
            pixels.append(img.getpixel((px, py))[:3])

    for x in range(w):
        add(x, strip)
        add(x, h - 1 - strip)
    for y in range(h):
        add(strip, y)
        add(w - 1 - strip, y)

    if not pixels:
        return (255, 255, 255)
    r = sum(p[0] for p in pixels) // len(pixels)
    g = sum(p[1] for p in pixels) // len(pixels)
    b = sum(p[2] for p in pixels) // len(pixels)
    return (r, g, b)


def cover_watermarks(img: Image.Image, bg: tuple) -> Image.Image:
    """ Cover top-left and bottom-right watermark areas with the background color."""
    w, h = img.size
    # The longer bottom-right watermark needs a larger cover area than the top-left one.
    tl_w, tl_h = min(160, w // 8), min(60, h // 16)
    br_w, br_h = min(380, w // 3), min(110, h // 10)
    overlay = Image.new("RGB", img.size, bg)
    # Top left.
    img.paste(overlay.crop((0, 0, tl_w, tl_h)), (0, 0))
    # Bottom right.
    img.paste(overlay.crop((w - br_w, h - br_h, w, h)), (w - br_w, h - br_h))
    return img


def process_images():
    ASSET_DIR.mkdir(parents=True, exist_ok=True)
    for src_rel, dst_name in MAPPINGS:
        src = SRC_DIR / src_rel
        dst = ASSET_DIR / dst_name
        if not src.exists():
            print(f"SKIP (missing): {src}")
            continue
        img = Image.open(src).convert("RGB")
        bg = sample_background_color(img)
        img = cover_watermarks(img, bg)
        img.save(dst, "JPEG", quality=95)
        print(f"OK: {dst_name} ({img.size[0]}x{img.size[1]})")


def generate_meow():
    """ Generate a short, quiet meow WAV: 22050 Hz, 16-bit mono."""
    RAW_DIR.mkdir(parents=True, exist_ok=True)
    sample_rate = 22050
    duration = 0.42
    n_samples = int(sample_rate * duration)
    amplitude = 0.18  # Low volume.

    data = bytearray()
    for i in range(n_samples):
        t = i / sample_rate
        # Drop frequency from 700 Hz to 350 Hz to mimic a descending meow.
        freq = 700 * math.exp(-3.5 * t)
        # Subtle vibrato.
        vibrato = 1.0 + 0.06 * math.sin(2 * math.pi * 18 * t)
        # Amplitude envelope: fast attack and decay.
        env = math.exp(-4.0 * t) * (1 - math.exp(-30 * t))
        sample = amplitude * env * math.sin(2 * math.pi * freq * t * vibrato)
        # Convert to 16-bit samples.
        val = int(max(-1, min(1, sample)) * 32767)
        data.extend(struct.pack("<h", val))

    wav_path = RAW_DIR / "meow.wav"
    with open(wav_path, "wb") as f:
        f.write(b"RIFF")
        f.write(struct.pack("<I", 36 + len(data)))
        f.write(b"WAVEfmt ")
        f.write(struct.pack("<IHHIIHH", 16, 1, 1, sample_rate, sample_rate * 2, 2, 16))
        f.write(b"data")
        f.write(struct.pack("<I", len(data)))
        f.write(data)
    print(f"OK: {wav_path}")


if __name__ == "__main__":
    process_images()
    generate_meow()
    print("All cat assets ready.")

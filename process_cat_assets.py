import os
import struct
import math
from pathlib import Path
from PIL import Image

BASE_DIR = Path("C:/Users/13671/Desktop/MobileApp")
SRC_DIR = BASE_DIR / "图片" / "初始-猫"
ASSET_DIR = BASE_DIR / "app" / "src" / "main" / "assets" / "cat"
RAW_DIR = BASE_DIR / "app" / "src" / "main" / "res" / "raw"

COLOR_MAP = {
    "灰": "gray",
    "蓝": "blue",
    "粉": "pink",
    "黄": "yellow",
    "白": "white",
    "黑": "black",
}

CROWN_MAP = {
    "蓝冠": "blue_crown",
    "粉冠": "pink_crown",
}

# 目标文件映射：(来源相对路径, 目标文件名)
MAPPINGS = [
    # 基础颜色
    ("不同颜色的猫/灰.jpg", "gray.jpg"),
    ("不同颜色的猫/蓝.jpg", "blue.jpg"),
    ("不同颜色的猫/粉.jpg", "pink.jpg"),
    ("不同颜色的猫/黄.jpg", "yellow.jpg"),
    ("不同颜色的猫/白.jpg", "white.jpg"),
    ("不同颜色的猫/黑.jpg", "black.jpg"),
    # 裙子
    ("裙子/灰-红裙.jpg", "gray_dress.jpg"),
    ("裙子/蓝-红裙.jpg", "blue_dress.jpg"),
    ("裙子/粉-红裙.jpg", "pink_dress.jpg"),
    ("裙子/黄-红裙.jpg", "yellow_dress.jpg"),
    ("裙子/白-红裙.jpg", "white_dress.jpg"),
    ("裙子/黑-红裙.jpg", "black_dress.jpg"),
    # 皇冠
    ("皇冠/灰-红裙-蓝冠.jpg", "gray_dress_blue_crown.jpg"),
    ("皇冠/蓝-红裙-蓝冠.jpg", "blue_dress_blue_crown.jpg"),
    ("皇冠/蓝-红裙-粉冠.jpg", "blue_dress_pink_crown.jpg"),
    ("皇冠/粉-红裙-蓝冠.jpg", "pink_dress_blue_crown.jpg"),
    ("皇冠/粉-红裙-粉冠.jpg", "pink_dress_pink_crown.jpg"),
    ("皇冠/黄-红裙-蓝冠.jpg", "yellow_dress_blue_crown.jpg"),
    ("皇冠/黄-红裙-粉冠.jpg", "yellow_dress_pink_crown.jpg"),
    ("皇冠/黑-红裙-蓝冠.jpg", "black_dress_blue_crown.jpg"),
    ("皇冠/黑-红裙-粉冠.jpg", "black_dress_pink_crown.jpg"),
]


def sample_background_color(img: Image.Image) -> tuple:
    """从四边外侧采样背景色（避开四角水印和中间主体），返回 RGB 平均值。"""
    w, h = img.size
    strip = 18          # 距离边缘的采样条带
    corner_skip = 160   # 四角不采样区域
    pixels = []

    def add(px, py):
        # 跳过四角
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
    """用背景色涂盖左上角和右下角的水印区域。"""
    w, h = img.size
    # 右下角水印较长，需要更大覆盖区；左上角水印较小
    tl_w, tl_h = min(160, w // 8), min(60, h // 16)
    br_w, br_h = min(380, w // 3), min(110, h // 10)
    overlay = Image.new("RGB", img.size, bg)
    # 左上角
    img.paste(overlay.crop((0, 0, tl_w, tl_h)), (0, 0))
    # 右下角
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
    """生成一声短促、音量较小的喵喵 WAV（22050Hz 16-bit mono）。"""
    RAW_DIR.mkdir(parents=True, exist_ok=True)
    sample_rate = 22050
    duration = 0.42
    n_samples = int(sample_rate * duration)
    amplitude = 0.18  # 小声

    data = bytearray()
    for i in range(n_samples):
        t = i / sample_rate
        # 频率从 700Hz 降到 350Hz，模拟喵叫声调下降
        freq = 700 * math.exp(-3.5 * t)
        # 轻微颤音
        vibrato = 1.0 + 0.06 * math.sin(2 * math.pi * 18 * t)
        # 振幅包络：快速起音 + 衰减
        env = math.exp(-4.0 * t) * (1 - math.exp(-30 * t))
        sample = amplitude * env * math.sin(2 * math.pi * freq * t * vibrato)
        # 转为 16-bit
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

""" FocusPets: cut out the three new cat crown images from 图片/初始-猫/皇冠 into transparent PNGs under assets/cat/. The background is beige #FEF8EA (254,248,234); reuse v2 edge decontamination and light feathering. Match CatWardrobe.assetPathFor with cat/<color>_dress_<crown-color>.png."""
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter
from rembg import remove

BASE = Path("C:/Users/13671/Desktop/MobileApp")
SRC = BASE / "图片" / "初始-猫" / "皇冠"
OUT = BASE / "app" / "src" / "main" / "assets" / "cat"
BG = np.array([254, 248, 234], dtype=float)  # #FEF8EA

MAPPING = {
    "白-红裙-蓝冠.jpg": "white_dress_blue_crown.png",
    "白-红裙-粉冠.jpg": "white_dress_pink_crown.png",
    "灰-红裙-粉冠.jpg": "gray_dress_pink_crown.png",
}


def decontaminate(arr: np.ndarray) -> np.ndarray:
    """ Remove background contamination from translucent edges: observed=alpha*foreground+(1-alpha)*background."""
    rgb = arr[..., :3].astype(float)
    a = arr[..., 3].astype(float) / 255.0
    mask = (a > 0.03) & (a < 0.97)
    denom = np.maximum(a, 1e-2)
    fg = np.empty_like(rgb)
    for c in range(3):
        fg[..., c] = (rgb[..., c] - (1.0 - a) * BG[c]) / denom
    fg = np.clip(fg, 0, 255)
    out = rgb.copy()
    out[mask] = fg[mask]
    arr[..., :3] = out.astype(np.uint8)
    return arr


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for src_name, out_name in MAPPING.items():
        jpg = SRC / src_name
        if not jpg.exists():
            print("SKIP 缺失:", src_name)
            continue
        img = Image.open(jpg).convert("RGB")
        out = remove(img).convert("RGBA")
        arr = decontaminate(np.array(out))
        res = Image.fromarray(arr, "RGBA")
        a = res.split()[3].filter(ImageFilter.GaussianBlur(0.8))
        res.putalpha(a)
        res.save(OUT / out_name)
        print(f"OK {src_name} -> cat/{out_name}")


if __name__ == "__main__":
    main()

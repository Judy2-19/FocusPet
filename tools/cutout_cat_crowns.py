"""FocusPets：把「图片/初始-猫/皇冠」里新加的 3 张猫王冠图抠成透明 PNG 写入 assets/cat/。

猫背景为米白 #FEF8EA (254,248,234)，沿用 v2 的边缘去污染消除米白描边 + 轻羽化。
命名与 CatWardrobe.assetPathFor 对齐：cat/<颜色>_dress_<冠色>.png
"""
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
    """洗掉半透明边缘里的背景色残留（去米白边）。观测=alpha*fg+(1-alpha)*bg。"""
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

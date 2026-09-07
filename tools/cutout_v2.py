"""FocusPets 小猫抠图精修 v2。

相比上一版（rembg 默认 u2net、无 alpha_matting）的改进：
1. 全部 21 张开启 alpha_matting（fg=240/bg=10/erase=10），边缘更利落、去掉软边色块；
2. 对黄色系（yellow*）额外做「仅剥离边界处的背景色像素」迭代清理：
   - 背景色已知为 #FEF8EA (254,248,234)，与黄毛在蓝色通道差异极大（黄毛 B≈53，背景 B≈234），
     因此用颜色距离判定，不会误删黄毛；
   - 只移除「既接近背景色、又处在前景轮廓最外圈」的像素（迭代剥边），
     白裙内部被猫身包围、并非边界，不会被误删。
输出透明 PNG 覆盖 assets/cat/，并生成预览与边缘色块指标。
"""
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter
from rembg import remove

BASE = Path("C:/Users/13671/Desktop/MobileApp")
ORIG = BASE / "tools" / "originals"
OUT = BASE / "app" / "src" / "main" / "assets" / "cat"
BG = np.array([254, 248, 234], dtype=float)
MAGENTA = (255, 0, 255)


def dist_to_bg(rgb: np.ndarray) -> np.ndarray:
    return np.sqrt(((rgb.astype(float) - BG) ** 2).sum(axis=2))


def decontaminate(arr: np.ndarray) -> np.ndarray:
    """洗掉半透明边缘像素里的背景色残留（核心去描边手段）。

    观测色 = alpha*前景 + (1-alpha)*背景  =>  前景 = (观测 - (1-alpha)*背景) / alpha
    对 0<alpha<1 的像素还原真实前景色，避免合成到任意背景色上时出现米白描边。
    """
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


def refine_yellow(arr: np.ndarray) -> np.ndarray:
    """迭代剥掉黄色图轮廓最外圈的背景色像素（不伤黄毛 / 白裙内部）。"""
    rgb = arr[..., :3].astype(float)
    d = dist_to_bg(rgb)
    bgcolored = d < 55          # 接近背景色（黄毛 d≈183，远不会被选中）
    alpha = arr[..., 3].copy()
    fg = alpha > 10
    for _ in range(25):
        up = np.roll(fg, 1, 0); up[0, :] = fg[0, :]
        down = np.roll(fg, -1, 0); down[-1, :] = fg[-1, :]
        left = np.roll(fg, 1, 1); left[:, 0] = fg[:, 0]
        right = np.roll(fg, -1, 1); right[:, -1] = fg[:, -1]
        neighbor_not_fg = ~(up & down & left & right)
        boundary = fg & neighbor_not_fg & bgcolored
        if not boundary.any():
            break
        alpha[boundary] = 0
        fg = alpha > 10
    arr[..., 3] = alpha
    return arr


def edge_fringe_pct(arr: np.ndarray) -> float:
    """统计猫轮廓边缘带里接近背景色的残留像素比例（越低越好）。"""
    alpha = arr[..., 3]
    rgb = arr[..., :3].astype(float)
    d = dist_to_bg(rgb)
    h, w = alpha.shape
    fg = alpha > 10
    minx, miny, maxx, maxy = w, h, 0, 0
    ys, xs = np.where(fg)
    if ys.size == 0:
        return 0.0
    minx, miny, maxx, maxy = xs.min(), ys.min(), xs.max(), ys.max()
    band = np.zeros_like(fg)
    band[max(0, miny - 8):min(h, maxy + 9), max(0, minx - 8):min(w, maxx + 9)] = True
    edge_fg = fg & band
    edge_bg = edge_fg & (d < 45)
    tot = int(edge_fg.sum())
    return 0.0 if tot == 0 else 100.0 * int(edge_bg.sum()) / tot


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    pngs = []
    report = []
    for jpg in sorted(ORIG.glob("*.jpg")):
        img = Image.open(jpg).convert("RGB")
        out = remove(img).convert("RGBA")
        arr = np.array(out)
        # 全局边缘去污染：消除半透明处的背景色描边（黄/白猫都受益）
        arr = decontaminate(arr)
        if jpg.stem.startswith("yellow"):
            arr = refine_yellow(arr)
        # 轻羽化：抗锯齿，避免缩放后出现硬边描边
        res = Image.fromarray(arr, "RGBA")
        a = res.split()[3].filter(ImageFilter.GaussianBlur(0.8))
        res.putalpha(a)
        png = OUT / (jpg.stem + ".png")
        res.save(png)
        pngs.append(png)
        report.append((jpg.stem, edge_fringe_pct(arr)))
        print(f"OK {jpg.name} -> {png.name}  边缘色块={report[-1][1]:.1f}%")

    # 预览
    cols = 7
    cell = 170
    rows = (len(pngs) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * cell, rows * cell), MAGENTA)
    for i, p in enumerate(pngs):
        im = Image.open(p).convert("RGBA").resize((cell, cell))
        sheet.paste(im, (i % cols * cell, i // cols * cell), im)
    sheet.save(BASE / "tools" / "preview_v2.png", "PNG")

    print("\n=== 黄色系边缘色块对比（应接近 0） ===")
    for name, pct in report:
        if name.startswith("yellow"):
            print(f"  {name:26s} {pct:.1f}%")


if __name__ == "__main__":
    main()

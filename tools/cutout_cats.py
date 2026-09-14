"""
把 assets/cat/*.jpg 的小猫背景抠成透明 PNG。

方法：对每个文件
1) 从四边采样背景色（BG）。
2) 从图片四边做「洪泛填充」：只清除与 BG 颜色接近且能连通到边框的像素，
   这样不会误删与背景撞色但属于主体的内部像素（例如白猫 #FFF 与米白背景 #FEF8EA 很接近）。
3) 边缘羽化：连通区域内、离 BG 越近的像素 alpha 越低，得到柔和的抗锯齿边缘。
4) 输出同名 .png（RGBA）。

依赖：Pillow（managed venv）
运行：tools 目录下用 managed python 执行
"""
import math
from pathlib import Path

from PIL import Image

BASE = Path("C:/Users/13671/Desktop/MobileApp")
ASSET_DIR = BASE / "app" / "src" / "main" / "assets" / "cat"

TOL = 16       # 洪泛阈值：与 BG 颜色距离 <= 该值的「连通到边框」像素判为背景
FEATHER = 32   # 羽化上界：连通区域内 alpha 在 [TOL, FEATHER] 之间由 0 升到 255
MAGENTA = (255, 0, 255)


def sample_background_color(img: Image.Image) -> tuple:
    """从四边（避开四角）采样背景色，返回平均 RGB。"""
    w, h = img.size
    strip = 18
    corner_skip = 160
    pixels = []

    def add(px, py):
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


def dist(a, b):
    return math.sqrt((a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2 + (a[2] - b[2]) ** 2)


def smoothstep(edge0, edge1, x):
    t = max(0.0, min(1.0, (x - edge0) / (edge1 - edge0)))
    return t * t * (3 - 2 * t)


def cutout(src: Path, dst: Path):
    img = Image.open(src).convert("RGBA")
    w, h = img.size
    px = img.load()
    bg = sample_background_color(img)

    # 1) 洪泛填充：从四边种子出发，收集与 BG 接近且连通到边框的像素
    mask = [bytearray(w) for _ in range(h)]
    stack = []
    for x in range(w):
        stack.append((x, 0))
        stack.append((x, h - 1))
    for y in range(h):
        stack.append((0, y))
        stack.append((w - 1, y))

    while stack:
        x, y = stack.pop()
        if x < 0 or x >= w or y < 0 or y >= h:
            continue
        if mask[y][x]:
            continue
        if dist(px[x, y][:3], bg) <= TOL:
            mask[y][x] = 1
            stack.append((x + 1, y))
            stack.append((x - 1, y))
            stack.append((x, y + 1))
            stack.append((x, y - 1))

    # 2) 羽化 alpha：连通区域内越接近 BG 越透明
    for y in range(h):
        for x in range(w):
            if mask[y][x]:
                d = dist(px[x, y][:3], bg)
                a = int(255 * smoothstep(TOL, FEATHER, d))
                r, g, b, _ = px[x, y]
                px[x, y] = (r, g, b, a)

    img.save(dst, "PNG")
    return bg


def make_preview(png_paths, out: Path, cols=7, cell=170):
    rows = (len(png_paths) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * cell, rows * cell), MAGENTA)
    for i, p in enumerate(png_paths):
        im = Image.open(p).convert("RGBA").resize((cell, cell))
        sheet.paste(im, (i % cols * cell, i // cols * cell), im)
    sheet.save(out, "PNG")


def main():
    ASSET_DIR.mkdir(parents=True, exist_ok=True)
    jpgs = sorted(ASSET_DIR.glob("*.jpg"))
    pngs = []
    for jpg in jpgs:
        png = ASSET_DIR / (jpg.stem + ".png")
        bg = cutout(jpg, png)
        pngs.append(png)
        print(f"OK {jpg.name} -> {png.name}  (bg≈{bg})")

    preview = BASE / "tools" / "preview_cutout.png"
    make_preview(pngs, preview)
    print(f"preview -> {preview}")


if __name__ == "__main__":
    main()

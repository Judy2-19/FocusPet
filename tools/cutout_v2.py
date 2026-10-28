""" FocusPets cat cutout refinement v2. Enable alpha_matting for all 21 images (fg=240/bg=10/erase=10) for cleaner edges. For yellow variants, iteratively remove only outer-contour pixels close to known BG #FEF8EA (254,248,234). Yellow fur has a very different blue channel (about 53 vs 234), and enclosed white dress regions are not boundary pixels. Overwrite assets/cat transparent PNGs and generate previews/edge-residue metrics."""
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
    """ Remove background RGB contamination from translucent edges. observed=alpha*foreground+(1-alpha)*background, so foreground=(observed-(1-alpha)*background)/alpha. Recover true RGB for 0<alpha<1 to avoid beige outlines on arbitrary backgrounds."""
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
    """ Iteratively peel background-colored pixels from yellow-image outer contours, preserving yellow fur and internal white dress regions."""
    rgb = arr[..., :3].astype(float)
    d = dist_to_bg(rgb)
    bgcolored = d < 55          # Close to BG; yellow fur has distance about 183 and will not be selected.
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
    """ Measure the proportion of near-background residual pixels in the contour edge band; lower is better."""
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
        # Global edge decontamination removes translucent background outlines for yellow/white cats.
        arr = decontaminate(arr)
        if jpg.stem.startswith("yellow"):
            arr = refine_yellow(arr)
        # Gentle antialias feathering prevents hard outlines after scaling.
        res = Image.fromarray(arr, "RGBA")
        a = res.split()[3].filter(ImageFilter.GaussianBlur(0.8))
        res.putalpha(a)
        png = OUT / (jpg.stem + ".png")
        res.save(png)
        pngs.append(png)
        report.append((jpg.stem, edge_fringe_pct(arr)))
        print(f"OK {jpg.name} -> {png.name}  边缘色块={report[-1][1]:.1f}%")

    # Preview.
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

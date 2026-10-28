""" FocusPets short-tail dog cutouts: read artwork/dog/short-tail and output transparent PNGs under assets/dog/. Use lightweight rembg u2netp, alpha decontamination, and gentle feathering for pale backgrounds. Name files tail_color[_red_tie][_black_suit|red_dress][_academic_cap]. Explicit color tokens allow future variants: short tails use black_suit, long tails red_dress, caps academic_cap. Deduplicate by filename, retaining the first image, including suit/cap folder duplicates. Process only short-tail; preserve existing long-tail artwork."""
from pathlib import Path
import os
import numpy as np
from PIL import Image, ImageFilter
from rembg import new_session, remove

BASE = Path(__file__).resolve().parents[1]
IN = BASE / "artwork" / "dog" / "short-tail"
OUT = BASE / "app" / "src" / "main" / "assets" / "dog"

# Use the lightweight model to reduce inference memory versus default u2net and avoid sandbox OOM.
SESS = new_session("u2netp")

COLORS = [("white", "white"), ("gray", "gray"), ("black", "black"),
          ("yellow", "yellow"), ("pink", "pink"), ("colorful", "colorful")]


def name_for(jpg: Path) -> str:
    stem = jpg.stem
    parent = jpg.parent.name          # Red tie / suit / graduation cap / default.
    text = f"{stem} {parent}"         # Keywords may occur in parent folder names, e.g. red-tie/yellow.jpg.
    # Dog color is the earliest color word in the filename. All dogs wear black suits,
    # so a later black token must not override the leading fur color (pink/yellow/black/gray/white).
    color = None
    best = None
    for k, v in COLORS:
        idx = stem.find(k)
        if idx != -1 and (best is None or idx < best):
            best = idx
            color = v
    parts = ["short"]
    if color:
        parts.append(color)
    if "tie" in text:
        parts.append("red_tie")
    if "suit" in text:
        parts.append("black_suit")
    elif "dress" in text:
        parts.append("red_dress")
    if any(k in text for k in ("graduation-cap", "graduation-cap")):
        parts.append("academic_cap")
    return "_".join(parts)


def decontaminate(img: Image.Image) -> Image.Image:
    """ Remove gray background matte from translucent edges. rembg RGB blends fur and background according to alpha; directly feathering spreads gray into low-contrast paws. Estimate the background, recover foreground RGB, then gently feather alpha for clean soft edges."""
    arr = np.asarray(img, dtype=np.float32)
    rgb = arr[:, :, :3]
    al = arr[:, :, 3] / 255.0
    transparent = al < 0.02
    if transparent.sum() > 0:
        B = np.median(rgb[transparent].reshape(-1, 3), axis=0)
    else:
        B = np.array([128.0, 128.0, 128.0])
    eps = 0.05
    a_safe = np.maximum(al, eps)
    F = (rgb - (1.0 - al)[..., None] * B[None, None, :]) / a_safe[..., None]
    F = np.clip(F, 0, 255).astype(np.uint8)
    al_img = Image.fromarray(
        (al * 255).clip(0, 255).astype(np.uint8), "L"
    ).filter(ImageFilter.GaussianBlur(0.6))
    return Image.fromarray(np.dstack([F, np.asarray(al_img)]), "RGBA")


def main():
    saved: dict[str, bytes] = {}
    pngs = []
    for jpg in sorted(IN.rglob("*.jpg")):
        try:
            img = Image.open(jpg).convert("RGB")
            # Resize the longest edge to at most 512 before rembg to limit memory usage.
            max_side = 512
            if max(img.size) > max_side:
                scale = max_side / max(img.size)
                img = img.resize((int(img.width * scale), int(img.height * scale)), Image.LANCZOS)
            out = remove(img, session=SESS).convert("RGBA")
            # Decontaminate rembg edges, which mix dog fur with the gray background matte.
            # Blurring alpha directly spreads gray into low-contrast paws.
            # First recover true fur RGB using the estimated background,
            # then gently feather alpha for clean soft edges.
            res = decontaminate(out)

            base = name_for(jpg)
            name = base
            i = 2
            while name in saved:
                # Identical filenames indicate the same outfit/cap combination; keep the first and skip duplicates.
                print(f"SKIP dup {jpg.name} -> {name}.png")
                break
            else:
                png = OUT / (name + ".png")
                res.save(png)
                saved[name] = res.tobytes()
                pngs.append(png)
                print(f"OK   {jpg.name} -> {name}.png")
        except Exception as e:
            print(f"ERR  {jpg.name}: {e}")

    print(f"\nGenerated {len(pngs)} short-tail dog transparent PNG files -> {OUT}")


if __name__ == "__main__":
    main()

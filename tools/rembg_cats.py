""" Use rembg (U²Net) for high-quality AI cutouts of assets/cat/*.jpg into transparent PNGs."""
from pathlib import Path

from PIL import Image, ImageFilter
from rembg import remove

BASE = Path("C:/Users/13671/Desktop/MobileApp")
ASSET_DIR = BASE / "app" / "src" / "main" / "assets" / "cat"
MAGENTA = (255, 0, 255)


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
        # Overwrite existing PNGs, including earlier flood-fill results.
        img = Image.open(jpg)
        out = remove(img)
        # Gently Gaussian-blur alpha for antialiased edges, avoiding beige hard outlines after scaling.
        r, g, b, a = out.split()
        a = a.filter(ImageFilter.GaussianBlur(1.4))
        out = Image.merge("RGBA", (r, g, b, a))
        out.save(png)
        pngs.append(png)
        print(f"OK {jpg.name} -> {png.name}")

    preview = BASE / "tools" / "preview_rembg.png"
    make_preview(pngs, preview)
    print(f"preview -> {preview}")


if __name__ == "__main__":
    main()

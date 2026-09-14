"""FocusPets 小狗抠图（短尾专用重生成）：从「图片/狗/短尾巴」抠出透明 PNG 到 assets/dog/。

背景为浅色（白/米），用 rembg(u2netp 轻量模型) 去背景 + alpha 去污染（消除灰脚）+ 轻羽化。
命名按 尾巴_颜色[_red_tie][_black_suit|red_dress][_academic_cap]：
  - 领带颜色写清：当前只有红领带 -> red_tie（以后加蓝领带直接 red_tie / blue_tie 即可）
  - 短尾穿黑西服 -> black_suit；长尾穿红礼裙 -> red_dress（outfit 颜色写清）
  - 学士帽写清为 academic_cap
同名文件按「文件名」去重，保留第一张，避免重复生成 _2/_3 垃圾；
西服文件夹里已带帽的图（灰/白）与学士帽文件夹同名，会被正确去重为一张。
只处理「短尾巴」，不动长尾巴（长尾巴已正确，命名同理为 red_tie / red_dress / academic_cap）。
"""
from pathlib import Path
import os
import numpy as np
from PIL import Image, ImageFilter
from rembg import new_session, remove

BASE = Path("C:/Users/13671/Desktop/MobileApp")
IN = BASE / "图片" / "狗" / "短尾巴"
OUT = BASE / "app" / "src" / "main" / "assets" / "dog"

# 轻量模型，推理内存远小于默认 u2net（避免沙箱 OOM）
SESS = new_session("u2netp")

COLORS = [("白", "white"), ("灰", "gray"), ("黑", "black"),
          ("黄", "yellow"), ("粉", "pink"), ("彩色", "colorful")]


def name_for(jpg: Path) -> str:
    stem = jpg.stem
    parent = jpg.parent.name          # 红领带 / 西服 / 学士帽 / 默认
    text = f"{stem} {parent}"         # 关键词可能出现在父文件夹名里（如「红领带/黄.jpg」）
    # 狗的颜色 = 文件名中「最早出现」的颜色词。注意：所有狗都穿「黑西服」，
    # 「黑」字会出现在后面，不能当成狗色；真正狗色（粉/黄/黑/灰/白）在文件名最前面。
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
    if "领带" in text:
        parts.append("red_tie")
    if "西服" in text:
        parts.append("black_suit")
    elif "礼裙" in text:
        parts.append("red_dress")
    if any(k in text for k in ("学士帽", "学院帽")):
        parts.append("academic_cap")
    return "_".join(parts)


def decontaminate(img: Image.Image) -> Image.Image:
    """去除抠图后残留在半透明边缘的灰色背景(matte)。

    rembg 输出的半透明像素 RGB 是「前景毛色 + 背景灰」按 alpha 混合的结果；
    直接羽化会把这层灰晕染到脚爪等低对比区域。这里用估计出的背景色反推
    真实毛色（alpha 去污染），再对 alpha 做轻微羽化得到干净软边。
    """
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
            # 送入 rembg 前先缩到最长边 ≤512，压住显存
            max_side = 512
            if max(img.size) > max_side:
                scale = max_side / max(img.size)
                img = img.resize((int(img.width * scale), int(img.height * scale)), Image.LANCZOS)
            out = remove(img, session=SESS).convert("RGBA")
            # 去污染：rembg 的半透明边缘是「狗毛 + 灰色背景」的混合(matte)。
            # 若直接对 alpha 做高斯模糊，灰色会被晕染到低对比的脚爪区域，形成「灰脚」。
            # 这里先用估计出的背景色反推真实毛色，把灰边还原成狗本身的颜色，
            # 再对 alpha 做轻微羽化得到干净软边。
            res = decontaminate(out)

            base = name_for(jpg)
            name = base
            i = 2
            while name in saved:
                # 同名即视为同一组合（西服/学士帽文件夹里的同款图），保留第一张，跳过其余
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

    print(f"\n共生成 {len(pngs)} 张短尾小狗透明 PNG -> {OUT}")


if __name__ == "__main__":
    main()

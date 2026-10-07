"""Build every app icon, banner, and in-app logo from branding/clutch-logo.png.

Run from android/:  py tools/make-brand-assets.py
"""
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "branding" / "clutch-logo.png"
RES = ROOT / "app" / "src" / "main" / "res"
CYAN = (0, 242, 252)


def shapes(mask: Image.Image, min_pixels: int = 200):
    """Connected white shapes as (bbox, pixel set), left to right."""
    w, h = mask.size
    on = mask.load()
    seen = set()
    found = []
    for y in range(h):
        for x in range(w):
            if on[x, y] and (x, y) not in seen:
                stack, pixels = [(x, y)], []
                seen.add((x, y))
                while stack:
                    cx, cy = stack.pop()
                    pixels.append((cx, cy))
                    for nx, ny in ((cx + 1, cy), (cx - 1, cy), (cx, cy + 1), (cx, cy - 1)):
                        if 0 <= nx < w and 0 <= ny < h and on[nx, ny] and (nx, ny) not in seen:
                            seen.add((nx, ny))
                            stack.append((nx, ny))
                if len(pixels) >= min_pixels:
                    xs = [p[0] for p in pixels]
                    ys = [p[1] for p in pixels]
                    found.append(((min(xs), min(ys), max(xs) + 1, max(ys) + 1), pixels))
    return sorted(found, key=lambda s: s[0][0])


def unpremultiply(rgb: Image.Image) -> Image.Image:
    """Logo is light art on black: the brightest channel becomes alpha and the colour is
    brought back to full strength, so the cyan keeps its glow on any background."""
    out = Image.new("RGBA", rgb.size)
    src, dst = rgb.load(), out.load()
    for y in range(rgb.height):
        for x in range(rgb.width):
            r, g, b = src[x, y]
            a = max(r, g, b)
            dst[x, y] = (0, 0, 0, 0) if a == 0 else (r * 255 // a, g * 255 // a, b * 255 // a, a)
    return out


def trim(img: Image.Image, pad: int = 0) -> Image.Image:
    box = img.getchannel("A").point(lambda v: 255 if v > 8 else 0).getbbox()
    x0, y0, x1, y1 = box
    return img.crop((max(0, x0 - pad), max(0, y0 - pad), min(img.width, x1 + pad), min(img.height, y1 + pad)))


def fit(img: Image.Image, width: int = 0, height: int = 0) -> Image.Image:
    scale = min(width / img.width if width else 1e9, height / img.height if height else 1e9)
    return img.resize((max(1, round(img.width * scale)), max(1, round(img.height * scale))), Image.LANCZOS)


def paste_center(canvas: Image.Image, art: Image.Image, cx: float, cy: float) -> None:
    canvas.alpha_composite(art, (round(cx - art.width / 2), round(cy - art.height / 2)))


def rounded(img: Image.Image, radius_ratio: float) -> Image.Image:
    big = Image.new("L", (img.width * 4, img.height * 4), 0)
    ImageDraw.Draw(big).rounded_rectangle(
        (0, 0, big.width - 1, big.height - 1), radius=round(big.width * radius_ratio), fill=255,
    )
    mask = big.resize(img.size, Image.LANCZOS)
    out = img.copy()
    out.putalpha(ImageChops.multiply(out.getchannel("A"), mask))
    return out


def main() -> None:
    logo = Image.open(SOURCE).convert("RGB")
    white = logo.convert("L").point(lambda v: 255 if v > 128 else 0).convert("1")

    # The first two shapes are the "C" and the play triangle inside it.
    first = shapes(white)[:2]
    mark_mask = Image.new("L", logo.size, 0)
    mark_px = mark_mask.load()
    for _, pixels in first:
        for p in pixels:
            mark_px[p] = 255
    mark_mask = mark_mask.filter(ImageFilter.MaxFilter(5))
    mark = unpremultiply(logo)
    mark.putalpha(ImageChops.multiply(mark.getchannel("A"), mark_mask))
    mark = trim(mark)

    # Cyan swoosh under the word, glow included.
    x0, y0, x1, y1 = (0, 412, logo.width, logo.height)
    swoosh = trim(unpremultiply(logo.crop((x0, y0, x1, y1))), pad=2)

    wordmark = trim(unpremultiply(logo), pad=6)

    def icon_art(size: int, mark_height: float, swoosh_width: float, gap: float) -> Image.Image:
        canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        m = fit(mark, height=round(size * mark_height))
        s = fit(swoosh, width=round(size * swoosh_width))
        total = m.height + size * gap + s.height
        top = (size - total) / 2
        paste_center(canvas, m, size / 2, top + m.height / 2)
        paste_center(canvas, s, size / 2, top + m.height + size * gap + s.height / 2)
        return canvas

    def on_black(art: Image.Image) -> Image.Image:
        bg = Image.new("RGBA", art.size, (0, 0, 0, 255))
        bg.alpha_composite(art)
        return bg

    nodpi = RES / "drawable-nodpi"
    nodpi.mkdir(parents=True, exist_ok=True)
    on_black(icon_art(512, 0.50, 0.66, 0.05)).save(nodpi / "clutch_icon.png", optimize=True)
    fit(wordmark, width=900).save(nodpi / "clutch_wordmark.png", optimize=True)
    # Adaptive icons crop to the middle ~66%, so the art sits well inside it.
    icon_art(432, 0.34, 0.46, 0.035).save(nodpi / "clutch_launcher_foreground.png", optimize=True)

    for density, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        folder = RES / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        art = on_black(icon_art(px * 4, 0.48, 0.64, 0.05))
        rounded(art, 0.22).resize((px, px), Image.LANCZOS).save(folder / "ic_launcher.png", optimize=True)
        rounded(art, 0.5).resize((px, px), Image.LANCZOS).save(folder / "ic_launcher_round.png", optimize=True)

    for density, (bw, bh) in {"xhdpi": (320, 180), "xxxhdpi": (640, 360)}.items():
        folder = RES / f"drawable-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        banner = Image.new("RGBA", (bw, bh), (0, 0, 0, 255))
        paste_center(banner, fit(wordmark, width=round(bw * 0.8), height=round(bh * 0.62)), bw / 2, bh / 2)
        banner.save(folder / "clutch_banner.png", optimize=True)


if __name__ == "__main__":
    main()

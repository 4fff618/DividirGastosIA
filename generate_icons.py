"""
Genera los íconos de launcher de DividirGastosIA a partir de icono.png
(logo negro sobre fondo transparente).

Genera en app/src/main/res/:
  - mipmap-{mdpi..xxxhdpi}/ic_launcher.png             (legacy: fondo + logo)
  - mipmap-{mdpi..xxxhdpi}/ic_launcher_round.png       (legacy circular)
  - mipmap-{mdpi..xxxhdpi}/ic_launcher_foreground.png  (adaptive: logo en zona segura)
  - mipmap-{mdpi..xxxhdpi}/ic_launcher_monochrome.png  (adaptive: silueta blanca)

Uso: python generate_icons.py
"""
import os
from PIL import Image, ImageDraw

PROJECT = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(PROJECT, "icono.png")
RES = os.path.join(PROJECT, "app", "src", "main", "res")

DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
LEGACY_DP = 48            # tamaño del ícono clásico
ADAPTIVE_DP = 108         # canvas del ícono adaptativo
SAFE_RATIO = 0.62         # el logo cabe en la zona segura (66/108 ≈ 0.61)
LEGACY_LOGO_RATIO = 0.80  # margen para el ícono clásico
BG = (255, 255, 255, 255)  # fondo blanco: contrasta con el logo negro


def load_logo() -> Image.Image:
    """Carga icono.png, recorta el margen transparente y lo centra en un cuadrado."""
    src = Image.open(SRC).convert("RGBA")
    logo = src.crop(src.getbbox())
    side = max(logo.size)
    square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    square.paste(logo, ((side - logo.width) // 2, (side - logo.height) // 2), logo)
    return square


def scaled(logo: Image.Image, canvas_px: int, ratio: float) -> Image.Image:
    target = round(canvas_px * ratio)
    return logo.resize((target, target), Image.LANCZOS)


def legacy(logo: Image.Image, px: int, round_mask: bool = False) -> Image.Image:
    """Ícono clásico: fondo opaco + logo centrado (con máscara circular si round)."""
    canvas = Image.new("RGBA", (px, px), BG)
    glyph = scaled(logo, px, LEGACY_LOGO_RATIO)
    canvas.alpha_composite(glyph, ((px - glyph.width) // 2, (px - glyph.height) // 2))
    if round_mask:
        mask = Image.new("L", (px, px), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, px - 1, px - 1), fill=255)
        canvas.putalpha(mask)
    return canvas


def adaptive(logo: Image.Image, px: int, mono: bool = False) -> Image.Image:
    """Capa adaptive: canvas transparente con el logo en la zona segura."""
    canvas = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    glyph = scaled(logo, px, SAFE_RATIO)
    if mono:
        white = Image.new("RGBA", glyph.size, (255, 255, 255, 255))
        white.putalpha(glyph.getchannel("A"))
        glyph = white
    canvas.alpha_composite(glyph, ((px - glyph.width) // 2, (px - glyph.height) // 2))
    return canvas


def main() -> None:
    logo = load_logo()
    for dpi, mult in DENSITIES.items():
        folder = os.path.join(RES, "mipmap-" + dpi)
        os.makedirs(folder, exist_ok=True)
        legacy_px = round(LEGACY_DP * mult)
        adaptive_px = round(ADAPTIVE_DP * mult)
        legacy(logo, legacy_px).save(os.path.join(folder, "ic_launcher.png"))
        legacy(logo, legacy_px, round_mask=True).save(
            os.path.join(folder, "ic_launcher_round.png"))
        adaptive(logo, adaptive_px).save(
            os.path.join(folder, "ic_launcher_foreground.png"))
        adaptive(logo, adaptive_px, mono=True).save(
            os.path.join(folder, "ic_launcher_monochrome.png"))
        print(f"{dpi}: legacy {legacy_px}px, adaptive {adaptive_px}px")


if __name__ == "__main__":
    main()
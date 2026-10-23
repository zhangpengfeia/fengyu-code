"""Create web assets from the original PNGs. Requires Pillow with WebP support."""

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "public/assets"
OUTPUT = ROOT / "src/assets/optimized"
LIMITS = {
    "scene-1": 1280,
    "scene-14": 1440,
    "scene-0": 1600,
    "scene-10": 640,
    **dict.fromkeys(
        [
            "planet-9", "planet-8", "scene-5", "planet-4", "planet-1", "planet-20",
            "planet-0", "planet-7", "planet-3", "planet-5", "planet-19", "planet-6",
        ],
        768,
    ),
}

OUTPUT.mkdir(parents=True, exist_ok=True)
before = after = 0
for name, limit in LIMITS.items():
    source = SOURCE / f"{name}.png"
    target = OUTPUT / f"{name}.webp"
    with Image.open(source) as image:
        image = image.convert("RGBA")
        image.thumbnail((limit, limit), Image.Resampling.LANCZOS)
        image.save(target, "WEBP", quality=90, method=6)
    before += source.stat().st_size
    after += target.stat().st_size
    print(f"{name}: {source.stat().st_size:,} -> {target.stat().st_size:,} bytes")
print(f"Total: {before:,} -> {after:,} bytes ({1 - after / before:.1%} smaller)")

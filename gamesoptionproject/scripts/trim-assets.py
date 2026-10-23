"""Trim transparent PDF page margins from planet PNGs (requires Pillow)."""

from pathlib import Path
import json
from PIL import Image

root = Path(__file__).resolve().parent.parent / "public" / "assets"
manifest_path = root / "manifest.json"
manifest = json.loads(manifest_path.read_text())
for item in manifest:
    if not item["file"].startswith("planet-") or item["file"] == "planet-10.png":
        continue
    path = root / item["file"]
    with Image.open(path) as image:
        bounds = image.getbbox()
        if bounds is None or bounds == (0, 0, image.width, image.height):
            continue
        cropped = image.crop(bounds)
        item.setdefault("pdfRenderSize", [image.width, image.height])
        item["width"], item["height"] = cropped.size
        cropped.save(path, optimize=True)
manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")

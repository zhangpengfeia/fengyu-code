"""Render book catalogue PDF slices to transparent WebP (Pillow + pypdfium2)."""

import json
from pathlib import Path
import sys

from PIL import Image
import pypdfium2 as pdfium

root = Path(sys.argv[1])
project = Path(__file__).resolve().parent.parent
output = project / "src/assets/catalog"
output.mkdir(parents=True, exist_ok=True)
slices = {
    "book-03-planet": ("第三册森林2/蒙版分组.bitmap.pdf", 1440),
    "book-03-landscape": ("第三册森林2/分组 10.bitmap.pdf", 1440),
    "book-03-game": ("第三册森林2/分组 18.bitmap.pdf", 800),
    "book-05-planet": ("第五册水果2/蒙版分组.bitmap.pdf", 1440),
    "book-05-landscape": ("第五册水果2/分组 11.bitmap.pdf", 1440),
    "book-05-game": ("第五册水果2/分组 1.bitmap.pdf", 800),
    "back": ("第三册森林2/分组 14.bitmap.pdf", 128),
}
manifest = []
for name, (relative, limit) in slices.items():
    document = pdfium.PdfDocument(str(root / relative))
    page = document[0]
    width, height = page.get_size()
    bitmap = page.render(scale=limit / max(width, height), fill_color=(0, 0, 0, 0))
    image = bitmap.to_pil().convert("RGBA")
    bounds = image.getbbox()
    if bounds:
        image = image.crop(bounds)
    image.thumbnail((limit, limit), Image.Resampling.LANCZOS)
    target = output / f"{name}.webp"
    image.save(target, "WEBP", quality=90, method=6)
    manifest.append({"file": target.name, "source": relative, "width": image.width, "height": image.height})
    print(f"{name}: {image.size}, {target.stat().st_size:,} bytes")
(project / "public/assets/catalog-manifest.json").write_text(
    json.dumps(manifest, ensure_ascii=False, indent=2) + "\n"
)

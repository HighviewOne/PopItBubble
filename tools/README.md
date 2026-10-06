# tools

Python scripts that generate the images in `docs/assets/` used by the README
and the landing page. They draw the bubbles with Pillow to match the app's
look; they don't run the app.

| Script | Output |
|---|---|
| `generate_banner.py` | `docs/assets/banner.png` |
| `generate_screenshot.py` | `docs/assets/screenshot.png` |
| `generate_demo.py` | `docs/assets/demo.gif` |

Requires Python 3 and Pillow (`pip install pillow`). `generate_banner.py` also
expects the DejaVu Sans Bold font at
`/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf`.

Run from the repository root, since output paths are relative to it:

```bash
python3 tools/generate_banner.py
```

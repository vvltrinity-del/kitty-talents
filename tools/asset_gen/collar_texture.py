"""Generate textures/entity/cat/collar.png: a full ring around the cat's neck plus a tag under the chin.

The vanilla collar texture only covers the back and part of the sides of the body box, so from the front it looks cut
off. Layout follows the body box (texOffs 20,0; 4 wide, 16 long, 6 deep on a 64x32 sheet): the first row of the four
side faces (v = 6) is the ring right behind the head. Greyscale so the dye tint colours it.
Usage: python tools/asset_gen/collar_texture.py
"""
import pathlib
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/kittytalents/textures/entity/cat/collar.png"

im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
BAND = [(188, 188, 188, 255), (182, 182, 182, 255)]
for x in range(20, 40):  # right side 20-25, belly 26-29, left side 30-35, back 36-39
    im.putpixel((x, 6), BAND[x % 2])
# Tag hanging under the chin, on the belly face just behind the band.
im.putpixel((27, 7), (232, 232, 232, 255))
im.putpixel((28, 7), (214, 214, 214, 255))
OUT.parent.mkdir(parents=True, exist_ok=True)
im.save(OUT)
print("wrote", OUT)

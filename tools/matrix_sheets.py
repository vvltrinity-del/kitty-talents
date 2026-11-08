"""Stacks render-matrix strips into labelled contact sheets: python -I tools/matrix_sheets.py <matrix dir> <out dir> [per_sheet]"""
import sys, os
from PIL import Image, ImageDraw
src, out = sys.argv[1], sys.argv[2]
per = int(sys.argv[3]) if len(sys.argv) > 3 else 5
os.makedirs(out, exist_ok=True)
files = sorted(f for f in os.listdir(src) if f.endswith('.png'))
for n in range(0, len(files), per):
    group = files[n:n + per]
    imgs = [Image.open(os.path.join(src, f)).convert('RGB') for f in group]
    w = max(i.width for i in imgs); h = sum(i.height for i in imgs)
    sheet = Image.new('RGB', (w, h), (40, 40, 40))
    y = 0
    for f, i in zip(group, imgs):
        sheet.paste(i, (0, y)); ImageDraw.Draw(sheet).text((6, y + 4), f[:-4], fill=(0, 0, 0)); y += i.height
    sheet.save(os.path.join(out, f'sheet_{n // per + 1}.png'))
print(len(files), 'strips ->', (len(files) + per - 1) // per, 'sheets')

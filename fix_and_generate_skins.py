from PIL import Image, ImageDraw

def fix_snow_leopard():
    path = "src/main/resources/assets/kittytalents/textures/entity/cat/skin_12.png"
    img = Image.open(path).convert("RGBA")
    
    # Clean up side of head so no eye pixels wrap awkwardly onto the cheeks
    # Left cheek: u=0..5, v=5..9
    fur_side_1 = (165, 172, 182, 255)
    fur_side_spot = (130, 138, 148, 255)
    for x in range(0, 5):
        for y in range(5, 9):
            # If pixel is bluish eye color, replace with proper coat
            c = img.getpixel((x, y))
            if c[2] > 200 and c[0] < 150:
                img.putpixel((x, y), fur_side_spot if (x + y) % 2 == 0 else fur_side_1)

    # Right cheek: u=10..15, v=5..9
    for x in range(10, 15):
        for y in range(5, 9):
            c = img.getpixel((x, y))
            if c[2] > 200 and c[0] < 150:
                img.putpixel((x, y), fur_side_spot if (x + y) % 2 == 0 else fur_side_1)

    # Re-draw the Face: u=5..10, v=5..9
    # y=5 (Forehead with subtle spots)
    img.putpixel((5, 5), (160, 168, 178, 255))
    img.putpixel((6, 5), (135, 142, 152, 255))
    img.putpixel((7, 5), (225, 230, 238, 255))
    img.putpixel((8, 5), (135, 142, 152, 255))
    img.putpixel((9, 5), (160, 168, 178, 255))

    # y=6 (EYES - Exactly at x=6 and x=8!)
    bright_blue = (35, 155, 255, 255)
    dark_pupil = (20, 95, 185, 255)
    white_sclera = (245, 248, 255, 255)
    bridge_fur = (220, 226, 235, 255)

    img.putpixel((5, 6), white_sclera)
    img.putpixel((6, 6), bright_blue)
    img.putpixel((7, 6), bridge_fur)
    img.putpixel((8, 6), bright_blue)
    img.putpixel((9, 6), white_sclera)

    # y=7 (Whisker pads)
    img.putpixel((5, 7), (220, 226, 235, 255))
    img.putpixel((6, 7), (235, 240, 248, 255))
    img.putpixel((7, 7), (220, 226, 235, 255))
    img.putpixel((8, 7), (235, 240, 248, 255))
    img.putpixel((9, 7), (220, 226, 235, 255))

    # y=8 (Muzzle line / chin)
    img.putpixel((5, 8), (170, 178, 188, 255))
    img.putpixel((6, 8), (235, 240, 248, 255))
    img.putpixel((7, 8), (245, 248, 252, 255))
    img.putpixel((8, 8), (235, 240, 248, 255))
    img.putpixel((9, 8), (170, 178, 188, 255))

    # Nose front: u=2..5, v=26..28
    pink_nose = (210, 135, 145, 255)
    muzzle_white = (240, 245, 250, 255)
    img.putpixel((2, 26), (220, 226, 235, 255))
    img.putpixel((3, 26), pink_nose)
    img.putpixel((4, 26), (220, 226, 235, 255))

    img.putpixel((2, 27), muzzle_white)
    img.putpixel((3, 27), muzzle_white)
    img.putpixel((4, 27), muzzle_white)

    img.save(path)
    print("Fixed Snow Leopard skin_12.png successfully.")

def create_panther_skin():
    # Base 64x32 cat texture
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    black_base = (20, 20, 26, 255)
    black_sheen = (30, 32, 42, 255)
    black_dark = (12, 12, 16, 255)
    blue_eye = (35, 160, 255, 255)
    eye_corner = (65, 80, 110, 255)
    nose_dark = (45, 38, 48, 255)

    # Fill body, head, legs with silky black textures
    # Head box (0..20, 0..10)
    for x in range(0, 20):
        for y in range(0, 10):
            img.putpixel((x, y), black_sheen if (x + y * 3) % 4 == 0 else black_base)

    # Head face: u=5..10, v=5..9
    img.putpixel((5, 5), black_base)
    img.putpixel((6, 5), black_dark)
    img.putpixel((7, 5), black_sheen)
    img.putpixel((8, 5), black_dark)
    img.putpixel((9, 5), black_base)

    # Eyes at x=6, x=8
    img.putpixel((5, 6), eye_corner)
    img.putpixel((6, 6), blue_eye)
    img.putpixel((7, 6), black_sheen)
    img.putpixel((8, 6), blue_eye)
    img.putpixel((9, 6), eye_corner)

    img.putpixel((5, 7), black_base)
    img.putpixel((6, 7), black_sheen)
    img.putpixel((7, 7), black_base)
    img.putpixel((8, 7), black_sheen)
    img.putpixel((9, 7), black_base)

    img.putpixel((5, 8), black_dark)
    img.putpixel((6, 8), black_base)
    img.putpixel((7, 8), black_sheen)
    img.putpixel((8, 8), black_base)
    img.putpixel((9, 8), black_dark)

    # Ears: left u=0..2, v=10..12; right u=6..8, v=10..12
    for x in range(0, 3):
        for y in range(10, 12):
            img.putpixel((x, y), black_dark)
    for x in range(6, 9):
        for y in range(10, 12):
            img.putpixel((x, y), black_dark)

    # Nose box u=0..8, v=24..28
    for x in range(0, 8):
        for y in range(24, 28):
            img.putpixel((x, y), black_base)
    # Nose front u=2..5, v=26..28
    img.putpixel((3, 26), nose_dark)

    # Body: u=20..40, v=0..22
    for x in range(20, 40):
        for y in range(0, 22):
            img.putpixel((x, y), black_sheen if (x * 2 + y) % 5 == 0 else black_base)

    # Tail 1: u=0..8, v=15..23
    for x in range(0, 8):
        for y in range(15, 23):
            img.putpixel((x, y), black_dark if y > 20 else black_base)

    # Tail 2: u=0..8, v=23..31
    for x in range(0, 8):
        for y in range(23, 31):
            img.putpixel((x, y), black_dark)

    # Legs: u=40..48, v=0..10; u=48..56, v=0..10; u=40..48, v=10..20; u=48..56, v=10..20
    for x in range(40, 60):
        for y in range(0, 22):
            img.putpixel((x, y), black_dark if y > 18 else (black_sheen if (x + y) % 3 == 0 else black_base))

    out_path = "src/main/resources/assets/kittytalents/textures/entity/cat/skin_16.png"
    img.save(out_path)
    out_path2 = "src/main/resources/assets/kittytalents/textures/entity/cat/panther.png"
    img.save(out_path2)
    print("Created Panther textures successfully.")

def create_item_textures():
    # 1. kitty_work_clipboard.png (16x16)
    img_clip = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img_clip)
    # Wooden clipboard board
    d.rectangle([2, 1, 13, 14], fill=(139, 90, 43, 255))
    d.rectangle([3, 2, 12, 13], fill=(185, 122, 60, 255))
    # Paper
    d.rectangle([4, 4, 11, 12], fill=(245, 245, 240, 255))
    # Clip at top
    d.rectangle([6, 1, 9, 3], fill=(160, 165, 175, 255))
    d.rectangle([7, 2, 8, 2], fill=(80, 85, 95, 255))
    # Written lines on paper with cat paw mark
    d.line([5, 6, 10, 6], fill=(70, 70, 80, 255))
    d.line([5, 8, 10, 8], fill=(70, 70, 80, 255))
    d.line([5, 10, 8, 10], fill=(70, 70, 80, 255))
    # Little golden paw stamp
    img_clip.putpixel((10, 10), (220, 170, 40, 255))
    img_clip.save("src/main/resources/assets/kittytalents/textures/item/kitty_work_clipboard.png")
    print("Saved kitty_work_clipboard.png")

    # 2. variant_chart.png (16x16)
    img_chart = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d2 = ImageDraw.Draw(img_chart)
    # Leather/parchment scroll
    d2.rectangle([2, 2, 13, 13], fill=(230, 205, 160, 255))
    d2.rectangle([3, 3, 12, 12], fill=(245, 225, 185, 255))
    # Gold trim / borders
    d2.rectangle([2, 2, 13, 2], fill=(180, 130, 40, 255))
    d2.rectangle([2, 13, 13, 13], fill=(180, 130, 40, 255))
    # Genealogical tree nodes and lines
    img_chart.putpixel((5, 5), (60, 120, 220, 255)) # Blue node
    img_chart.putpixel((10, 5), (220, 60, 60, 255)) # Red node
    img_chart.putpixel((7, 7), (180, 140, 50, 255)) # Line split
    img_chart.putpixel((8, 7), (180, 140, 50, 255))
    img_chart.putpixel((7, 9), (40, 180, 80, 255)) # Green child node
    img_chart.putpixel((8, 9), (40, 180, 80, 255))
    img_chart.putpixel((7, 11), (180, 60, 220, 255)) # Purple legendary node
    img_chart.save("src/main/resources/assets/kittytalents/textures/item/variant_chart.png")
    print("Saved variant_chart.png")

fix_snow_leopard()
create_panther_skin()
create_item_textures()

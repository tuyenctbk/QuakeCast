import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def create_app_icon(output_path="ic_launcher_512.png"):
    size = 512
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Background: Deep tech dark navy gradient with subtle radial glow
    cx, cy = size / 2.0, size / 2.0
    for r in range(int(size * 0.8), 0, -2):
        factor = r / (size * 0.8)
        # Deep space navy #090E17 to #0F1C30
        red = int(9 + (18 - 9) * (1 - factor))
        green = int(14 + (32 - 14) * (1 - factor))
        blue = int(23 + (55 - 23) * (1 - factor))
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(red, green, blue, 255))

    # Base background rect to fill corners
    bg = Image.new("RGBA", (size, size), (9, 14, 23, 255))
    bg.paste(img, (0, 0), img)
    img = bg
    draw = ImageDraw.Draw(img)

    # Concentric seismic shockwaves (rings)
    # Ring 1 (outer red glow)
    r1 = 195
    for w in range(6, 0, -1):
        alpha = int(40 + (6 - w) * 20)
        draw.ellipse([cx - r1 - w, cy - r1 - w, cx + r1 + w, cy + r1 + w], outline=(255, 23, 68, alpha), width=2)
    draw.ellipse([cx - r1, cy - r1, cx + r1, cy + r1], outline=(255, 45, 85, 220), width=4)

    # Ring 2 (mid amber warning)
    r2 = 145
    for w in range(5, 0, -1):
        alpha = int(35 + (5 - w) * 22)
        draw.ellipse([cx - r2 - w, cy - r2 - w, cx + r2 + w, cy + r2 + w], outline=(255, 145, 0, alpha), width=2)
    draw.ellipse([cx - r2, cy - r2, cx + r2, cy + r2], outline=(255, 160, 20, 240), width=4)

    # Central Globe Base Sphere
    r_globe = 95
    # Globe gradient (cyan / oceanic deep glow)
    for r in range(r_globe, 0, -1):
        f = r / r_globe
        cr = int(0 + (10 - 0) * f)
        cg = int(180 * (1 - f*0.4))
        cb = int(240 * (1 - f*0.2))
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(cr, cg, cb, 255))

    # Globe Grids (parallels and meridians)
    grid_color = (15, 30, 60, 180)
    # Equator & parallels
    draw.line([cx - r_globe + 5, cy, cx + r_globe - 5, cy], fill=grid_color, width=3)
    draw.ellipse([cx - r_globe * 0.88, cy - r_globe * 0.45, cx + r_globe * 0.88, cy + r_globe * 0.45], outline=grid_color, width=2)
    draw.ellipse([cx - r_globe * 0.88, cy - r_globe * 0.8, cx + r_globe * 0.88, cy + r_globe * 0.1], outline=grid_color, width=2)
    draw.ellipse([cx - r_globe * 0.88, cy - r_globe * 0.1, cx + r_globe * 0.88, cy + r_globe * 0.8], outline=grid_color, width=2)
    # Central Meridian
    draw.ellipse([cx - r_globe * 0.45, cy - r_globe, cx + r_globe * 0.45, cy + r_globe], outline=grid_color, width=2)
    draw.line([cx, cy - r_globe + 3, cx, cy + r_globe - 3], fill=grid_color, width=3)

    # Epicenter Beacon (pulsing core dot)
    r_epicenter = 18
    # Outer glow of epicenter
    for eg in range(36, 0, -2):
        alpha = int(180 * (1 - eg / 36.0))
        draw.ellipse([cx - eg, cy - eg, cx + eg, cy + eg], fill=(255, 23, 68, alpha))
    draw.ellipse([cx - r_epicenter, cy - r_epicenter, cx + r_epicenter, cy + r_epicenter], fill=(255, 30, 75, 255))
    # Bright center highlight
    draw.ellipse([cx - 7, cy - 7, cx + 7, cy + 7], fill=(255, 255, 255, 255))

    # Seismic waveform / shockwave pulse crossing below
    seismo_y = 390
    wave_pts = [
        (40, seismo_y),
        (130, seismo_y),
        (160, seismo_y - 12),
        (185, seismo_y + 18),
        (210, seismo_y - 35),
        (235, seismo_y + 45),
        (256, seismo_y - 85),
        (275, seismo_y + 70),
        (300, seismo_y - 45),
        (325, seismo_y + 25),
        (350, seismo_y - 10),
        (380, seismo_y),
        (472, seismo_y)
    ]

    # Wave glow
    for gw in (10, 6, 4):
        draw.line(wave_pts, fill=(0, 229, 255, 60), width=gw, joint="curve")
    draw.line(wave_pts, fill=(255, 255, 255, 255), width=4, joint="curve")

    # Corner subtle vignette / border
    draw.rounded_rectangle([2, 2, size - 3, size - 3], radius=110, outline=(0, 229, 255, 70), width=3)

    img.save(output_path, "PNG")
    print(f"Created app icon at {output_path}")


def create_feature_graphic(output_path="feature_graphic_1024x500.png"):
    width, height = 1024, 500
    img = Image.new("RGBA", (width, height), (8, 14, 25, 255))
    draw = ImageDraw.Draw(img)

    # Tech grid background
    grid_spacing = 40
    for x in range(0, width, grid_spacing):
        draw.line([(x, 0), (x, height)], fill=(16, 28, 48, 140), width=1)
    for y in range(0, height, grid_spacing):
        draw.line([(0, y), (width, y)], fill=(16, 28, 48, 140), width=1)

    # Right side 3D Globe visualization
    gcx, gcy = 760, 250
    gradius = 165

    # Globe shockwave rings
    for r, col, w in [(240, (255, 23, 68, 60), 2), (210, (255, 145, 0, 80), 3), (185, (0, 229, 255, 100), 2)]:
        draw.ellipse([gcx - r, gcy - r, gcx + r, gcy + r], outline=col, width=w)

    # Globe sphere
    for r in range(gradius, 0, -1):
        f = r / gradius
        cr = int(4 + 10 * (1 - f))
        cg = int(25 + 130 * (1 - f))
        cb = int(55 + 180 * (1 - f))
        draw.ellipse([gcx - r, gcy - r, gcx + r, gcy + r], fill=(cr, cg, cb, 255))

    # Globe grid lines
    ggrid = (10, 35, 70, 200)
    draw.line([gcx - gradius + 5, gcy, gcx + gradius - 5, gcy], fill=ggrid, width=3)
    draw.ellipse([gcx - gradius * 0.9, gcy - gradius * 0.45, gcx + gradius * 0.9, gcy + gradius * 0.45], outline=ggrid, width=2)
    draw.ellipse([gcx - gradius * 0.9, gcy - gradius * 0.8, gcx + gradius * 0.9, gcy + gradius * 0.1], outline=ggrid, width=2)
    draw.ellipse([gcx - gradius * 0.9, gcy - gradius * 0.1, gcx + gradius * 0.9, gcy + gradius * 0.8], outline=ggrid, width=2)
    draw.ellipse([gcx - gradius * 0.45, gcy - gradius, gcx + gradius * 0.45, gcy + gradius], outline=ggrid, width=2)
    draw.line([gcx, gcy - gradius + 4, gcx, gcy + gradius - 4], fill=ggrid, width=3)

    # Epicenter tremors on globe
    quakes = [
        (gcx - 30, gcy - 20, 16, (255, 23, 68), "M 7.2"),
        (gcx + 45, gcy + 35, 12, (255, 145, 0), "M 5.8"),
        (gcx - 65, gcy + 50, 9, (0, 229, 255), "M 4.6"),
    ]
    for qx, qy, qr, qcol, label in quakes:
        draw.ellipse([qx - qr * 2, qy - qr * 2, qx + qr * 2, qy + qr * 2], outline=qcol + (90,), width=2)
        draw.ellipse([qx - qr, qy - qr, qx + qr, qy + qr], fill=qcol + (240,))
        draw.ellipse([qx - 4, qy - 4, qx + 4, qy + 4], fill=(255, 255, 255, 255))

    # Real-time Seismograph waveform along bottom
    w_pts = [
        (40, 430), (180, 430), (220, 415), (250, 450), (280, 370), (310, 480),
        (340, 340), (370, 460), (400, 390), (430, 440), (470, 425), (510, 430),
        (650, 430), (700, 410), (740, 445), (780, 430), (980, 430)
    ]
    draw.line(w_pts, fill=(0, 229, 255, 80), width=8, joint="curve")
    draw.line(w_pts, fill=(255, 255, 255, 230), width=3, joint="curve")

    # Typography & Branding (Left Side)
    # App Title & Subtitles using standard drawing or fallback fonts
    try:
        font_title = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 62)
        font_sub = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 24)
        font_badge = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial.ttf", 16)
        font_body = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial.ttf", 18)
    except:
        font_title = ImageFont.load_default()
        font_sub = font_title
        font_badge = font_title
        font_body = font_title

    # Live pulse dot before title
    draw.ellipse([54, 76, 74, 96], fill=(255, 23, 68, 255))
    draw.ellipse([50, 72, 78, 100], outline=(255, 23, 68, 120), width=3)
    draw.text((88, 56), "QuakeCast", fill=(255, 255, 255, 255), font=font_title)

    # Subtitle
    draw.text((55, 142), "LIVE SEISMIC MONITOR & 3D GLOBE", fill=(0, 229, 255, 255), font=font_sub)

    # Description points
    bullets = [
        "✓  Real-Time Official USGS Seismic Telemetry",
        "✓  Interactive 3D Canvas Globe & Hypocenter Inspection",
        "✓  Intelligent Proximity Warnings with Wave Arrival Physics",
        "✓  Optimized for Mobile, Tablets & Android TV Screen Saver"
    ]
    for idx, b in enumerate(bullets):
        draw.text((55, 195 + idx * 32), b, fill=(200, 215, 235, 255), font=font_body)

    # Feature Badges
    badges = [
        ("USGS DIRECT FEED", (0, 229, 255)),
        ("3D SHOCKWAVES", (255, 145, 0)),
        ("ANDROID TV READY", (100, 220, 100)),
        ("100% AD-FREE", (255, 23, 68))
    ]
    bx = 55
    for text, col in badges:
        bw = len(text) * 9 + 20
        draw.rounded_rectangle([bx, 340, bx + bw, 372], radius=6, fill=(col[0], col[1], col[2], 35), outline=col + (180,), width=1)
        draw.text((bx + 10, 348), text, fill=col + (255,), font=font_badge)
        bx += bw + 14

    img.save(output_path, "PNG")
    print(f"Created feature graphic at {output_path}")

if __name__ == "__main__":
    create_app_icon()
    create_feature_graphic()


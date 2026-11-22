#!/usr/bin/env python3
import os
from PIL import Image, ImageDraw, ImageFont


def create_full_bleed_icon(size=1024):
    """
    Create a 100% opaque, full-bleed square icon with NO baked-in rounded corners or borders,
    matching the adaptive icon vector design with proper alpha compositing.
    Every element stays cleanly inside the circular safe zone while the rich sapphire-blue
    radial gradient background fills 100% of the icon space edge-to-edge.
    """
    img = Image.new("RGBA", (size, size), (12, 47, 90, 255))
    draw = ImageDraw.Draw(img)
    cx = size / 2.0
    cy = size * 0.475
    s = size / 72.0  # Maps the 72dp visible safe-area cleanly onto the full 512/1024px icon canvas

    # 1. Full-bleed rich sapphire-ocean radial gradient (high contrast vs dark Google TV launcher)
    max_r = int(size * 0.80)
    for r in range(max_r, 0, -4):
        f = min(1.0, r / (size * 0.62))
        if f < 0.55:
            t = f / 0.55
            red = int(25 * (1 - t) + 17 * t)
            green = int(103 * (1 - t) + 70 * t)
            blue = int(178 * (1 - t) + 128 * t)
        else:
            t = (f - 0.55) / 0.45
            red = int(17 * (1 - t) + 12 * t)
            green = int(70 * (1 - t) + 47 * t)
            blue = int(128 * (1 - t) + 90 * t)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(red, green, blue, 255))

    # 2. Subtle alpha-blended outer ambient radar rings
    ambient = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    adraw = ImageDraw.Draw(ambient)
    for ring_dp, alpha in [(34.0, 38), (42.0, 22)]:
        rr = ring_dp * s
        adraw.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], outline=(0, 229, 255, alpha), width=max(2, int(1.0 * s)))
    img = Image.alpha_composite(img, ambient)

    # 3. Concentric shockwave rings (centered on globe at (cx, cy))
    rings = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    rdraw = ImageDraw.Draw(rings)
    r1 = 27.5 * s
    rdraw.ellipse([cx - r1, cy - r1, cx + r1, cy + r1], outline=(255, 42, 85, 220), width=max(2, int(2.0 * s)))
    r2 = 21.8 * s
    rdraw.ellipse([cx - r2, cy - r2, cx + r2, cy + r2], outline=(255, 171, 0, 238), width=max(2, int(2.2 * s)))
    img = Image.alpha_composite(img, rings)

    # 4. 3D Earth Globe Sphere
    globe = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(globe)
    r_globe = 16.0 * s
    for r in range(int(r_globe), 0, -2):
        f = r / r_globe
        cr = int(0 + 10 * f)
        cg = int(216 * (1 - f * 0.60))
        cb = int(245 * (1 - f * 0.42))
        gdraw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(cr, cg, cb, 255))
    gdraw.ellipse(
        [cx - r_globe, cy - r_globe, cx + r_globe, cy + r_globe],
        outline=(0, 229, 255, 255),
        width=max(2, int(1.8 * s)),
    )
    img = Image.alpha_composite(img, globe)

    # 5. Subtle Earth meridians & equator (alpha-blended)
    mer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    mdraw = ImageDraw.Draw(mer)
    mer_rx = 7.6 * s
    mdraw.ellipse(
        [cx - mer_rx, cy - r_globe, cx + mer_rx, cy + r_globe],
        outline=(255, 255, 255, 125),
        width=max(1, int(1.0 * s)),
    )
    mdraw.line([cx - r_globe, cy, cx + r_globe, cy], fill=(255, 255, 255, 110), width=max(1, int(0.9 * s)))
    mdraw.line([cx, cy - r_globe, cx, cy + r_globe], fill=(255, 255, 255, 95), width=max(1, int(0.9 * s)))
    img = Image.alpha_composite(img, mer)

    # 6. Epicenter Pulsating Core (blended glow + solid crimson + crisp white center)
    epi_glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    eg_draw = ImageDraw.Draw(epi_glow)
    r_glow = 7.8 * s
    eg_draw.ellipse([cx - r_glow, cy - r_glow, cx + r_glow, cy + r_glow], fill=(255, 23, 68, 95))
    img = Image.alpha_composite(img, epi_glow)

    epi = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    e_draw = ImageDraw.Draw(epi)
    r_epi = 4.8 * s
    r_core = 2.1 * s
    e_draw.ellipse([cx - r_epi, cy - r_epi, cx + r_epi, cy + r_epi], fill=(255, 23, 68, 255))
    e_draw.ellipse([cx - r_core, cy - r_core, cx + r_core, cy + r_core], fill=(255, 255, 255, 255))
    img = Image.alpha_composite(img, epi)

    # 7. Crisp Seismograph Waveform cleanly inside the circular safe zone
    def pt(dx, dy):
        return (cx + dx * s, cy + dy * s)

    wave_pts = [
        pt(-22, 23),
        pt(-14, 23),
        pt(-11, 18.5),
        pt(-7.5, 27),
        pt(-3.5, 13.5),
        pt(1.0, 29),
        pt(5.0, 18.5),
        pt(8.5, 23),
        pt(22, 23),
    ]
    w_glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(w_glow).line(wave_pts, fill=(0, 229, 255, 135), width=max(2, int(4.0 * s)), joint="curve")
    img = Image.alpha_composite(img, w_glow)

    w_core = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(w_core).line(wave_pts, fill=(255, 255, 255, 255), width=max(2, int(2.1 * s)), joint="curve")
    img = Image.alpha_composite(img, w_core)

    return img.convert("RGB")


def create_app_icon(output_path="ic_launcher_512.png"):
    hi_res = create_full_bleed_icon(1024)
    icon_512 = hi_res.resize((512, 512), Image.Resampling.LANCZOS)
    icon_512.save(output_path, "PNG", optimize=True)
    print(f"Created full-bleed opaque 512x512 store icon at {output_path}")

    mipmaps = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for folder, px in mipmaps.items():
        out_dir = os.path.join("app/src/main/res", folder)
        os.makedirs(out_dir, exist_ok=True)

        sq = hi_res.resize((px, px), Image.Resampling.LANCZOS)
        sq.save(os.path.join(out_dir, "ic_launcher.png"), "PNG", optimize=True)

        round_img = sq.convert("RGBA")
        mask = Image.new("L", (px * 4, px * 4), 0)
        ImageDraw.Draw(mask).ellipse([0, 0, px * 4 - 1, px * 4 - 1], fill=255)
        mask = mask.resize((px, px), Image.Resampling.LANCZOS)
        round_img.putalpha(mask)
        round_img.save(os.path.join(out_dir, "ic_launcher_round.png"), "PNG", optimize=True)

    print("Regenerated all mipmap ic_launcher.png and ic_launcher_round.png densities")


def create_feature_graphic(output_path="feature_graphic_1024x500.png"):
    width, height = 1024, 500
    img = Image.new("RGBA", (width, height), (12, 42, 82, 255))
    draw = ImageDraw.Draw(img)

    # Rich sapphire-ocean background gradient
    for y in range(height):
        f = y / height
        r = int(18 * (1 - f) + 9 * f)
        g = int(66 * (1 - f) + 32 * f)
        b = int(122 * (1 - f) + 64 * f)
        draw.line([(0, y), (width, y)], fill=(r, g, b, 255))

    # Subtle alpha-blended tech grid
    grid = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(grid)
    for x in range(0, width, 40):
        g_draw.line([(x, 0), (x, height)], fill=(0, 229, 255, 22), width=1)
    for y in range(0, height, 40):
        g_draw.line([(0, y), (width, y)], fill=(0, 229, 255, 22), width=1)
    img = Image.alpha_composite(img, grid)

    # Right side 3D Globe visualization
    gcx, gcy = 765, 240
    gradius = 158

    rings = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    rdraw = ImageDraw.Draw(rings)
    for r, col, w in [(224, (255, 42, 85, 170), 3), (196, (255, 171, 0, 200), 3), (174, (0, 229, 255, 210), 2)]:
        rdraw.ellipse([gcx - r, gcy - r, gcx + r, gcy + r], outline=col, width=w)
    img = Image.alpha_composite(img, rings)

    globe = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    gl_draw = ImageDraw.Draw(globe)
    for r in range(gradius, 0, -2):
        f = r / gradius
        cr = int(4 + 10 * (1 - f))
        cg = int(45 + 150 * (1 - f))
        cb = int(95 + 150 * (1 - f))
        gl_draw.ellipse([gcx - r, gcy - r, gcx + r, gcy + r], fill=(cr, cg, cb, 255))
    img = Image.alpha_composite(img, globe)

    mer = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    mdraw = ImageDraw.Draw(mer)
    ggrid = (255, 255, 255, 95)
    mdraw.line([gcx - gradius + 5, gcy, gcx + gradius - 5, gcy], fill=ggrid, width=2)
    mdraw.ellipse([gcx - gradius * 0.9, gcy - gradius * 0.45, gcx + gradius * 0.9, gcy + gradius * 0.45], outline=ggrid, width=2)
    mdraw.ellipse([gcx - gradius * 0.45, gcy - gradius, gcx + gradius * 0.45, gcy + gradius], outline=ggrid, width=2)
    mdraw.line([gcx, gcy - gradius + 4, gcx, gcy + gradius - 4], fill=ggrid, width=2)
    img = Image.alpha_composite(img, mer)

    # Epicenter tremors on globe
    epi_layer = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    edraw = ImageDraw.Draw(epi_layer)
    for qx, qy, qr, qcol in [
        (gcx - 28, gcy - 22, 16, (255, 23, 68)),
        (gcx + 44, gcy + 32, 12, (255, 171, 0)),
        (gcx - 62, gcy + 46, 9, (0, 229, 255)),
    ]:
        edraw.ellipse([qx - qr * 2, qy - qr * 2, qx + qr * 2, qy + qr * 2], outline=qcol + (125,), width=2)
        edraw.ellipse([qx - qr, qy - qr, qx + qr, qy + qr], fill=qcol + (245,))
        edraw.ellipse([qx - 4, qy - 4, qx + 4, qy + 4], fill=(255, 255, 255, 255))
    img = Image.alpha_composite(img, epi_layer)

    # Real-time Seismograph waveform along bottom (clean separation below badges)
    w_pts = [
        (40, 442), (180, 442), (215, 430), (242, 458), (270, 402), (298, 480),
        (326, 396), (354, 464), (384, 418), (414, 450), (446, 438), (485, 442),
        (640, 442), (690, 426), (730, 454), (770, 442), (984, 442)
    ]
    wg = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    ImageDraw.Draw(wg).line(w_pts, fill=(0, 229, 255, 120), width=8, joint="curve")
    img = Image.alpha_composite(img, wg)
    wc = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    ImageDraw.Draw(wc).line(w_pts, fill=(255, 255, 255, 240), width=3, joint="curve")
    img = Image.alpha_composite(img, wc)

    draw = ImageDraw.Draw(img)
    font_title = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 62)
    font_sub = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 23)
    font_badge = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 14)
    font_body = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial.ttf", 18)

    # Live pulse dot before title
    dot_layer = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    ddraw = ImageDraw.Draw(dot_layer)
    ddraw.ellipse([48, 70, 80, 102], fill=(255, 23, 68, 90))
    ddraw.ellipse([54, 76, 74, 96], fill=(255, 23, 68, 255))
    ddraw.ellipse([60, 82, 68, 90], fill=(255, 255, 255, 255))
    img = Image.alpha_composite(img, dot_layer)

    draw = ImageDraw.Draw(img)
    draw.text((90, 56), "QuakeCast", fill=(255, 255, 255, 255), font=font_title)
    draw.text((54, 142), "LIVE SEISMIC MONITOR & 3D GLOBE", fill=(0, 229, 255, 255), font=font_sub)

    bullets = [
        "Real-Time Official USGS Seismic Telemetry",
        "Interactive 3D Canvas Globe & Hypocenter Inspection",
        "Intelligent Proximity Warnings with Wave Arrival Physics",
        "Optimized for Mobile, Tablets & Android TV Screen Saver"
    ]
    for idx, b in enumerate(bullets):
        by = 196 + idx * 32
        draw.ellipse([56, by + 7, 64, by + 15], fill=(0, 229, 255, 255))
        draw.text((76, by), b, fill=(225, 240, 255, 255), font=font_body)

    badges = [
        ("USGS DIRECT FEED", (0, 229, 255)),
        ("3D SHOCKWAVES", (255, 171, 0)),
        ("ANDROID TV READY", (105, 240, 174)),
        ("100% AD-FREE", (255, 82, 82))
    ]
    bx = 54
    for text, col in badges:
        bbox = draw.textbbox((0, 0), text, font=font_badge)
        bw = (bbox[2] - bbox[0]) + 20
        draw.rounded_rectangle([bx, 336, bx + bw, 368], radius=6, fill=(10, 32, 62, 235), outline=col + (220,), width=2)
        draw.text((bx + 10, 344), text, fill=(255, 255, 255, 255), font=font_badge)
        bx += bw + 10

    img.convert("RGB").save(output_path, "PNG", optimize=True)
    print(f"Created feature graphic at {output_path}")


if __name__ == "__main__":
    create_app_icon()
    create_feature_graphic()

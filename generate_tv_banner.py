#!/usr/bin/env python3
import os
from PIL import Image, ImageDraw, ImageFont


def draw_tv_banner(width, height):
    """Render full-bleed opaque 16:9 Android TV banner at 1280x720 with proper alpha compositing, then downsample."""
    rw, rh = 1280, 720
    scale = rw / 640.0

    im = Image.new("RGBA", (rw, rh), (12, 47, 90, 255))
    draw = ImageDraw.Draw(im)

    # 1. Full-bleed rich sapphire-ocean gradient background (clearly distinct from dark Google TV launcher)
    for y in range(rh):
        f = y / rh
        r = int(20 * (1 - f) + 10 * f)
        g = int(76 * (1 - f) + 38 * f)
        b = int(138 * (1 - f) + 74 * f)
        draw.line([(0, y), (rw, y)], fill=(r, g, b, 255))

    # 2. Subtle alpha-blended tech grid
    grid_layer = Image.new("RGBA", (rw, rh), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(grid_layer)
    grid_step = int(32 * scale)
    for x in range(0, rw, grid_step):
        g_draw.line([(x, 0), (x, rh)], fill=(0, 229, 255, 24), width=1)
    for y in range(0, rh, grid_step):
        g_draw.line([(0, y), (rw, y)], fill=(0, 229, 255, 24), width=1)
    im = Image.alpha_composite(im, grid_layer)

    # 3. Luminous radial glow behind globe on the left
    gx = int(142 * scale)
    gy = int(172 * scale)

    glow = Image.new("RGBA", (rw, rh), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    for r in range(int(145 * scale), 0, -4):
        alpha = int(50 * (1 - r / (145 * scale)))
        gdraw.ellipse([gx - r, gy - r, gx + r, gy + r], fill=(0, 229, 255, alpha))
    im = Image.alpha_composite(im, glow)

    # 4. Crisp vector-like artwork overlay with proper RGBA blending
    art = Image.new("RGBA", (rw, rh), (0, 0, 0, 0))
    adraw = ImageDraw.Draw(art)

    # Shockwave concentric rings
    for radius, color, w in [
        (int(96 * scale), (255, 42, 85, 215), int(3.0 * scale)),
        (int(78 * scale), (255, 171, 0, 235), int(3.2 * scale)),
        (int(60 * scale), (0, 229, 255, 245), int(3.2 * scale)),
    ]:
        adraw.ellipse([gx - radius, gy - radius, gx + radius, gy + radius], outline=color, width=max(2, w))

    # 3D Earth Globe base sphere with smooth cyan-blue shading
    earth_r = int(50 * scale)
    for r in range(earth_r, 0, -2):
        f = r / earth_r
        cr = int(0 + 10 * f)
        cg = int(212 * (1 - f * 0.55))
        cb = int(245 * (1 - f * 0.42))
        adraw.ellipse([gx - r, gy - r, gx + r, gy + r], fill=(cr, cg, cb, 255))
    adraw.ellipse(
        [gx - earth_r, gy - earth_r, gx + earth_r, gy + earth_r],
        outline=(0, 229, 255, 255),
        width=max(2, int(2.4 * scale)),
    )
    im = Image.alpha_composite(im, art)

    # Subtle meridians & equator blended cleanly
    mer = Image.new("RGBA", (rw, rh), (0, 0, 0, 0))
    mdraw = ImageDraw.Draw(mer)
    mdraw.ellipse(
        [gx - int(22 * scale), gy - earth_r, gx + int(22 * scale), gy + earth_r],
        outline=(255, 255, 255, 115),
        width=max(1, int(1.6 * scale)),
    )
    mdraw.line([(gx - earth_r, gy), (gx + earth_r, gy)], fill=(255, 255, 255, 105), width=max(1, int(1.5 * scale)))
    mdraw.line([(gx, gy - earth_r), (gx, gy + earth_r)], fill=(255, 255, 255, 95), width=max(1, int(1.4 * scale)))
    im = Image.alpha_composite(im, mer)

    # Epicenter core glow + solid core
    epi = Image.new("RGBA", (rw, rh), (0, 0, 0, 0))
    edraw = ImageDraw.Draw(epi)
    epi_outer = int(15 * scale)
    epi_r = int(9.5 * scale)
    edraw.ellipse([gx - epi_outer, gy - epi_outer, gx + epi_outer, gy + epi_outer], fill=(255, 23, 68, 110))
    edraw.ellipse([gx - epi_r, gy - epi_r, gx + epi_r, gy + epi_r], fill=(255, 23, 68, 255))
    edraw.ellipse([gx - int(4 * scale), gy - int(4 * scale), gx + int(4 * scale), gy + int(4 * scale)], fill=(255, 255, 255, 255))
    im = Image.alpha_composite(im, epi)

    # Seismograph waveform along bottom (blended cyan glow + crisp white core)
    wave_y = int(316 * scale)
    wave_pts = [
        (0, wave_y),
        (int(55 * scale), wave_y),
        (int(82 * scale), wave_y - int(12 * scale)),
        (int(100 * scale), wave_y + int(16 * scale)),
        (int(118 * scale), wave_y - int(26 * scale)),
        (int(134 * scale), wave_y + int(30 * scale)),
        (int(150 * scale), wave_y - int(40 * scale)),
        (int(166 * scale), wave_y + int(34 * scale)),
        (int(184 * scale), wave_y - int(18 * scale)),
        (int(204 * scale), wave_y),
        (rw, wave_y),
    ]
    w_glow = Image.new("RGBA", (rw, rh), (0, 0, 0, 0))
    ImageDraw.Draw(w_glow).line(wave_pts, fill=(0, 229, 255, 145), width=max(2, int(6.5 * scale)), joint="curve")
    im = Image.alpha_composite(im, w_glow)
    w_core = Image.new("RGBA", (rw, rh), (0, 0, 0, 0))
    ImageDraw.Draw(w_core).line(wave_pts, fill=(255, 255, 255, 245), width=max(2, int(2.6 * scale)), joint="curve")
    im = Image.alpha_composite(im, w_core)

    # Typography
    draw = ImageDraw.Draw(im)
    font_title = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", int(64 * scale))
    font_sub = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", int(19 * scale))
    font_pill = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", int(16 * scale))

    rx = int(260 * scale)

    # 1. USGS LIVE Pill Badge
    pill_text = "USGS LIVE"
    bbox_p = draw.textbbox((0, 0), pill_text, font=font_pill)
    dot_pad = int(18 * scale)
    pw = (bbox_p[2] - bbox_p[0]) + int(28 * scale) + dot_pad
    ph = int(30 * scale)
    py = int(80 * scale)

    draw.rounded_rectangle(
        [rx, py, rx + pw, py + ph],
        radius=ph // 2,
        fill=(155, 20, 42, 255),
        outline=(255, 92, 92, 255),
        width=max(2, int(1.8 * scale)),
    )
    # Crisp red/white live dot inside pill
    ld_r = int(4.5 * scale)
    ld_cx = rx + int(16 * scale)
    ld_cy = py + ph // 2
    draw.ellipse([ld_cx - ld_r, ld_cy - ld_r, ld_cx + ld_r, ld_cy + ld_r], fill=(255, 255, 255, 255))
    draw.text((rx + int(28 * scale), py + int(5 * scale)), pill_text, font=font_pill, fill=(255, 255, 255, 255))

    # 2. Main Title: "QuakeCast"
    title_text = "QuakeCast"
    ty = py + ph + int(12 * scale)
    draw.text((rx, ty), title_text, font=font_title, fill=(255, 255, 255, 255))

    bbox_t = draw.textbbox((rx, ty), title_text, font=font_title)
    dot_x = bbox_t[2] + int(8 * scale)
    dot_y = bbox_t[3] - int(16 * scale)
    draw.ellipse([dot_x, dot_y, dot_x + int(11 * scale), dot_y + int(11 * scale)], fill=(0, 229, 255, 255))

    # 3. Subtitle: "REAL-TIME SEISMIC MONITOR"
    sub_text = "REAL-TIME SEISMIC MONITOR"
    draw.text((rx, bbox_t[3] + int(14 * scale)), sub_text, font=font_sub, fill=(195, 225, 255, 255))

    return im.convert("RGB").resize((width, height), Image.Resampling.LANCZOS)


def main():
    densities = [
        ("app/src/main/res/drawable-mdpi/tv_banner.png", 160, 90),
        ("app/src/main/res/drawable-hdpi/tv_banner.png", 240, 135),
        ("app/src/main/res/drawable-xhdpi/tv_banner.png", 320, 180),
        ("app/src/main/res/drawable-xxhdpi/tv_banner.png", 480, 270),
        ("app/src/main/res/drawable-xxxhdpi/tv_banner.png", 640, 360),
    ]
    for path, w, h in densities:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        banner = draw_tv_banner(w, h)
        banner.save(path, "PNG", optimize=True)
        print(f"Created: {path} ({w}x{h})")

    # Store Listing Android TV Banner (1280x720, 24-bit opaque RGB PNG, < 1MB)
    store_banner = draw_tv_banner(1280, 720)
    store_banner.save("tv_banner_1280x720.png", "PNG", optimize=True)
    print("Created Google Play Store TV Banner: tv_banner_1280x720.png (1280x720)")

    for legacy in ["app/src/main/res/drawable/tv_banner.png", "app/src/main/res/drawable/tv_banner.xml"]:
        if os.path.exists(legacy):
            os.remove(legacy)
            print(f"Removed legacy: {legacy}")


if __name__ == "__main__":
    main()

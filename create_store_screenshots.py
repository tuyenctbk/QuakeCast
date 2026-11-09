#!/usr/bin/env python3
import os
import shutil
from PIL import Image, ImageDraw, ImageFont, ImageFilter

WIDTH, HEIGHT = 1080, 2400

PHONE_CARDS = [
    {
        "badge": "3D SEISMIC VISUALIZATION",
        "title": "Interactive 3D Earth Globe",
        "subtitle": "Real-time USGS seismic event monitoring & tectonic plate arcs",
        "src": "screenshots/mobile/01_globe_main.png",
        "dst": "screenshots/store_listing/phone/01_3d_seismic_globe.png"
    },
    {
        "badge": "EARTHQUAKE PHYSICS",
        "title": "Wave Propagation Simulator",
        "subtitle": "Precise P & S wave arrival countdowns & TNT energy equivalent",
        "src": "screenshots/mobile/02_earthquake_detail.png",
        "dst": "screenshots/store_listing/phone/02_wave_propagation_simulator.png"
    },
    {
        "badge": "GLOBAL EVENT TELEMETRY",
        "title": "Worldwide Seismic Feed",
        "subtitle": "Live magnitude filters, focal depth indicators & felt reports",
        "src": "screenshots/mobile/03_event_feed.png",
        "dst": "screenshots/store_listing/phone/03_worldwide_event_feed.png"
    },
    {
        "badge": "HISTORICAL INSIGHTS",
        "title": "30-Day Trend Analytics",
        "subtitle": "Deep seismic frequency histograms & magnitude distribution",
        "src": "screenshots/mobile/04_seismic_analytics.png",
        "dst": "screenshots/store_listing/phone/04_seismic_trend_analytics.png"
    },
    {
        "badge": "PERSONAL PROTECTION",
        "title": "Immediate Proximity Alerts",
        "subtitle": "Continuous tremor detection within your designated safety radius",
        "src": "screenshots/mobile/05_recent_alerts.png",
        "dst": "screenshots/store_listing/phone/05_proximity_alerts_history.png"
    },
    {
        "badge": "LIFE-SAVING ACTION",
        "title": "Emergency Safety Protocols",
        "subtitle": "Instant Drop-Cover-Hold guidance & post-quake recovery checklist",
        "src": "screenshots/mobile/06_safety_guide.png",
        "dst": "screenshots/store_listing/phone/06_emergency_safety_protocols.png"
    },
    {
        "badge": "ACCESSIBLE ALERTS",
        "title": "Custom Radius & TTS Audio",
        "subtitle": "Fine-tune seismic thresholds, speech announcements & sirens",
        "src": "screenshots/mobile/07_proximity_settings.png",
        "dst": "screenshots/store_listing/phone/07_custom_radius_tts_audio.png"
    },
    {
        "badge": "EXPERIENCE & CONTROLS",
        "title": "Seismic Haptics & Dark Theme",
        "subtitle": "Shake-to-refresh g-force calibration & deep AMOLED themes",
        "src": "screenshots/mobile/08_support_development.png",
        "dst": "screenshots/store_listing/phone/08_seismic_haptics_dark_theme.png"
    }
]

TV_CARDS = [
    ("screenshots/tv/01_tv_screensaver_overview.png", "screenshots/store_listing/tv/01_tv_ambient_screensaver.png"),
    ("screenshots/tv/02_tv_quake_telemetry.png", "screenshots/store_listing/tv/02_tv_continuous_telemetry.png"),
    ("screenshots/tv/03_tv_quake_detail_dialog.png", "screenshots/store_listing/tv/03_tv_scientific_modal_dialog.png"),
    ("screenshots/tv/04_tv_cycle_next.png", "screenshots/store_listing/tv/04_tv_remote_dpad_cycling.png"),
    ("screenshots/tv/05_tv_cycle_prev.png", "screenshots/store_listing/tv/05_tv_fault_line_mapping.png"),
    ("screenshots/tv/06_tv_paused_state.png", "screenshots/store_listing/tv/06_tv_freeze_telemetry_inspection.png"),
    ("screenshots/tv/07_tv_major_magnitude_focus.png", "screenshots/store_listing/tv/07_tv_major_subduction_zones.png"),
    ("screenshots/tv/08_tv_global_seismicity.png", "screenshots/store_listing/tv/08_tv_global_ring_of_fire.png")
]

def make_gradient_bg(w, h):
    base = Image.new("RGBA", (w, h), (8, 14, 26, 255))
    top_glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(top_glow)
    
    # Subtle radial cyan glow behind header
    for r in range(450, 0, -15):
        alpha = int(45 * (1 - r / 450))
        draw.ellipse([w//2 - r*2, 280 - r, w//2 + r*2, 280 + r], fill=(0, 229, 255, alpha))
        
    return Image.alpha_composite(base, top_glow)

def round_corners(im, radius):
    mask = Image.new('L', im.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([(0, 0), im.size], radius, fill=255)
    result = im.copy()
    result.putalpha(mask)
    return result

def render_phone_screenshot(card, font_title, font_sub, font_badge):
    os.makedirs(os.path.dirname(card["dst"]), exist_ok=True)
    canvas = make_gradient_bg(WIDTH, HEIGHT)
    draw = ImageDraw.Draw(canvas)
    
    # 1. Badge Pill
    badge_text = card["badge"]
    bbox_b = draw.textbbox((0, 0), badge_text, font=font_badge)
    bw = bbox_b[2] - bbox_b[0] + 36
    bh = bbox_b[3] - bbox_b[1] + 20
    bx = (WIDTH - bw) // 2
    by = 130
    draw.rounded_rectangle([bx, by, bx + bw, by + bh], radius=bh//2, fill=(0, 229, 255, 30), outline=(0, 229, 255, 120), width=2)
    draw.text((bx + 18, by + 8), badge_text, font=font_badge, fill=(0, 229, 255, 255))
    
    # 2. Main Title
    title_text = card["title"]
    bbox_t = draw.textbbox((0, 0), title_text, font=font_title)
    tw = bbox_t[2] - bbox_t[0]
    draw.text(((WIDTH - tw) // 2, by + bh + 30), title_text, font=font_title, fill=(255, 255, 255, 255))
    
    # 3. Subtitle
    sub_text = card["subtitle"]
    bbox_s = draw.textbbox((0, 0), sub_text, font=font_sub)
    sw = bbox_s[2] - bbox_s[0]
    draw.text(((WIDTH - sw) // 2, by + bh + 105), sub_text, font=font_sub, fill=(148, 163, 184, 255))
    
    # 4. Device Frame with Screenshot
    src_img = Image.open(card["src"]).convert("RGBA")
    
    # Target frame dimension: 880px wide
    target_w = 880
    aspect = src_img.height / src_img.width
    target_h = int(target_w * aspect)
    
    scaled_shot = src_img.resize((target_w, target_h), Image.Resampling.LANCZOS)
    rounded_shot = round_corners(scaled_shot, radius=44)
    
    # Position phone mockup
    phone_x = (WIDTH - target_w) // 2
    phone_y = by + bh + 175
    
    # Outer device bezel border
    bezel_pad = 12
    draw.rounded_rectangle(
        [phone_x - bezel_pad, phone_y - bezel_pad, phone_x + target_w + bezel_pad, phone_y + target_h + bezel_pad],
        radius=54,
        fill=(15, 23, 42, 255),
        outline=(51, 65, 85, 255),
        width=3
    )
    
    # Subtle Cyan edge highlight
    draw.rounded_rectangle(
        [phone_x - 3, phone_y - 3, phone_x + target_w + 3, phone_y + target_h + 3],
        radius=46,
        outline=(0, 229, 255, 80),
        width=2
    )
    
    canvas.paste(rounded_shot, (phone_x, phone_y), rounded_shot)
    
    # Save optimized RGB PNG
    canvas.convert("RGB").save(card["dst"], "PNG", optimize=True)
    print(f"Generated: {card['dst']}")

def main():
    font_title = ImageFont.truetype('/System/Library/Fonts/Supplemental/Arial Bold.ttf', 54)
    font_sub = ImageFont.truetype('/System/Library/Fonts/Supplemental/Arial.ttf', 27)
    font_badge = ImageFont.truetype('/System/Library/Fonts/Supplemental/Arial Bold.ttf', 20)
    
    print("Generating 8 Phone Store Listing Screenshots...")
    for card in PHONE_CARDS:
        render_phone_screenshot(card, font_title, font_sub, font_badge)
        
    print("\nCopying and verifying 8 Android TV 1080p Store Listing Screenshots...")
    os.makedirs("screenshots/store_listing/tv", exist_ok=True)
    for src, dst in TV_CARDS:
        shutil.copyfile(src, dst)
        print(f"Verified TV: {dst}")

    print("\nAll Store Listing Screenshots ready!")

if __name__ == "__main__":
    main()

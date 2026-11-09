#!/usr/bin/env python3
import os
import shutil
from PIL import Image, ImageDraw, ImageFont

WIDTH, HEIGHT = 1080, 2400

PHONE_CARDS = [
    {
        "badge": "3D SEISMIC VISUALIZATION",
        "title": "Interactive 3D Earth Globe",
        "subtitle": "Real-time USGS earthquake monitoring & tectonic plate arcs",
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
    im = Image.new("RGB", (w, h), (7, 12, 22))
    draw = ImageDraw.Draw(im)
    
    # Smooth background gradient from top to bottom
    for y in range(h):
        factor = y / h
        r = int(7 * (1 - factor) + 12 * factor)
        g = int(12 * (1 - factor) + 20 * factor)
        b = int(22 * (1 - factor) + 36 * factor)
        draw.line([(0, y), (w, y)], fill=(r, g, b))
        
    return im

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
    bw = (bbox_b[2] - bbox_b[0]) + 44
    bh = 46
    bx = (WIDTH - bw) // 2
    by = 120
    draw.rounded_rectangle(
        [bx, by, bx + bw, by + bh],
        radius=bh // 2,
        fill=(10, 36, 56),
        outline=(0, 229, 255),
        width=2
    )
    # Center text vertically in pill
    tw_b = bbox_b[2] - bbox_b[0]
    th_b = bbox_b[3] - bbox_b[1]
    draw.text(
        (bx + (bw - tw_b) // 2, by + (bh - th_b) // 2 - bbox_b[1]),
        badge_text,
        font=font_badge,
        fill=(0, 229, 255)
    )
    
    # 2. Main Title
    title_text = card["title"]
    bbox_t = draw.textbbox((0, 0), title_text, font=font_title)
    tw = bbox_t[2] - bbox_t[0]
    draw.text(((WIDTH - tw) // 2, by + bh + 24), title_text, font=font_title, fill=(255, 255, 255))
    
    # 3. Subtitle
    sub_text = card["subtitle"]
    bbox_s = draw.textbbox((0, 0), sub_text, font=font_sub)
    sw = bbox_s[2] - bbox_s[0]
    draw.text(((WIDTH - sw) // 2, by + bh + 96), sub_text, font=font_sub, fill=(148, 163, 184))
    
    # 4. Device Mockup Frame
    src_img = Image.open(card["src"])
    
    # Crop status bar (top 105px) and gesture bar (bottom 55px)
    crop_top = 105
    crop_bottom = src_img.height - 55
    cropped_src = src_img.crop((0, crop_top, src_img.width, crop_bottom))
    
    target_w = 880
    aspect = cropped_src.height / cropped_src.width
    target_h = int(target_w * aspect)
    
    scaled_shot = cropped_src.resize((target_w, target_h), Image.Resampling.LANCZOS).convert("RGBA")
    rounded_shot = round_corners(scaled_shot, radius=36)
    
    phone_x = (WIDTH - target_w) // 2
    phone_y = by + bh + 165
    
    # Device Outer Frame / Bezel
    bezel = 14
    draw.rounded_rectangle(
        [phone_x - bezel, phone_y - bezel, phone_x + target_w + bezel, phone_y + target_h + bezel],
        radius=48,
        fill=(15, 23, 42),
        outline=(51, 65, 85),
        width=3
    )
    
    # Cyan accent border
    draw.rounded_rectangle(
        [phone_x - 2, phone_y - 2, phone_x + target_w + 2, phone_y + target_h + 2],
        radius=38,
        outline=(0, 229, 255),
        width=2
    )
    
    # Paste screenshot
    canvas.paste(rounded_shot, (phone_x, phone_y), rounded_shot)
    
    # Save optimized RGB PNG
    canvas.save(card["dst"], "PNG", optimize=True)
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

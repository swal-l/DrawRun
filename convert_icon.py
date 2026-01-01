
try:
    from PIL import Image
except ImportError:
    import os
    print("PIL not found, renaming file instead")
    # Fallback if PIL not installed: just simple copy but with no conversion
    exit(1)

source_path = r"C:\Users\lomic\.gemini\antigravity\brain\78442eca-ac41-4a2b-bb4f-4fc448a307b2\app_icon_fixed_legs_1766349742345.png"
dest_path = r"c:\Users\lomic\Dev\orbital-belt\app\src\main\res\mipmap-xxhdpi\ic_launcher.jpg"

try:
    print(f"Opening {source_path}")
    with Image.open(source_path) as img:
        print(f"Format: {img.format}, Mode: {img.mode}")
        # Convert to RGB to ensure no alpha channel issues for JPG
        rgb_img = img.convert('RGB')
        rgb_img.save(dest_path, "JPEG", quality=95)
        print(f"Saved to {dest_path}")
except Exception as e:
    print(f"Error: {e}")
    exit(1)


import re

file_path = r"c:\Users\lomic\Dev\orbital-belt\TranspiStats\Stats\transpistats.vercel.app\assets\index-D8pGdV4m.js"
output_path = r"c:\Users\lomic\Dev\orbital-belt\transpi_features.txt"

# Keywords likely to appear in UI for statistics
keywords = [
    "Distance", "Elevation", "Time", "Duration", "Calories", 
    "Total", "Year", "Month", "Week", "Best", "Record", 
    "Activity", "Count", "Speed", "Pace", "Heart Rate", 
    "Power", "Watts", "Cadence", "Heatmap", "Map", "Chart",
    "Bilan", "Summary", "Stats", "Analyse", "Comparison",
    "Run", "Ride", "Swim", "Hike", "Walk",
    "Eddington", "Kudo", "Achievement", "Badge"
]

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

with open(output_path, "w", encoding="utf-8") as out:
    out.write(f"File size: {len(content)} characters\n")

    for kw in keywords:
        # Find Matches with surrounding context
        matches = [m.start() for m in re.finditer(re.escape(kw), content, re.IGNORECASE)]
        out.write(f"\n--- Matches for '{kw}' ({len(matches)}) ---\n")
        # Sample first 5
        for i, start in enumerate(matches[:5]):
            s = max(0, start - 50)
            e = min(len(content), start + 100)
            snippet = content[s:e].replace('\n', ' ')
            out.write(f"...{snippet}...\n")

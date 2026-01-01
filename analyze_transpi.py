
import re

file_path = r"c:\Users\lomic\Dev\orbital-belt\TranspiStats\Stats\transpistats.vercel.app\assets\index-D8pGdV4m.js"
output_path = r"c:\Users\lomic\Dev\orbital-belt\analysis_results.txt"

keywords = ["token", "limit", "athlete", "sync", "api", "intervals.icu", "strava", "garmin", "fetch", "axios", "bilan"]

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

with open(output_path, "w", encoding="utf-8") as out:
    out.write(f"File size: {len(content)} characters\n")

    for kw in keywords:
        out.write(f"\n--- Matches for '{kw}' ---\n")
        matches = [m.start() for m in re.finditer(re.escape(kw), content, re.IGNORECASE)]
        out.write(f"Found {len(matches)} matches\n")
        for i, start in enumerate(matches[:20]): # Limit to first 20
            s = max(0, start - 150)
            e = min(len(content), start + 150)
            snippet = content[s:e].replace('\n', ' ')
            out.write(f"Match {i+1}: ...{snippet}...\n")

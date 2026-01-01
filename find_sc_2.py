
import re

file_path = r"c:\Users\lomic\Dev\orbital-belt\TranspiStats\Stats\transpistats.vercel.app\assets\index-D8pGdV4m.js"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Look for Sc = something
matches = re.findall(r'Sc\s*=\s*([^;,}]+)', content)
print(f"Sc matches: {matches[:5]}") # Show first 5

# Look for any http link
links = re.findall(r'https?://[^\s"\')]+', content)
print(f"Links found: {links[:10]}")

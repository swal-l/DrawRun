
import re

file_path = r"c:\Users\lomic\Dev\orbital-belt\TranspiStats\Stats\transpistats.vercel.app\assets\index-D8pGdV4m.js"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Look for Sc = assignment or const Sc = 
# Since minified, it might be Sc="https://..."
matches = re.findall(r'Sc\s*=\s*["\']([^"\']+)["\']', content)
print(f"Sc matches: {matches}")

# Also look for client_id
client_ids = re.findall(r'client_id["\']?\s*[:=]\s*["\'](\d+)["\']', content)
print(f"Client IDs: {client_ids}")

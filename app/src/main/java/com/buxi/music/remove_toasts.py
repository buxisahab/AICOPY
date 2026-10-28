import os

directories_to_scan = [
    r"d:\Wondershare\Download\Ai-master\app\src\main\java\com\buxi\music\ui\player",
    r"d:\Wondershare\Download\Ai-master\app\src\main\java\com\buxi\music\ui\screens",
    r"d:\Wondershare\Download\Ai-master\app\src\main\java\com\buxi\music\playback",
    r"d:\Wondershare\Download\Ai-master\app\src\main\java\com\buxi\music\utils",
    r"d:\Wondershare\Download\Ai-master\app\src\main\java\com\buxi\music"
]

for directory in directories_to_scan:
    if not os.path.exists(directory):
        continue
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(".kt"):
                path = os.path.join(root, file)
                try:
                    with open(path, 'r', encoding='utf-8') as f:
                        content = f.read()
                    
                    if "Toast.makeText" in content or "android.widget.Toast.makeText" in content:
                        lines = content.split('\n')
                        new_lines = []
                        i = 0
                        while i < len(lines):
                            line = lines[i]
                            if "Toast.makeText" in line:
                                new_lines.append("// " + line)
                                # Check if it spans multiple lines until .show() or )
                                if ".show()" not in line and ")" not in line:
                                    j = i + 1
                                    while j < len(lines) and j < i + 5:
                                        new_lines.append("// " + lines[j])
                                        if ".show()" in lines[j] or ")" in lines[j]:
                                            i = j
                                            break
                                        j += 1
                            else:
                                new_lines.append(line)
                            i += 1
                        
                        with open(path, 'w', encoding='utf-8') as f:
                            f.write('\n'.join(new_lines))
                        print(f"Commented toasts in {path}")
                except Exception as e:
                    print(f"Error processing {path}: {e}")

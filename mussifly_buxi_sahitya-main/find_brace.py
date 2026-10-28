import re

file_path = r"d:\all_test_tune\mussifly_3.1_w\OpenTune-master\app\src\main\java\com\musifly\android\playback\MusicService.kt"

with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

count = 0
line_num = 0

print("Line | Balance | Content")
print("-" * 80)

for i, line in enumerate(lines, 1):
    open_count = line.count('{')
    close_count = line.count('}')
    prev_count = count
    count += open_count - close_count
    
    # Print lines where balance changes significantly or becomes zero
    if open_count > 0 or close_count > 0 or (prev_count != 0 and count == 0):
        content = line.strip()[:60]  # First 60 chars
        print(f"{i:4} | {count:+3} -> {prev_count:+3} | {content}")
        
        # Mark potential problem areas
        if count < 0:
            print(f"     *** ERROR: Too many closing braces at line {i}")
            break

print(f"\nFinal balance: {count}")
print(f"Expected: 0")
print(f"Difference: {count}")

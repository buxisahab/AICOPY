import xml.etree.ElementTree as ET
import re
import os

input_file = r'app/src/main/res/drawable/sound-effect.svg'
output_file = r'app/src/main/res/drawable/sound_effect.xml'

print(f"Reading from: {os.path.abspath(input_file)}")

try:
    tree = ET.parse(input_file)
    root = tree.getroot()
    
    view_width = root.get('width', '512').replace('px', '')
    view_height = root.get('height', '512').replace('px', '')
    
    xml_content = f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="{view_width}"
    android:viewportHeight="{view_height}">
'''
    
    # Iterate through all elements, looking for paths
    for path in root.iter():
        if path.tag.endswith('path'):
            d = path.get('d')
            if not d: continue
            
            transform = path.get('transform')
            
            # Use White as base color
            fill_color = "#FFFFFFFF"
            
            if transform:
                # Parse translate(x,y) or translate(x y)
                match = re.search(r'translate\(([-\d.]+)[, ]+([-\d.]+)\)', transform)
                if match:
                    tx, ty = match.groups()
                    xml_content += f'''    <group android:translateX="{tx}" android:translateY="{ty}">
        <path
            android:fillColor="{fill_color}"
            android:pathData="{d}" />
    </group>
'''
                else:
                     xml_content += f'''    <path
        android:fillColor="{fill_color}"
        android:pathData="{d}" />
'''
            else:
                 xml_content += f'''    <path
        android:fillColor="{fill_color}"
        android:pathData="{d}" />
'''

    xml_content += '</vector>'
    
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(xml_content)
        
    print(f"Successfully converted to {output_file}")

except Exception as e:
    print(f"Error: {e}")

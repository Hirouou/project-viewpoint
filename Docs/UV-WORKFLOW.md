# UV contribution workflow

Search for an item by model name in UV-GALLERY.html or UV-INDEX.csv. Open models/<name>.obj in Blender or use the library file in the same folder. The matching MTL links to the paint PNG.

Paint textures/PNG/<name>_UV.png in Aseprite without resizing or renaming it. The matching UV guide shows connected surfaces and chart numbers. To see saved PNG edits in Blender, open models/ProjectViewPoint-UV-Library-Surfaces.blend and run Scripts/Reload-UV-PNGs.py once from the Blender Text Editor. Images refresh within two seconds.

Submit the changed PNG and a preview identified by model name. The maintainer reviews it before mod integration. Each map starts with that model's existing mod texture remapped to the organized UVs; the source project texture sheets are not included. The two radios reserved for rework are excluded.

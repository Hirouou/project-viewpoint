import bpy
from pathlib import Path

# Execute once in Blender's Text Editor to reload only PNGs whose disk file changed.
UV_PNG_DIR = Path(bpy.path.abspath('//../textures/PNG')).resolve()
_uv_png_mtimes = {}

def _watch_uv_pngs():
    for image in bpy.data.images:
        try:
            path = Path(bpy.path.abspath(image.filepath)).resolve()
            if not str(path).casefold().startswith(str(UV_PNG_DIR).casefold()):
                continue
            stat = path.stat()
            stamp = (stat.st_mtime_ns, stat.st_size)
            key = str(path).casefold()
            previous = _uv_png_mtimes.get(key)
            if previous is not None and stamp != previous:
                image.reload()
            _uv_png_mtimes[key] = stamp
        except (OSError, RuntimeError):
            continue
    return 2.0

if not bpy.app.timers.is_registered(_watch_uv_pngs):
    bpy.app.timers.register(_watch_uv_pngs, first_interval=2.0)
print('Atualização automática de PNG UV ativa; verificação a cada 2 segundos.')

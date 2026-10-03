# Interiors 0.1.18

Original runtime source and the verified model pack for Project ViewPoint.

- `source/local/vpinteriors/`: Java hooks and original serialized roof resources.
- `mod/common/mod.info`: version and dependencies.
- `mod/42/media/modelpacks/furniturefix/pack.properties`: authoritative runtime model/sprite associations.
- `mod/42/media/lua/client/ViewpointFurnitureFix.lua`: module registration.

Build with `Scripts/build_project.py` and locally installed Project Zomboid, Viewpoint and ZombieBuddy dependencies. The repository does not include their Java binaries. The builder packages the roof resources along with the original classes. Native Aim in the Steam download remains 0.4.11.

See `Docs/Build-0.1.18-PT-BR.md` for changes and known remaining issues. Offline checks passed; gameplay/FPS confirmation remains separate.

# 3D Models — Interiors 0.1.18

Original runtime source and the verified model pack for Project ViewPoint.

The [existing Steam Workshop item](https://steamcommunity.com/sharedfiles/filedetails/?id=3811400314) now distributes the **3D Models** pack, with the same mod ID `ViewpointFurnitureFix`. [Native Aim](https://steamcommunity.com/sharedfiles/filedetails/?id=3813269725) is a separate optional download with mod ID `ViewpointADSTest`. Either pack can be enabled without the other; both still require Viewpoint and ZombieBuddy. See [the migration guide](../../Docs/MOD-SPLIT.md).

- `source/local/vpinteriors/`: Java hooks and original serialized roof resources.
- `mod/common/mod.info`: version and dependencies.
- `mod/42/media/modelpacks/furniturefix/pack.properties`: authoritative runtime model/sprite associations.
- `mod/42/media/lua/client/ViewpointFurnitureFix.lua`: module registration.

Build with `Scripts/build_project.py` and locally installed Project Zomboid, Viewpoint and ZombieBuddy dependencies. The repository does not include their Java binaries. The builder packages the roof resources along with the original classes. Native Aim remains 0.4.11 in its own Steam download.

See `Docs/Build-0.1.18-PT-BR.md` for changes and known remaining issues. Offline checks passed; gameplay/FPS confirmation remains separate.

Many models are already in the game but still need finished textures; some models have visual bugs. Model/material work and rendering fixes involving Viewpoint remain in progress. The Workshop split changes how the packs are distributed and does not add the unfinished Muldraugh review or local window/HVAC experiments to this release.

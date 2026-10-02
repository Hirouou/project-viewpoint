# Asset Origin and License Manifest

This manifest records creator, source, dependencies, rights, and review status for each asset selected for the public repository. Repository visibility is not permission to use an asset.

## Owner confirmation — 2026-10-02

Hiroki expressly confirmed that all 603 models and their PNG maps in this collaboration package were created from scratch with Codex as his original project work, and authorized publishing them for people to improve the Project ViewPoint mod. This provenance record is based on that owner declaration. The permission is limited to collaboration on the mod; reuse in other games, mods, products, or projects is not granted. The public package is governed by `LICENSE.md`.

## Audit snapshot — 2026-10-02

The audited `Model-Texture-Handoff` collection contains 47,809 files: 78 `.blend` files under geometry batches, 9,809 `.obj`, 26,644 `.png`, and one `.fbx`. These totals include backups, repeated revisions, source/reference material, validation outputs, and exported packs. The contribution package in this checkout is curated separately: it contains 603 project models with UVs organized by their surfaces, model-specific texture maps remapped from the existing mod textures, and matching visual UV guides. Full source sheets are not included.

The `0.1.10` performance-test build contains 544 runtime `.obj` files, 507 `.png` files, and 507 `.mtl` files under its `42` tree, plus the poster PNG under `common`; it has no `.blend` sources. Its README says the build is unpublished and still needs in-game performance testing. Runtime exports are not editable master sources and are excluded from this repository.

## Model handoff inventory

The handoff README describes `geometry/` as model batches, with editable review files and manifests. The following batches are marked frozen in `geometry/STATUS.json`; frozen means handed off for review, not approved for public redistribution or tested in a live game:

| Batch/revision | Snapshot in source status | Publication state |
| --- | --- | --- |
| G001, G002, G004, G005 | Frozen geometry handoffs | Candidate revisions and baseline/candidate packs need file-level curation |
| G006-r1 | 10 models, 37 sprite bindings | Offline validation only; integration not marked ready |
| G007-r6 | 7 window-state models | Offline validation only; game state/render behavior remains untested |
| G008-r2 | 25 models, 26 sprite bindings | Offline validation only; integration not marked ready |
| G009-r3 | 48 models, 58 sprite bindings | Offline validation only; texture work and in-game review remain separate |
| G010 | 153 model parts | Offline geometry handoff; game binding/review remains pending |
| G011-r2 | 36 counter modules, 144 bindings | Offline validation only; integration not marked ready |
| G012 | 345 audited models | Candidate patch selected by hashes; not a cleared bulk export |
| G013 | README describes 43 model parts for review | Not listed as a frozen batch in the status file |
| G014 | Manifest lists 10 models; 8 source OBJ/MTL pairs and a Blender review scene are present | Review handoff; several sprite bindings/atlas checks remain open, and two guitar variants are explicitly deferred |

The 78 Blender files include older review scenes and backups; they are not 78 distinct final models. Select only current, author-confirmed sources after checking embedded data and dependencies. The one FBX is a vanilla vehicle reference, not a publishable model source.

## Asset register

This checkout contains a maintainer-authorized UV collaboration package. Its 603 OBJ models come from the current local 0.1.11 mod pack and the G013/G014 project handoffs. The two models `pz_house_makeshift_ham` and `pz_house_makeshift_radio` are intentionally excluded for rework. Each model's hash, source group, paired MTL, model-specific texture map, guide, and face/chart counts are recorded in [`Docs/UV-MANIFEST.json`](Docs/UV-MANIFEST.json). Each paint PNG uses the model's existing mod texture remapped to the new UV layout; full source sheets, screenshots, and Aseprite projects are not included.

| Repository path | Creator | Source / dependencies | SHA-256 | Rights and permission | Status |
| --- | --- | --- | --- | --- | --- |
| `models/*.obj` (603 files); `Materials/*.mtl` (603 files); `textures/PNG/*_UV.png` (603 files); `textures/UV-Guides/*_UV-Guia.png` (603 files); `models/ProjectViewPoint-UV-Library-Surfaces.blend` | Hiroki, original work confirmed by the owner on 2026-10-02 | Current local 0.1.11 project model pack plus G013/G014 project sources; original project PNG maps remapped to the UV layout; full source sheets excluded | Per-model file SHA-256 in `Docs/UV-MANIFEST.json` | All Rights Reserved; limited permission to review, fork and propose improvements for Project ViewPoint under `LICENSE.md` | Owner cleared for public contribution review; not yet integrated into the mod |
| `Scripts/Reload-UV-PNGs.py` | Hiroki project tooling created with Codex | Reloads the project PNGs in the editable Blender scene | Versioned in the repository commit | Same project-only permission under `LICENSE.md` | Included supporting workflow |
| `Scripts/NativeAim/**` | Hiroki project code and numeric calibration work | Native Aim 0.4.9 source snapshot; Java/Lua sources and calibration data only. External game/framework classes are dependencies, not included. Retired migration identifiers containing a personal name were removed from the public Lua copy. | Per-file SHA-256 in `Docs/SOURCE-MANIFEST.json` | Same project-only permission under `LICENSE.md` | Public development snapshot; not an installer |
| `Scripts/Interiors/**` | Hiroki project code | Interiors 0.1.13 Java/Lua source snapshot and text-only model bindings. Runtime game/framework binaries and extracted source images are excluded. | Per-file SHA-256 in `Docs/SOURCE-MANIFEST.json` | Same project-only permission under `LICENSE.md` | Public development snapshot; model integration requires review |
| `Scripts/build_project.py` | Hiroki project tooling created with Codex | Portable build entry point; dependency paths are supplied by each developer | Per-file SHA-256 in `Docs/SOURCE-MANIFEST.json` | Same project-only permission under `LICENSE.md` | No automatic installation or bundled dependencies |

Only files explicitly identified in this register as original work owned by Hiroki are covered by the All Rights Reserved notice in [`LICENSE.md`](LICENSE.md). A contributor's own file requires a separate, explicit project-only permission recorded here.

## Explicit exclusions

These original paths remain excluded from the public model repository because they contain vanilla-game references/extractions, native image data, or uncurated runtime exports. Do not copy them wholesale. The registered per-model PNG maps and UV guides under this repository's `textures/` folder are included based on Hiroki's explicit confirmation that this package is his original work:

| Excluded source path (relative to its named workspace) | Why it is excluded |
| --- | --- |
| `Model-Texture-Handoff/geometry/G001-kitchen/baseline-pack/**` and `candidate-pack/**` | Each pack contains 310 OBJ files and 177 PNG files among 763 files; the README identifies these as native-source baselines/candidates. |
| `Model-Texture-Handoff/geometry/G004-upholstery/baseline-pack/**` and `candidate-pack/**` | Repeated native baseline/candidate export packs; not an author-only source set. |
| `Model-Texture-Handoff/geometry/G005-counter-family/baseline-pack/**` and `candidate-pack/**` | Repeated native baseline/candidate export packs; not an author-only source set. |
| `Model-Texture-Handoff/geometry/G*/source/native_sprites/**` | Extracted native sprite references; the game files themselves are not redistributed here. |
| `Model-Texture-Handoff/geometry/G*/source/derived-native/**` | Derivatives/crops of native game imagery; excluded pending rights review and not needed for model-only publication. |
| `Model-Texture-Handoff/geometry/G013-lowpoly-house-essentials/backups/overinclusive-native-extraction/**` | Explicitly overinclusive native-image extraction backup. |
| `Model-Texture-Handoff/geometry/G003-vehicle-investigation/source/vehicle_reference/Vehicles_CarNormal.fbx` | Vanilla vehicle model used as a reference; not an original mod asset. |
| `ProjectViewPoint3DInteriors-0.1.10-performance-test/ViewpointFurnitureFix/42/**` | Full runtime build tree, including hundreds of exported models/textures and compiled/runtime files; not a source-only, rights-cleared model set. |
| `Model-Texture-Handoff/textures/**` source tree, including all `.aseprite`/Aseprite/Zprite project files and source sheets | The full authoring tree is excluded. Only the registered per-model PNG maps and UV guides are copied into the collaboration package. |
| All other `geometry/**/baseline-pack/**` or `candidate-pack/**` files not individually registered | Their ownership and embedded/native dependencies have not been cleared for public redistribution. |

Vanilla visual references may be documented by game-relative path/name and a short text description. Do not put full extracted models, source texture sheets, screenshots, archives, or unrelated samples in this repository. The registered, model-specific paint maps are the curated exception for UV editing.

## Approval

Hiroki reviews each proposed asset and decides whether to include it in the mod. A repository merge does not guarantee mod integration or a Steam Workshop update. Assets with unclear origin, embedded vanilla content, third-party dependencies, or missing project-only permission remain excluded.

SPRINT 2 STATUS: INCOMPLETE

# Phase 4 Completion Sprint 2 Report

Date: 2026-07-13
Application ID: com.indianservers.ai_stem
Repository: C:\Indian Servers\AR-STEM

## 1. Executive Summary

Sprint 2 is incomplete.

This sprint made real production progress in three areas:

- Screen-mode graph interaction now has a real overlap chooser bottom sheet and selected-curve feedback.
- Room persistence now exists with `AiStemDatabase`, schema export, project entities, repositories, legacy scene-file migration, and a reachable "My Mathematics Projects" screen.
- Graph sampling now feeds a renderer-ready primitive model consumed by Screen Mode.

The sprint cannot be called complete because the required production AR graph renderer, AR graph placement, AR picking, transform gizmo, numeric transform editor, projected label engine, Instant Placement, Depth Occlusion, Storage Access Framework import/export UI, and physical AR validation remain incomplete or missing.

Phase 5 was not implemented.

## 2. Previous Status

Before this sprint, the project had partial foundations for graph sampling, screen graph picking, snapping calculations, coalesced history, AR capability checks, and file-based scene persistence. Room persistence, production overlap UI, `.aistemmath` validation, and renderer-ready graph primitives were absent.

## 3. Interaction Implementation

Implemented:

- Screen graph picking now opens a real Compose bottom sheet titled `Choose an Object` when multiple graph curves overlap.
- Rows show icon, object name, object type, visible state, locked/editable state, and hit distance.
- Actions implemented: `Select`, `Select Next`, and `Cancel`.
- Repeated taps with the same candidate list cycle deterministically.
- Selected graph curves receive a visible emphasis ring in Screen Mode.

Still incomplete:

- AR object picking.
- Production transform gizmo.
- Numeric transform editor.
- Snapping integration into transforms.
- Coalesced history integration into rendered interactions.

## 4. Picking Evidence

Implemented files:

- `domain/interaction/PickingEngine.kt`
- `feature/graphing/GraphingViewModel.kt`
- `feature/graphing/GraphingStudioScreen.kt`

Automated coverage:

- Existing `RemediationInteractionTest.kt` covers nearest hit, hidden object filtering, overlap ordering/cycling, snapping, and coalesced history foundation.

Manual/runtime evidence:

- Screen canvas uses `GraphRenderPrimitive.Polyline` geometry for hit testing.
- AR picking remains unvalidated and incomplete.

## 5. Transform Gizmo Evidence

Not implemented in this sprint.

## 6. Snapping Evidence

Existing snapping domain logic remains in `domain/interaction/SnappingEngine.kt`.

No production transform or AR placement integration was completed in this sprint.

## 7. Label Engine

Not implemented in this sprint.

## 8. Room Schema

Implemented:

- `AiStemDatabase`
- Room version: `androidx.room:room-*:2.8.4`
- KSP enabled.
- Room schema export enabled.
- Exported schema: `C:\Indian Servers\AR-STEM\app\schemas\com.indianservers.ai_stem.data.project.AiStemDatabase\1.json`

Entities:

- Project
- Scene
- SceneObject
- GraphProject
- GraphExpression
- GraphSlider
- DataTable
- GeometryObject
- ConstructionDependency
- SceneGroup
- Annotation
- Measurement
- Appearance
- Viewport
- ArPlacement
- ProjectThumbnail
- DeletedProject
- SchemaMetadata

The schema uses foreign keys, cascading rules, and indices for project relationships.

## 9. Legacy Migrations

Implemented:

- `LegacySceneMigration` detects existing `math-scenes/*.scene` files.
- It parses legacy schema, name, ID, and object count safely.
- Valid scenes are inserted transactionally into Room.
- Corrupt individual files are skipped without aborting the whole migration.
- Migration completion is recorded in `schema_metadata`.
- Original legacy files are left in place.

Limitations:

- It preserves project names and IDs where available, but only migrates legacy objects as conservative legacy object records. Full transform and parameter reconstruction from legacy lines is not yet complete.

## 10. Project Management

Implemented production screen:

- `My Mathematics Projects`

Supported:

- New Project
- Search
- Sort by recent/name
- Rename
- Save As
- Duplicate
- Favourite
- Delete
- Restore Deleted
- Recent Projects
- Legacy migration status

Limitations:

- Project thumbnails are schema-ready but not generated.
- Auto Save from graph/AR editing is not wired.
- Open/save integration with every editor is not complete.

## 11. Import/Export

Implemented:

- `.aistemmath` ZIP container codec.
- Required entries supported: `manifest.json`, `project.json`, `graph.json`, `scene.json`, `geometry.json`, `tables/*.csv`, `thumbnail.png`.
- Validation covers size, entry count, ZIP path traversal, schema version, JSON depth, and future-version rejection.
- JVM tests cover round trip and ZIP traversal rejection.

Not implemented:

- Android Storage Access Framework UI.
- Transactional Room insertion from imported archives.
- PNG export UI.
- Transparent PNG export.
- CSV import/export UI.

## 12. Graph Render Architecture

Implemented:

- `GraphRenderPrimitive`
- `Polyline`
- `PointSet`
- `TriangleMesh`
- `FilledRegion`
- `ArrowSet`
- `TextAnchor`
- Stable IDs, source expression IDs, bounds, material keys, picking metadata, vertices, indices where applicable, and update versions.
- `GraphRenderPrimitiveBuilder`
- Screen Mode now consumes `GraphRenderPrimitive.Polyline` for rendering and picking.

## 13. AR Graph Renderer

Not implemented.

SceneView and Filament dependencies remain present, but there is no production Filament graph renderer, material cache, geometry cache, GPU-buffer lifecycle management, or AR graph mesh upload.

## 14. ARCore Placement

Not implemented.

No table, floor, wall, free-standing, room-scale, reticle, anchor, or graph-root placement workflow was completed.

## 15. Instant Placement

Not implemented.

## 16. Depth Occlusion

Not implemented.

## 17. Tests

Passed:

- `.\gradlew.bat clean assembleDebug assembleRelease testDebugUnitTest lintDebug`
- `.\gradlew.bat app:dependencies --configuration debugRuntimeClasspath`
- `.\gradlew.bat app:dependencies --configuration releaseRuntimeClasspath`

New JVM tests:

- `GraphRenderPrimitiveTest`
- `ProjectArchiveCodecTest`

New instrumented tests:

- `ProjectRoomInstrumentedTest`

Instrumented tests compiled into `app-debug-androidTest.apk`, but did not execute because the ADB target disconnected during installation.

## 18. Physical-Device Status

Physical AR validation is blocked.

`connectedDebugAndroidTest` failed after ADB reported:

`device 'adb-Q8G6ZLF6NR69Q85D-vvvTJj._adb-tls-connect._tcp' not found`

Zero tests ran on the device target. AR runtime behavior, graph placement, depth, Instant Placement, AR picking, transform gizmo, lifecycle, and performance remain unvalidated.

## 19. Build Results

| Command | Result |
| --- | --- |
| `.\gradlew.bat clean assembleDebug assembleRelease testDebugUnitTest lintDebug` | Passed |
| `.\gradlew.bat connectedDebugAndroidTest` | Failed: ADB device disconnected during APK install |
| `.\gradlew.bat app:dependencies --configuration debugRuntimeClasspath` | Passed |
| `.\gradlew.bat app:dependencies --configuration releaseRuntimeClasspath` | Passed |

## 20. APK Paths

Debug APK:

`C:\Indian Servers\AR-STEM\app\build\outputs\apk\debug\app-debug.apk`

Size: 100,374,377 bytes

Unsigned release APK:

`C:\Indian Servers\AR-STEM\app\build\outputs\apk\release\app-release-unsigned.apk`

Size: 82,501,766 bytes

Android test APK:

`C:\Indian Servers\AR-STEM\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk`

Size: 2,678,549 bytes

## 21. Screenshots

No verified screenshots were captured in this sprint.

## 22. Remaining Gaps

Remaining mandatory Sprint 2 gaps: 26.

- AR object picking.
- Transform gizmo.
- Numeric transform editor.
- Snapping integration.
- Coalesced interaction history integration.
- World-to-screen projected label engine.
- Full legacy transform/parameter migration.
- Editor-level Auto Save.
- Open/save integration for graph and AR editors.
- SAF import UI.
- SAF export UI.
- PNG and transparent PNG export.
- CSV import/export UI.
- Transactional archive import into Room.
- Filament graph renderer.
- Material cache.
- Geometry cache.
- Dynamic GPU-buffer replacement.
- AR graph placement.
- Vertical placement.
- Instant Placement.
- Depth occlusion.
- AR graph controls.
- AR graph trace.
- Instrumented test execution on a stable target.
- Physical AR device validation.

## 23. Changed-File Inventory

Added:

- `app/src/main/java/com/indianservers/ai_stem/data/project/AiStemDatabase.kt`
- `app/src/main/java/com/indianservers/ai_stem/data/project/ProjectEntities.kt`
- `app/src/main/java/com/indianservers/ai_stem/data/project/ProjectDao.kt`
- `app/src/main/java/com/indianservers/ai_stem/data/project/ProjectRepositories.kt`
- `app/src/main/java/com/indianservers/ai_stem/data/project/ProjectArchive.kt`
- `app/src/main/java/com/indianservers/ai_stem/domain/graph/GraphRenderPrimitive.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/projects/ProjectManagerScreen.kt`
- `app/src/test/java/com/indianservers/ai_stem/GraphRenderPrimitiveTest.kt`
- `app/src/test/java/com/indianservers/ai_stem/ProjectArchiveCodecTest.kt`
- `app/src/androidTest/java/com/indianservers/ai_stem/ProjectRoomInstrumentedTest.kt`
- `app/schemas/com.indianservers.ai_stem.data.project.AiStemDatabase/1.json`
- `PHASE_4_COMPLETION_SPRINT_2_REPORT.md`

Modified:

- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/java/com/indianservers/ai_stem/app/AiStemApp.kt`
- `app/src/main/java/com/indianservers/ai_stem/app/AppDestination.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/mathematics/MathematicsHomeScreen.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/graphing/GraphingViewModel.kt`
- `app/src/main/java/com/indianservers/ai_stem/feature/graphing/GraphingStudioScreen.kt`

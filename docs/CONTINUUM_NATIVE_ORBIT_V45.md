# Continuum native honeycomb orbit — Fold6 v45

## Authority and scope
- Existing Pocket Agent package: industries.leeway.pocket
- Base: repair/floating-agent-interaction (v44); update-in-place, no second APK or Brain.
- Source counterpart: LeeWay Runtime Fabric PR #19 merge 72d6036987f4412ff683f5362ad7e76367482444.
- Only the existing Continuum UI controller, stylesheet, resource hash lock, scoped version and release test are changed.

## Interface contract
Selecting an existing category or source-backed directory centers the selected honeycomb and arranges the returned child honeycombs around it. Folder and media icons remain inside their honeycombs. Original content is rendered within the existing hexagonal verified-byte viewer. No new card, database, or mock directories are introduced. Existing controls and same-origin owner-bound read contract are retained.

## Qualification scope
- Node source/resource integrity assertions: six local PASS.
- Gradle :app:assembleDebug and :app:testDebugUnitTest: local PASS, debug APK SHA-256 2070034A9F1DAE61D708B03C561C4F3B900239EB9EB337A6F3BA8CC825FC4221.
- Live PC source-backed browser smoke and responsive viewport tests were performed separately on the mirrored Runtime Fabric sources.
- Device installation, APK readback and physical WebView acceptance require separate execution evidence. No golden-release or Formula result is claimed here.
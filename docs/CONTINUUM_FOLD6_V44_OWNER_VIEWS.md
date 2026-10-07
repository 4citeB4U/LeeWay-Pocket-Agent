# Fold6 Continuum v44 — owner-bound Knowledge and Devices

## Scope
This release extends the existing `AndroidContinuumReadAdapter`. It does not create another Brain, database, gateway, model runner, or authority.

The existing Continuum UI assets remain pinned in `app/src/main/assets/agent-vt/SOURCE.json`, and the Android WebView continues to load them from the installed APK through the same origin and hash checks.

## Views
- `experience`: existing owner-bound Continuum events, unchanged.
- `knowledge`: surviving records from the existing Digital Brain `nodes` table, excluding the device-hardware category.
- `devices`: surviving `hardware-component` and `device-current:` node records from the same owner-bound Brain.

The adapter opens **both** databases with `SQLiteDatabase.OPEN_READONLY`, verifies the same phone owner identity and revisioned resource bindings, and returns bounded source-ID pages. Cross-device and cross-body record IDs are rejected.

These are *retained record representations*, not evidence that original referenced files are present on the phone. `storageKind=INDEX_REFERENCE` and `originalFileVerified=false` remain explicit. No synthetic hierarchy or downloadable original is implied.

## Source and physical qualification
- Owner PC built v44 from existing v43 local source with Gradle 8.10.2 and Java 17.
- APK package: `industries.leeway.pocket`; version code 44, `1.0.0-continuum-owner-views-rc17`.
- Installed owner-preserving on Fold6 through authenticated USB ADB; package manager read-back showed version code 44.
- The current Android package is not dependent on permanent USB. Wireless independence remains a separate connection gate.
- Continuum's 3D curved UI renders on Fold6 and the owner-local Brain storage exists. Actual counts for the newly connected views require app-level runtime verification; do not infer that empty categories have data.

## Limits
No changes to Brain schemas or their existing stored records. No Formula admission, general Veritas, Ledger write, cross-body replication, automatic data relocation or production-model grant.

# Brain ingestion continuation — scoped software qualification

<!-- REGION: LEEWAY.BRAIN.QUALIFICATION; TAG: INGESTION_SCOPE_AND_REUSE
WHO: Creator-authorized Agent Lee; WHAT: Record executed work and precise limitations.
WHEN: After portable identity repair; WHERE: existing Pocket candidate and Brain module.
WHY: Software tests cannot become physical-device or customer-release evidence.
HOW: Source manifests, actual host test output, APK inspection and release rejection.
LICENSE: MIT -->

## Executed

The existing shared Brain now reconciles authorized file metadata through its existing ownership and resource-binding interfaces. The Android adapter commits into the existing nodes, provenance, sync_events and node_tombstones tables. This is an extension of the existing Brain, not another persistent index, event bus, registry or consciousness.

Implemented and compiled behavior: create, modify, supplied-stable-identity rename/move, restore, idempotent repeat census and absence-based tombstones after complete authorized enumeration. Missing permissions, stale handles, wrong body ownership, unknown native root identity and incomplete enumeration do not authorize absence-based deletions.

The app-private native sensor reads metadata only. It does not copy file contents, produce a content hash, execute Formula placement or claim extraction completed. Native path-derived object keys do not infer rename identity from matching names or content: they produce create/deletion observations; providers with actual stable source identities can use the shared rename path.

The existing owner-enabled overlay service starts and stops the native FileObserver/reconciliation sensor. A startup scan is also connected. The 60-second reconciliation interval is an implementation cadence while that service runs, not proof of a 180-second source-to-durable-commit guarantee, Android uptime or unattended background operation.

## Tests and correction

Final qualification: 19 new shared reconciliation tests, 18 prior Brain tests, 15 native-scanner tests, 34 prior Android unit regressions and 29 Node gate/boundary tests: 115 total, no failed or skipped tests. Host-native scanner tests create, modify, rename and delete real temporary files and inspect real directory-link escapes. Core store tests and root-identity fault injections are explicitly synthetic unit fixtures, not device acceptance.

The first run had four failing scanner assertions. On this Windows/JDK host, BasicFileAttributes.fileKey returned null. Production was not weakened to mark that complete: unknown identity still prevents absence-based deletion. The Android adapter supplies its native lstat device/inode identity; that adapter compiled but has not run on the phone. The native tests now distinguish metadata enumeration from full identity qualification. Earlier failure logs are retained for inspection.

## Reuse evidence

Recovered original Digital Brain live_sync.py was inspected for create/modify/rename/delete, provenance and tombstone semantics. SHA-256: D0981129C34F5CD140B836DC956FFA391F2842A3362BC5740A1992731F749546. Original store.py SHA-256: BFCBE8AF757E1DB767F0AD6FCD9E279378037663328AB931EA5C8A8B90089266. Those recovered sources were not mutated. Original recursive visualization and Formula placement were not silently replaced or declared implemented by this work.

The actual Continuum/LDWMD development lineage was resolved to Runtime Fabric commit 234c2ab2e1fd264bd9326e3599685040681e62fa, branch continuum-vtensor-wave1. Read: automation-runtime/src/continuum/continuum-federation.mjs and automation-runtime/src/vtensor/vram-database.mjs. The latter exposes LeeWayDeviceWorkingMemoryDatabase with required hardware allocation/free evidence. Existing Android SQLite scaffolds are not equivalent to those canonical implementations. No Runtime branch merge, new federation service or replacement LDWMD was performed.

## Still blocked

- External owner-granted document trees and full authorized phone filesystem coverage.
- Android SQLite/FileObserver runtime, restart, permission restoration and measured 180-second freshness.
- Original recursive Brain viewer using actual local file/device nodes.
- Canonical Continuum and LDWMD runtime bindings.
- Canonical conversation executor, complete Voice/Skills/Devices/VT integration and full physical acceptance.
- Release signing, qualified platform distribution and canonical LeeWay condensation.

The generated APK is a debug candidate only. No phone install, production promotion, canonical Formula execution or Learning Ledger update occurred.

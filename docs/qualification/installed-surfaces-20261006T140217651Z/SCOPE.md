# Actual installed Agent Lee surface repair

<!-- REGION: LEEWAY.DEPLOYMENT.EVIDENCE; TAG: ACTUAL_NOT_FIXTURE
WHO: Creator-authorized engineering; WHAT: Repair installed UI/diagnostics rather than another source-only milestone.
WHEN: Explicit request to fix existing PC and phone; WHERE: exact existing native package and carrier.
WHY: Prior source/test completion was not deployment. HOW: Backup, signature/hash checks, in-place change and readback.
LICENSE: MIT -->

## Why the owner saw the old behavior

The Android installation was versionCode 26 while the tested source candidate was versionCode 27. The installed phone lacked the portable viewer, had no granted overlay app-op at inspection, and the native badge source still rendered a generic circle. The candidate had no launcher artwork. Its hardware node contained only model/storage basics.

The PC settings controls emitted leeway:open-surface events with no handler for Brain or Diagnostics. Its desktop launcher also required health.status=ok, which the actual carrier never returned; the actual identity field is now validated instead. The existing carrier and existing recovered Brain database were reused, not replaced.

## What actually changed

Android versionCode 28, versionName 1.0.0-live-surface-repair-rc1 was built and installed over the original app with the same verified signing certificate. No uninstall or app-data clearing was executed. Original app APK and private state were backed up locally before mutation. Schema-based migration renamed the existing three databases to neutral filenames, mapped existing Brain/Continuum IDs to this installation's generated Keystore/UUID identity, and recorded a native migration receipt. The last connected readback showed MainActivity running and the completed migration receipt. Full post-upgrade record-count comparison and rendered phone acceptance remain pending because the phone disconnected afterward.

The same PC emblem PNG is now the Android launcher artwork, in-app right-side button and native floating-tab bitmap. Idle colors are visual animation, not fabricated telemetry. Native speech state callbacks may change visual color but acoustic voice selection was not modified. The main phone sphere now uses the already-recovered pinned local Three r128 runtime, not a CDN load.

Phone Diagnostics is wired to the existing internal Brain viewer's hardware universe. Native observations cover CPU descriptors, memory, storage, battery, thermal/power states, display, camera descriptors, audio descriptors, sensor descriptors, network capabilities and OS. Cameras/microphones were not opened and nearby devices were not scanned. Unsupported thermal/utilization/electrical tests remain unavailable, never fabricated. This is not a claim of unrestricted physical motherboard probing.

PC Brain and Diagnostics handlers are active in the existing carrier. The original recovered database is opened via the existing device binding and Brain manifest. No duplicate PC Brain was created. Existing records retain RECOVERED_RECORD_NOT_REOBSERVED labels, while newly sampled native hardware records carry current timestamps and source names. The live database quick_check returned ok, with 198 baseline records and 209 after adding the current hardware root and ten sections. Temperature measurements have no qualified provider and remain unavailable.

The actual installed PC URL was exercised with Playwright without fixture records or substituted network responses. Eleven live checks passed: loaded emblem, changing colors, hamburger-to-original-Brain, return, Diagnostics-to-hardware, fresh memory observations, motherboard provenance, foreign-origin rejection, GET rejection, cross-body rejection, and no uncaught browser errors. The real desktop launcher was executed and an Agent Lee window was observed. The existing native Windows right-edge tab was restarted with the same artwork and in-memory color cycling; it reported visible and produced distinct rendered frames.

## Limits, not waived

Phone overlay permission was not yet verified enabled. The ADB device and wireless mDNS discovery were both absent at final checks, so no phone screenshot, visual click-through, floating-home-tab proof or reboot claim is made. Successful install is not that proof.

The phone conversation executor remains unbound; canonical Continuum/LDWMD execution, shared live voice qualification, complete Skills/VT/Devices integration, full filesystem permissions/ingestion coverage, owner recovery, signing for customers and LeeWay condensation remain separate mandatory gates. Existing historical carrier limitations are not declared fixed by the UI repair. Windows transparent page CSS does not by itself establish a desktop-through native transparent application window. Full cross-device visual parity is not claimed.

This is an owner-requested in-place issue repair, NOT promotion of the entire package to Golden. The debug repair APK is not a universal OS installer or completed customer distribution. Platform-specific adapters and owner permissions remain necessary; endpoint URLs, drive letters and another customer's identity must not become reusable application configuration.

Private database copies, private tar archives, credentials and original package backups remain local and are not committed with this evidence. Formula was not executed and the Learning Ledger was not updated.

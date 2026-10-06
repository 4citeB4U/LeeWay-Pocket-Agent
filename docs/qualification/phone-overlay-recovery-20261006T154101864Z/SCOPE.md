# Phone floating button and Voice ownership — bounded recovery

<!-- REGION: LEEWAY.QUALIFICATION; TAG: OVERLAY_AND_VOICE_ARE_DIFFERENT_AUTHORITIES
WHO: Creator-authorized Agent Lee. WHAT: Record source repair, real Voice proof and disconnected-device boundary.
WHEN: Right-edge floating-button and Voice authority correction. WHERE: Existing Pocket/Voice systems.
WHY: A notification is not a visible button; synthesis is not human audibility.
HOW: Reuse native service, exact source tests, actual current worker, hashes and truthful missing-link result.
LICENSE: MIT -->

## Requested visual and Voice boundaries

The supplied image shows the Windows application and its separate right-edge emblem over the red desktop, plus an unwanted duplicate emblem inside the application. It is not a current phone screenshot. The requested Android emblem is a small native overlay on the owning user's home screen/ordinary apps, not another in-page control or a fullscreen touch barrier. Android owns access to display/audio hardware and its protected system surfaces; it does not choose Agent Lee's speaker identity.

The latest inspected Voice authority repair already resolves the selected employee voice through the existing Voice Fabric. The PC carrier reports authority=LEEWAY_VOICE_FABRIC, deviceMayOverride=false and systemVoiceFallback=false. The current binding remains the provisional kokoro-am_michael selection, pending final owner audition; this pass did not change that selection or promote it.

Nineteen existing Voice authority regressions passed. A new real carrier/worker request included attempted Android/system-voice override fields. The returned audio retained the current Voice Fabric identity and binding revision. The output was non-silent 24000 Hz PCM, 348044 bytes, SHA-256 07C44866EB42D6C96A81393D148679CC5AFD224998B3B8A84DA1C82545364563. The binding file remained byte-identical. This pass did not play the waveform, confirm human hearing, execute the phone conversation path or prove two-way voice-selection synchronization. Existing uncommitted work in Voice SDK/pipeline files was not overwritten.

## Source defects repaired in the existing phone overlay

The old service restored an unbounded center-relative vertical coordinate, did not re-clamp on display changes, silently returned when permission was missing, and advertised Side tab ready before any attachment was observed. MainActivity also re-enabled the service on every resume when permission existed, overriding an explicit disable.

The existing PocketOverlayService now uses one small TYPE_APPLICATION_OVERLAY window anchored to the physical right edge inside observed safe insets. Position is normalized, clamped, and recomputed after display configuration changes. The window is non-focusable and non-touch-modal; it does not deliberately intercept touches outside its bounds. Drag, cancellation, multi-touch and long press cannot become the short tap used to open Agent Lee. The original artwork is reused. No second service, APK, Brain, identity or Voice controller was introduced.

The permission-return flow remembers the owner's request, restores only when permitted and enabled, and respects explicit disable. First-time enablement explains the required owner-controlled overlay permission. It does not set app-ops, bypass permission UI, elevate privileges, change volume, or switch voices. The status record distinguishes permission required, start blocked, attachment pending, attached and stopped; native attachment still explicitly does not prove on-screen visibility. Existing boot/package restart entrypoints remain in use, with platform start failures reported instead of concealed.

The unwanted in-page Android emblem/button and its injected style were removed from the candidate. The native external emblem is retained. Sphere geometry, shared Voice selection and the broader consumer-menu/transparent-sphere tasks were not silently changed or declared complete by this patch.

## Executed and not executed

Thirteen overlay geometry/permission-decision/gesture unit tests passed. The final candidate passed 118 Kotlin/JVM checks and 47 existing Node regression checks, with no failures/skips, and assembled versionCode 29 / versionName 1.0.0-overlay-recovery-rc2. These tests do not execute Android WindowManager or constitute a phone screenshot. See the qualification receipt/source hashes for the exact final APK.

All live ADB and authorized wireless-discovery checks in this pass returned no phone. No Android package was installed, app opened, permission changed, user data modified or screenshot claimed. The actual reason the currently installed phone tab is absent remains unverified until that device is connected; the code defects above are observed source problems, not a fabricated live-device diagnosis.

Next installed gate: inspect current package and permission -> apply only the exact qualified scoped update with existing backup/signature procedure -> owner approval if permission is absent -> show external emblem on home screen -> switch to another ordinary app -> verify emblem remains -> drag/rotate/reopen -> verify bounds and underlying controls -> inspect native status and screenshot. Phone speech and selected-voice propagation are separate gates and must not be inferred from an overlay.

No production merge, Golden promotion, canonical Formula execution or Learning Ledger update occurred. Original broader completion requirements remain open.

# Agent Lee Golden UI — Security, Latency and Flow Code Review
Date: 2026-10-09
Audit scope: currently connected PC runtime, native Python window host, Android Z Fold6 application source, three owner screens, voice and evidence paths.
Method: static source inspection, Android manifest/WebView bridge review, live read-only loopback timings (3 samples per endpoint), SHA-linked existing Golden UI contract.
Classification: **PARTIAL COVERAGE — NOT A COMPLETE EXHAUSTIVE PENETRATION TEST**. No destructive tests, no code changes.

## Executive finding
**Overall: RELEASE GATE FAIL / REMEDIATION REQUIRED**. The approved Golden visual design is unaffected, but security and performance need qualification prior to cross-device production promotion.

## Confirmed positive controls
- Android manifest `android:allowBackup=false`, `android:usesCleartextTraffic=false`. Non-launcher feature activities `exported=false`.
- Several WebViews disable file access and deny WebChromeClient permission requests. Debugging guarded by `BuildConfig.DEBUG` in the inspected classes.
- PC Round Box message handler in `pocket-voice.html` checks both exact `event.origin===location.origin` and `event.source===frame.contentWindow`; move deltas bounded to 300.
- PC carrier JSON POST request rejects mismatched Origin headers. Continuum read and request controls use additional local-origin checks.
- Android owner views validate surface key allowlist and expose small owner-scoped bridge. Avoid declaring this proof of cross-origin safety for every other bridge.

## Findings (source evidence and impact)
### SEC-01 High — JSON CORS and origin coverage
`carrier-server.mjs:385-386` serializes JSON with `Access-Control-Allow-Origin: *`. Some sensitive GET handlers rely only on route-specific local checks; HTTP Origin is absent in many non-browser clients. Review every JSON GET/POST route, authenticated device identity, loopback binding, and request-control checks. Remove wildcard exposure from sensitive responses or constrain to trusted owner origin; do not treat an Origin header as authentication. **Not exploited in this read-only audit.**

### SEC-02 Medium / review required — Android JavaScript bridge attack surface
`ThreeSurfaceActivity.kt:22-39`, `DigitalBrainActivity.kt:31-51`, `FloatingSphereWindow.kt:48-62`, `RoundBoxWindow.kt:53-57`, `VoiceStudioActivity.kt:57-87` and `ModelsActivity.kt:60-82` enable JS/bridges. Several use restricted local assets, but script imports, redirects, and exposed bridge methods require per-page allowlist tests. Some pages lack an explicit inspected `shouldOverrideUrlLoading` guard. Require no untrusted origin can reach a privileged bridge. Test deep links, subframes, intent extras and navigation redirects before PASS.

### SEC-03 Medium — innerHTML sinks and content provenance
`pocket-voice.html:928,1144`, `leeway-three-reference-screens.js:4` construct markup with `innerHTML`. Several runtime values are escaped by the helper `esc`, but all render paths and interpolated values need source-by-source trace. Where only text is needed use `textContent`; do not infer exploitable XSS without a controlled input proof.

### UX-01 High — Round Box selected voice / playback race
`round-box-leeway.html:870-902` requests `/voice/segment`, then `/voice/binding`, and builds a complete base64 WAV in browser memory; no AbortController, timeout or cancellation token on those calls. `new Audio(...)` completion wait can remain pending on a stalled decoder; blob URL cleanup occurs only after the playback promise resolves/rejects. `flushSpeechStream` speaks only after the completed response; not low-latency streaming. Require bounded render/playback, cancellation, stale-turn suppression and live audio receipts. Some effects may be mitigated by provider timeouts not audited here.

### UX-02 High — Android Round Box execution disconnected (as previously observed)
`RoundBoxWindow.kt:72-78` bridge may return `UI_CONNECTED_EXECUTOR_NOT_BOUND` and `VOICE_CONTROL_NOT_BOUND_TO_THIS_SURFACE`; reconcile against latest A2 installed package, then ensure canonical `UnifiedAgentLeeRuntime` and `PocketVoiceActivity` govern all messages, not parallel voice/agent systems. A2 source changes are concurrent; recertify installed binary before claiming.

### PERF-01 High — Evidence index latency
Loopback `GET /owner-evidence` (PowerShell Invoke-WebRequest; 3 sequential samples): **1590.3, 1900.8, 1777.6 ms**. Compare `/ui` 87.9/37.9/71.6ms, `/round-box-leeway.html` 29.4/28.6/29.5ms, `/health` 38.5/33.4/33.6ms, `/voice/binding` 15.4/14.9/14.8ms. These are local request latency observations, not cold/warm render, GPU or UI input timing. Audit evidence rescanning and implement bounded cache/incremental loading and cancellation with receipts, without changing results provenance.

### PERF-02 Medium — UI mutation and drag responsiveness
`pocket-voice.html:1231` and `round-box-leeway.html:1300` attach whole-document `MutationObserver` that iterates controls; repeated mutations can cause avoidable work. Restrict observer to relevant containers, debounce frame work, disconnect on page teardown. PC floating drag still uses asynchronous `host('move',...) ` in `pocket-voice.html` and requires actual 60/120Hz pointer-trace tests; prior user-reported drift is not disproved by process status.

### UX-03 Medium — Golden UI platform parity not yet qualified
Exact PC/phone SVG source hash equality is supported (as recorded in Golden contract). On Mac, tablet, laptop, and physical PC/phone full-window behavior, titlebar red X, resizing, low-DPI/high-DPI, keyboard focus, touch scroll, screen-reader controls, reduced motion, screenshot baselines, responsive breakpoints and actual audible voice remain open acceptance tests.

## Priority remediation gates
1. **P0 security:** endpoint access matrix and origin/authn/credential policy; JS bridge method-by-method origin test; prevent untrusted WebView navigation to bridge-bearing pages; controlled XSS probe across innerHTML sinks.
2. **P0 functional:** canonical Agent Lee input → governed executor → receipt/Continuum → selected voice → playback; real PC/phone audio and no duplicate voice processes.
3. **P1 performance:** Evidence index cache/incremental retrieval and p50/p95; actual pointer-to-frame and gesture delay measurements; stop stale audio/drag queues; memory and GPU profiling.
4. **P1 accessibility/reliability:** red controls, focus and escape semantics, responsive viewport, resize and close, offline/error reporting; platform matrix.
5. **P2 release freeze:** reproducible committed source, SBOM/dependency scan, secret scan, static analyzers, UI integration tests, independent Veritas receipts and Golden manifest admission.

## Verification discipline
The design is **OWNER GOLDEN APPROVED**, while implementation security/performance/reliability is **NOT PRODUCTION-ADMITTED**. Findings SEC-01/02 are risks requiring controlled verification, not asserted real-world compromise. No code modified by this review. Avoid declaring complete exhaustive audit of every UI dependency, installed binary and OS-level attack surface from this bounded examination.

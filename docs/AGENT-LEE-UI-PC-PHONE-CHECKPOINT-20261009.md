# Agent Lee — PC + Galaxy Z Fold6 UI Mission Checkpoint (2026-10-09)

STATUS: INVENTORY IN PROGRESS / NO UI PATCH DEPLOYED / NO VISUAL PASS CLAIMED

## Authority and scope
Human requested UI-only work simultaneously accountable on PC and Galaxy Z Fold6: transparent floating coin, brain + input/output side-by-side instead of overlap, remove unwanted "TAP SPHERE TO TALK" overlay, full hamburger-menu/page/button coverage and phone/PC feature parity. Do not change Formula, Veritas, Continuum, cognition or runtime behavior for this UI mission. PC and phone must have independently observed acceptance results. Do not overwrite preexisting uncommitted files.

## Verified connection inventory
- Windows device: Remote Desktop Commander name Agent-Lee, device ID 82faa11a-9b3c-488f-8dca-dbf23452e984; ping returned pong 2026-10-09T11:45:09Z.
- Phone: Windows-host ADB returned serial RFCX70LNT7X, model SM_F956U, state device. This proves ADB transport only, not an installed UI build or interactive functionality. Observed phone focused activity: Samsung launcher, not Pocket Agent.
- Phone application package observed in earlier ADB query: industries.leeway.pocket (earlier query also emitted Android user-150 security warning).

## Existing sources — preserve; drive paths are observations, NOT hardcoded application identities
- Android Git working tree (observed): E:\LeeWay-Work\agent-lee-fold6-unified ; origin https://github.com/4citeB4U/LeeWay-Pocket-Agent.git ; HEAD 64cf4be8daf85284d6a81bb38fb40534c1072597 ; 83 modified/untracked entries observed by git status summary. DO NOT blindly reset, clean, or overwrite.
- Phone sphere frontend: app/src/main/assets/agent_lee_sphere_transparent.html. HTML/body background currently transparent; contains a status-text 'Tap sphere to speak'. Presence in source does NOT prove installed APK has it.
- Phone floating shell: app/src/main/java/industries/leeway/pocket/FloatingSphereWindow.kt. Window uses PixelFormat.TRANSLUCENT and WebView Color.TRANSPARENT; panel handling enlarges the same floating window to dp(360) x dp(610), a plausible cause for overlapping sphere/chat; causal proof pending installed-render inspection.
- Phone alternate overlay: app/src/main/java/industries/leeway/pocket/PocketOverlayService.kt; uses PixelFormat.TRANSLUCENT. Inspect both overlays before selecting patch.
- Phone other UI activity entrypoints observed: MainActivity.kt, DigitalBrainActivity.kt, VoiceStudioActivity.kt, SkillsActivity.kt, ModelsActivity.kt, AgentTabletActivity.kt, plus assets/digital-brain, assets/skills-ui, assets/models-ui, assets/voice-studio, assets/agent-vt. Enumerate full menu from actual runtime and source; do not assume this list is exhaustive.
- PC active UI candidate path: E:\LeeWay-Work\pc-consciousness-live\runtime\machine-consciousness\pocket-voice.html. Related: agent-ui-interaction.js; agent-lee-tag.png/.webp. PC source directory was NOT a Git repository at inspected level. Live served URL/process-to-file mapping and authority for PC UI remain UNVERIFIED.
- Historical PC and phone UI records (not current acceptance): E:\LeeWay-Work\pc-consciousness-live\receipts\p0-agent-lee and receipts\hamburger-surfaces-20261007.

## Defect matrix: each must be verified separately on BOTH devices
| ID | Surface | PC | Phone | Acceptance |
|---|---|---|---|---|
| UI-01 | Floating coin transparency | OPEN | OPEN | Only actual coin visible; surrounding pixels truly transparent over other apps |
| UI-02 | Brain/chat geometry | OPEN | OPEN | Brain stays visible; input/output opens to right, same initial size where screen permits, responsive fit otherwise |
| UI-03 | Unwanted speech overlay | OPEN | OPEN | No 'TAP SPHERE TO TALK' bubble covering brain; voice action still reachable |
| UI-04 | Hamburger inventory and all subpages | OPEN | OPEN | All intended menu entries exist and route to working pages; no dead controls |
| UI-05 | Cross-device feature parity | OPEN | OPEN | Every intended feature present on PC and phone; platform-specific layouts tested |
| UI-06 | Interaction lifecycle | OPEN | OPEN | Click/touch to open/close, dragging, focus, scroll, Back, orientation/resizing and restoration function |
| UI-07 | Page load and rendering | OPEN | OPEN | No frozen/blank/empty screen; reasonable loading behavior |
| UI-08 | Evidence and diagnostics UI | OPEN | OPEN | Routes and views are reachable and render; no fabricated underlying receipts |
| UI-09 | Accessibility and legibility | OPEN | OPEN | Text, controls, contrast and touch targets work at each device's scaling |

## Required smallest repair sequence
1. Observe actual PC foreground UI and identify its served live source by process/address/file evidence. Inspect phone installed APK and visible overlay including screenshots and UI hierarchy.
2. Enumerate every menu entry and destination in each real installed surface; record route/behavior parity table before changing anything.
3. Establish clean change provenance: note SHA-256 of files to touch, dirty-tree baseline, backup/rollback, expected rendering, acceptance assertions. Do not overwrite unrelated in-progress edits.
4. Repair only confirmed UI CSS/assets/native window placement or menu links in canonical source. Avoid parallel frontend or rewrites.
5. Build/test desktop UI and Android APK independently; deploy only after provenance and rollback are clear; inspect both UIs and screenshot each acceptance case.
6. Record real commands, exit codes, hashes, UI images, installation identifiers and failures in a dated receipt adjacent to this checkpoint. Commit/push only proven scope when authorized, without sweeping unrelated changes.

## Handoff / evidence rules
- This document is an observed-state handoff, not a Veritas PASS receipt.
- No UI source edited; no APK installed; no PC UI redeployed by this checkpoint-writing operation.
- No cross-device UI parity PASS.
- RECEIPT NOT CREATED; LEARNING LEDGER NOT UPDATED.
- NEXT GATE: live PC-source mapping + installed-phone comparison + complete hamburger inventory, then smallest UI patch.

# LEEWAY GOLDEN AGENT LEE UI — FROZEN OWNER STANDARD
**Authority:** Sole human owner approval, October 9, 2026
**State:** DESIGN_AND_INTERACTION_FROZEN; CROSS_PLATFORM_QUALIFICATION_PENDING
**Standard ID:** LEEWAY-AGENT-LEE-UI-GOLDEN-20261009
**Applicability:** Windows PC, laptop, macOS, Android phone, tablet; preserve native OS behavior only where required.
**Principle:** One Agent Lee interface identity across devices. No duplicate visual language or replacement navigation system.

## REQUIRED VISUAL BASELINE
1. Floating Elemental Digital Brain/Agent Lee coin stays visually recognizable and owner movable. Idle motion is gentle; dragging must not trail, drift or block interaction.
2. Hamburger opens the approved reference design: dark navy gradient, neon-cyan bordered navigation cards, Agent Lee header and subtitle, large icon tiles, content labels and OPEN affordances, scrolling, conspicuous RED X to close. **Exact canonical SVG artwork** from `leeway-menu-icons.js`, not substituted emojis, characters, or generic icon packs.
3. Preserve all ten canonical menu destinations and their working actions: Digital Brain, Continuum, Diagnostics, Voice, Agent VT, Models, Skills, Authority & Permissions, Evidence & Receipts, About LeeWay.
4. Owner pages About, Evidence and Authority use their approved dark/cyan independent, full-window reference screens, not inset overlays covering Digital Brain. Reuse `leeway-three-reference-screens.css/js` where bound, with device-local data and permission boundaries. No invented device/permission/receipt counts.
5. Round Box is an independently movable/resizable input/output surface. Background is clear around the actual designed panel. Preserve microphone, conversation, mode toggle and streaming text. One governed Agent Lee executor and selected LeeWay Voice Fabric; no separate browser voice identity.
6. Back, previous-arrow, Exit, Close, and X buttons are RED wherever those controls exist, including hamburger X. Preserve their original actions. Native title-bar close decoration requires platform-native qualification, not CSS-only claims.
7. Windows and other larger displays: separate independent windows, consistent grips and predictable move/resize behavior; content fills available window on resizing and remains readable. Phone/tablet: equivalent capability under touch, safe area and orientation changes without breaking interactions.
8. Do not replace the Digital Brain, knowledge, Formula, Continuum, machine consciousness, execution, or governance to satisfy UI design. No parallel UI or agent runtime.
9. Visual components, interaction states, hover/press/focus, scrolling, navigation order, accessibility and close behavior constitute the standard. Responsive changes must preserve identity while respecting physical device constraints.

## CANONICAL SOURCE SNAPSHOT / AUDIT PIN
Current PC runtime root (installed device-specific deployment, NOT a required absolute installation path): `E:\LeeWay-Work\pc-consciousness-live\runtime\machine-consciousness`.
- PC `pocket-voice.html` SHA256 `B333441AC1D1A2172B265FB24AD783C6AE21C4F90F4CD2B772DDE85EBD013BDD`.
- PC `leeway-menu-icons.js` SHA256 `C3C0EB1D1837D8F0D42D7A58BAFC4B1127DC26ED692951CDDBCF21E298E2FD4F` (matches Android contributor SVG).
- PC `round-box-leeway.html` hash must be re-measured before release; active UI revisions can occur concurrently.
- PC native window host `agent-ui-host.py` SHA256 `D93F43DAE41F64312BBBEAA2F8A0D81E4E636DF0A2ADE3435EB2FBDB9CA9587B`.
Android project `E:\LeeWay-Work\agent-lee-fold6-unified` (installation paths are not canonical), Git HEAD at snapshot `64cf4be8daf85284d6a81bb38fb40534c1072597`; **working tree has uncommitted A2 modifications, so HEAD is not the installed artifact's exact source SHA.**
- Android installed package `industries.leeway.pocket` versionCode 69, versionName `1.0.0-hamburger-text-rc42`.
- Android `agent_lee_sphere_transparent.html` SHA256 `C893B9E016E832083458ACD43B56C033269946722909F919D8C3309D3855A6BC`.
- Android `leeway-menu-icons.js` SHA256 `C3C0EB1D1837D8F0D42D7A58BAFC4B1127DC26ED692951CDDBCF21E298E2FD4F`.
- Android `leeway-three-reference-screens.css` SHA256 `E23B607CD12A3D90F96AB9E1B5274B1B37C219E89E3CD3E8D9451988AEF7E0DB`.
- Android `leeway-three-reference-screens.js` SHA256 `904799B7052DA082BD3ADDE4F35DF30D6B7B5120E8CBBBE542B15BEF51502F1A`.

## GOLDEN PACKAGE ADMISSION / CHANGE CONTROL
- Owner-approved GOLDEN UI design is normative for every distributable Agent Lee package; actual deployed files must be reproduced from a committed, checked-out canonical source and verified by SHA.
- Capture reference screenshots and interaction recordings at qualified canonical dimensions on PC, laptop, macOS, Android phone and tablet. Compare header, icon paths, colors, margins, content, overlay isolation, transitions, drag/resize and actual buttons.
- Validate each menu destination and independently move/resize/close windows and touch equivalents. Check landscape/portrait, high DPI, font scaling, readable content, reduced motion, offline behavior.
- Independently verify speech and input/output flow under selected Voice Fabric and governed execution. This is a separate outstanding functional gate, not automatically PASS from visual approval.
- Any change to visuals, icons, window controls, animation or behavior requires explicit owner approval, source diff, regressions, independent evidence and updated release manifest. Changes to non-UI internals must not change the Golden UI.
- Path agnostic packaging: files/asset locators relative to installed application, no mandatory E:\ or C:\ root.
- Release MUST FAIL if missing/alternate SVG assets, menu items, red navigation controls, split-over-brain owner pages, layout clipping, broken input/output, or unauthorized voice/agent substitution.
- Status at freeze: OWNER DESIGN ACCEPTED. Device-wide visual parity, macOS/laptop/tablet builds, audible voice, full UI gesture acceptance and package reproducibility NOT YET VERIFIED. Do not write a Veritas PASS or production release approval for those without real tests.

## CONTRIBUTORS — ACTIONABLE HANDOFF
**A1 PC:** freeze and reuse exact Android phone SVG/menu and three owner-screen components. Do not substitute approximate UI. Preserve native host and binding. Qualify actual PC window and interaction screenshots.
**A2 Android:** treat this as the mandatory UI contract. Lock exact Android SVG/menu/three owner pages and Round Box behavior into tracked source and release artifact; check installed APK versus source. Do not overwrite A1 PC.
**Mac/laptop/tablet owner:** adopt identical content, assets and behavior, only adapt native window/touch safe areas. Do not fork the design.
**Evidence location:** existing `docs/AGENT-LEE-DUAL-DEVICE-UI-QUALIFICATION-20261009.md` and this Golden contract. Report ALREADY BUILT / MISSING GLUE / TRULY MISSING for any divergence, with file+SHA and tests.

**FROZEN REQUIREMENT:** Deliver this exact Agent Lee visual identity and the same user-facing movement/navigation interactions on every device, not an example or a redesign.

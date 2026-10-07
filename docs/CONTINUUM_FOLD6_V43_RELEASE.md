# Fold6 v43 Continuum UI — source scope

This scoped release captures the existing Agent Lee Android navigation and
Continuum owner's read-only adapter, the upgraded curved honeycomb user interface,
and the local resource SHA-256 admission manifest.

It deliberately excludes user databases, debug signing keys, receipt files,
local device identifiers, installed APK backups, and private voice reference WAVs.
Other pre-existing workstation assets remain owned by their separate repositories.

Observed on 2026-10-07: installed \`industries.leeway.pocket\` versionCode 43,
versionName \`1.0.0-continuum-curved-rc16\`.
APK SHA-256 (owner-local debug signing): \`20bc3c9feb01eca6000316d2c0253048e4e619320d44890e15f1b3edabea6d14\`.
This is *not* a release-signing contract for other customers.
The actual Fold6 Continuum sphere and expanded Knowledge honeycomb were
observed on the physical phone via owner-authorized ADB screenshots.

The Android reader remains owner-body-local; the phone cannot implicitly read
the PC's 837 Knowledge records. PDF rendering in Android WebView needs
separate physical viewer qualification. The source-only CI checks hash pins,
native read-only boundaries, and Android build setup; do not substitute CI
for live on-phone interaction or signed production distribution.

Existing source assets include third-party Mammoth and DOMPurify with upstream
license notices preserved inside the minified bundles.

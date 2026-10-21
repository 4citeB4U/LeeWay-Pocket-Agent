# LEEWAY HEADER — DO NOT REMOVE
REGION: POCKET_AGENT / MODEL_HUB_CONTRACT
TAG: LEEWAY-POCKET-PAL-SLOWLANE-MODEL-HUB-20261021
WHO: Leonard Jerome Lee (Creator); OpenAI coding assistant (authoring support)
WHAT: Cross-device Gemma GGUF model-selection contract and PocketPal handoff
WHEN: 2026-10-21
WHERE: 4citeB4U/LeeWay-Pocket-Agent/model-hub
WHY: Reuse model-selection authority, do not rebuild a third-party inference runtime or pretend desktop PocketPal exists
HOW: Public versioned allowlist, Hugging Face repository+file and explicit PocketPal hub/run native URL
AUTHORIZED ROLE: Proposed model-hub integration contract. No production admission or actual mobile model install asserted.
LICENSE: Creator rights reserved for original text. PocketPal source is MIT; Gemma model weights use Google Gemma terms.

## Canonical portable Gemma allowlist

[portable-gemma.v1.json](catalog/portable-gemma.v1.json) supplies one model identity across platforms. Each LeeWay device chooses its execution backend, but **only a fresh downloaded model hash can establish byte identity**. The registry alone does not prove installation or compatibility.

* **Windows:** Ollama, using its llama.cpp runtime. Explicit owner download approval. Status from Ollama's localhost tags/processes.
* **Android:** PocketPal AI, with `pocketpal://hub/run?repo_id=...`. The upstream app opens its full Hugging Face repo details view, where the owner picks a GGUF and downloads/loads it. Filename in the URI is attribution only, not a forced file.
* **iOS:** Same PocketPal native route, subject to installed app and actual device capabilities. A custom build would require the Apple/Xcode native build and owner signing.

## Important boundaries

No automatic or silent model installation, no proprietary LeeWay authority transfer, and no recommendation to enter sensitive personal documentation into test prompts. Model inference is a **suggestion**, not regulatory or safety certification. Voice One remains governed by LeeWay Voice Fabric, not by the model host. Canonical Formula, Continuum, LDWMD/VRAM Database, independent Veritas and real GPU compute retain their own qualification gates.

## Contest companion

LeeWay SlowLane (new separate Hacktoberfest product) integrates this contract with human-review paperwork and a shared host database. It may display this catalog from GitHub, but each phone's PocketPal runtime remains separate from the application until the native inference bridge is actually implemented and measured.

## License provenance

Upstream PocketPal: https://github.com/a-ghorbani/pocketpal-ai (MIT). GGUF weights are third-party distributables under **Gemma model license**, not PocketPal MIT. Nothing from either third party is copied into this registry. Model files are not redistributed by LeeWay.

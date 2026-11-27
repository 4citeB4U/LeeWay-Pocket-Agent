# LEEWAY HEADER — DO NOT REMOVE
REGION: POCKET_AGENT / MODEL_HUB_SERVICE_CLIENT
TAG: LEEWAY-POCKET-AGENT-MODEL-HUB-EXTRACTION-20261021
WHO: Leonard Jerome Lee (Owner); OpenAI coding assistant (migration contributor)
WHAT: Make native Pocket Agent a client of separate sovereign Model Hub
WHEN: 2026-10-21
WHERE: LeeWay-Pocket-Agent/model-hub
WHY: Single Brain Law; no duplicate source of model inference authority
HOW: Discover and call standalone LeeWay Model Hub service; keep legacy catalog readable during migration
AUTHORIZED ROLE: Source migration contract, not production-native deployment
LICENSE: LeeWay original work reserved; PocketPal MIT and Gemma terms remain separately applicable.

## Canonical model hub moved

**New authoritative repository:** https://github.com/4citeB4U/LeeWay-Model-Hub

**Public model discovery:**
https://4citeb4u.github.io/LeeWay-Model-Hub/catalog/models.json

**Public model-selection widget:**
https://4citeb4u.github.io/LeeWay-Model-Hub/

**Native/browser client module:**
https://github.com/4citeB4U/LeeWay-Model-Hub/blob/main/clients/model-hub-client.mjs

**Runtime endpoint:** Configured owner-authorized service (not GitHub Pages).
The Pocket Agent client calls `GET /v1/catalog`, `GET /v1/models`, `GET /v1/status`, `POST /v1/models/activate`, and `POST /v1/chat`. A model is selected and warmed on its provider before phone/PC/Mac clients send requests; individual phones need no model weights when using the reachable inference service.

## Deprecated local registry

`model-hub/catalog/portable-gemma.v1.json` remains a **read-only compatibility snapshot** to avoid breaking existing Pocket Agent, SlowLane or native adapters. It no longer acts as current selection authority; do not add new models here. New models and admission changes belong in `4citeB4U/LeeWay-Model-Hub/catalog/models.json`.

Do not remove the snapshot until all consumers use the new service and regression tests confirm no remaining references.

## PocketPal is an optional local fallback

The previously implemented PocketPal `pocketpal://hub/run` link is a mobile GGUF download/picker handoff when the owner wants offline inference. It is **not required** for remote model service access. PocketPal's MIT license does not cover Gemma weights. No local phone model installation is implied by registry publication.

## Backend and authority boundaries

Canonical Formula → LDWMD/VRAM → physical GPU → Continuum → independent Veritas → Receipt/Learning Ledger remains a separately qualified execution chain. Published LeeWay Edge GPU source alone does not certify a remotely reachable GPU LLM engine. The Model Hub's GPU provider is fail-closed until a qualified real inference service is registered.

Native HARDLINE visual widget source is separately retained in Windows Runtime Fabric; production UI migration must preserve original content/frame and pass its canonical visual and runtime gates. The new API widget is functional but not represented as a pixel-identical replacement.

<!-- LEEWAY UNIVERSAL FORMULA GOVERNANCE BEGIN -->

# LeeWay Universal Formula Governance

Authority: Creator and LeeWay Standards.
Formula identity: LEEWAY-FORMULA-v1.0.

The canonical LeeWay Formula is mandatory throughout
the entire LeeWay ecosystem.

Every current and future application, capability,
agent, worker, employee, model, provider, adapter,
automation and integration inherits this requirement.

All applicable interactions, responses, decisions,
actions, revisions, executions, computations, model
inferences, resource allocations, information transfers,
memory operations, storage operations and deployments
must consume the canonical Formula through their
authorized and qualified domain contracts.

Formula availability, documentation, configuration,
health and historical results do not constitute actual
Formula consumption for a new operation.

Recover relevant authorized memory before substantive
work. Preserve identity, provenance, Formula integrity,
authorization, original receipts and verified learning.

Never invent observations, adapter mappings, mathematical
results, execution evidence or independent verification.

No application may bypass applicable Formula governance
by dispatching directly to a provider.

Every new application and capability must demonstrate
applicable Formula consumption before production admission.

Preserve the existing authority hierarchy:

Creator > Standards > Root of Trust > Runtime Fabric >
Agent Lee > Harness > Formula > skills/models/tools >
execution > Veritas > Receipt Authority > Learning Ledger.

Execute the complete engineering loop:

INVESTIGATE > DIAGNOSE > PLAN > IMPLEMENT > TEST >
VALIDATE > REPAIR > RETEST > VERIFY > EVIDENCE >
CHECKPOINT > CONTINUE.

Documentation compliance does not establish executable
enforcement or production admission.

<!-- LEEWAY UNIVERSAL FORMULA GOVERNANCE END -->

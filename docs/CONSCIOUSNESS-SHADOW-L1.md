# LeeWay Pocket — Machine Consciousness L1 Shadow Ingress

Status: IMPLEMENTATION CANDIDATE / ZERO ANSWER AUTHORITY

Pinned parent source:
`a18be4b53cd3d74338894b92b48bb7bedff4e208`

Candidate version:
`0.2.13-consciousness-shadow-rc1`

## Purpose

Attach the already-qualified phone-side LeeWay Machine Consciousness shadow to real Pocket user turns without changing the Agent Lee answer path.

This stage answers one question:

> Can Pocket observe the same user turn with the non-LLM consciousness, preserve its state/evidence, and compare that candidate cognition against the actual Agent Lee response without letting the shadow influence the response?

## Authority boundary

The shadow has:

- observe-only authority;
- no Device Bridge action authority;
- no Formula evaluation authority;
- no prompt mutation authority;
- no response replacement authority;
- no memory-promotion authority.

The existing Device Bridge `agent.chat` path remains authoritative for the actual Pocket answer.

## Data path

```text
Pocket user turn
      |
      +----> ConsciousnessShadow.observe()
      |        |
      |        +--> POST localhost:8789/api/ask
      |        +--> private shadow trace
      |
      +----> existing SkillAuthorityClient
      +----> existing DeviceBridgeClient
      +----> existing agent.chat
      +----> actual Agent Lee response
               |
               +--> ConsciousnessShadow.recordActual()
```

The candidate shadow answer is never inserted into `prompt`, `userRequest`, `creatorContext`, or Device Bridge authority evidence.

## Fail-open behavior

If localhost:8789 is absent, slow, malformed or unreachable:

- the shadow records unavailability;
- the real Pocket request continues;
- the existing Agent Lee answer path is unchanged.

Shadow networking runs on a detached background thread with bounded local timeouts.

## Local network boundary

Android cleartext remains denied globally.

The candidate adds a network-security rule that permits cleartext only to `localhost`, because the shadow service is a phone-local Termux runtime.

No external HTTP cleartext destination is authorized.

## Inspectability

Pocket adds one read-only menu entry:

`Consciousness shadow (L1)`

It shows:

- whether the shadow was reachable;
- observed turn count;
- shadow intent;
- prism classification and balance;
- Formula health observed by the shadow;
- continuity state;
- shadow candidate answer;
- actual Agent Lee response preview.

This display does not alter cognition or execution.

## Acceptance before installation

Required:

1. Pocket source-contract test PASS.
2. Consciousness-shadow invariant test PASS.
3. Android unit tests PASS.
4. Android APK build PASS.
5. APK package identity remains `industries.leeway.pocket`.
6. Candidate is signed with the existing LeeWay Android signing authority before update installation.
7. Baseline/rollback source remains pinned at the parent commit.
8. Installed OFF/fail-open behavior must preserve ordinary Agent Lee responses.
9. No shadow output may enter the Device Bridge prompt.

## Rollback

Stop the localhost shadow to disable observation immediately without changing Pocket behavior.

If the Pocket candidate itself must be rolled back, reinstall the pinned baseline build signed by the same LeeWay Android signing authority.

## Claim boundary

This integration can prove side-by-side live observation inside Pocket.

It does not prove that the shadow improves responses, does not close MC-G9, and does not establish subjective consciousness.
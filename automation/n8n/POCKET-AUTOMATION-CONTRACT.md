# LeeWay Pocket Agent — n8n Automation Contract

Pocket Lee uses one canonical n8n webhook as the automation entry point.

## Canonical endpoint

Configure Pocket Lee with the production n8n webhook URL for a workflow named:

`LeeWay Pocket Automation Router`

Recommended webhook path:

`/webhook/leeway-pocket`

## Request contract

Pocket Lee sends JSON:

```json
{
  "source": "leeway-pocket-agent",
  "agent": "Agent Lee",
  "action": "schedule | email | reminder | research | assistant_request",
  "request": "original natural-language request",
  "timestamp": "ISO-8601 timestamp"
}
```

Optional authentication:

`Authorization: Bearer <POCKET_AUTOMATION_TOKEN>`

## Response contract

Every n8n route should return JSON:

```json
{
  "ok": true,
  "message": "Short natural-language result for Agent Lee to speak",
  "receipt_id": "optional LeeWay receipt identifier"
}
```

On failure:

```json
{
  "ok": false,
  "message": "What failed and what Lee should tell the user"
}
```

## Router actions

### schedule
Use n8n for calendar lookup, conflict checking, appointment creation/modification, recurring schedules, and confirmation receipts.

### email
Use n8n for recipient resolution, drafting, sending, follow-up flows, attachments, and email receipts. Consequential sends should use the LeeWay confirmation policy before execution.

### reminder
Use n8n for one-time reminders, recurring reminders, follow-up checks, and notification delivery.

### research
Use n8n to orchestrate search/retrieval tools, collect sources, normalize results, and return a compact evidence-backed result to Pocket Lee.

### assistant_request
Catch-all automation lane for LeeWay workflows that do not belong to one of the explicit actions above. This should route through the LeeWay Harness/Formula authority rather than letting arbitrary n8n nodes become independent authority.

## Recommended n8n organization

Keep the Pocket webhook thin. The router should dispatch to reusable LeeWay sub-workflows:

- `LeeWay / Pocket / Calendar`
- `LeeWay / Pocket / Email`
- `LeeWay / Pocket / Reminders`
- `LeeWay / Pocket / Research`
- `LeeWay / Pocket / General Automation`

The router returns the final short response plus a receipt identifier to Pocket Lee.

## LeeWay authority rule

n8n is the automation/execution plane, not the root authority. Pocket Lee/Harness decides what capability is requested; n8n performs the approved workflow; consequential actions should produce evidence/receipts and obey LeeWay confirmation/governance rules.

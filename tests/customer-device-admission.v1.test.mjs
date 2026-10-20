/*
REGION: LEEWAY.POCKET.GOLDEN_ENROLLMENT_QUALIFICATION
TAG: CUSTOMER_DEVICE_ADMISSION_FAIL_CLOSED_V1
WHAT: Qualify existing candidate evaluator against denied authority, ownership, device proof, revocation and replay.
WHY: Candidate metadata must never equal an authorized or redeemed installation.
WHO: LeeWay source release qualification; no credentials or live device access.
WHERE: tests/customer-device-admission.v1.test.mjs
WHEN: 2026-10-20
HOW: Deterministic fixtures and Node built-in test runner. LICENSE: MIT.
*/
import test from 'node:test';
import assert from 'node:assert/strict';
import {admissionSchema,evaluateEnrollmentAdmission} from '../contracts/customer-device-admission.v1.mjs';

const NOW=1792500000000;
const valid=()=>({
 nowMs:NOW,
 request:{schemaVersion:admissionSchema,ownerId:'owner-fixture',agentId:'agent-lee',
  deviceId:'device-fixture',challengeId:'challenge-fixture',platform:'android',
  expiresAtMs:NOW+60000,challengeConsumed:false,approvalCommitted:true,platformCompatible:true},
 ownerProof:{verified:true,ownerId:'owner-fixture',challengeId:'challenge-fixture',issuerAuthorityVerified:true},
 deviceProof:{verified:true,deviceId:'device-fixture',challengeId:'challenge-fixture',
  ownerId:'owner-fixture',proofOfPossessionVerified:true,publicKeyFingerprint:'fixture-public-fingerprint'},
 entitlement:{verified:true,ownerId:'owner-fixture',active:true,
  entitlementId:'entitlement-fixture',issuerAuthorityVerified:true},
 revocation:{authoritativeReadback:true,ownerId:'owner-fixture',deviceId:'device-fixture',revoked:false}
});
const denied=(change,reason)=>{
 const f=valid();change(f);
 const result=evaluateEnrollmentAdmission(f);
 assert.equal(result.status,'BLOCKED');
 assert.equal(result.reason,reason);
 assert.equal(result.authorized,false);
 assert.equal(result.candidateSha256,undefined);
};

test('complete synthetic proof is candidate only, never authorization or installation',()=>{
 const result=evaluateEnrollmentAdmission(valid());
 assert.equal(result.status,'CANDIDATE_ADMISSION_READY');
 assert.equal(result.authorized,false);
 assert.equal(result.reason,'AUTHORITY_REDEMPTION_AND_DURABLE_CONSUMPTION_REQUIRED');
 assert.match(result.candidateSha256,/^[a-f0-9]{64}$/);
});
test('invalid schema or substituted Agent Lee identity is refused',()=>{
 denied(f=>{f.request.schemaVersion='unknown'},'ENROLLMENT_REQUEST_INVALID');
 denied(f=>{f.request.agentId='other-agent'},'AGENT_IDENTITY_INVALID');
});
test('missing trusted owner signature and wrong challenge are refused',()=>{
 denied(f=>{f.ownerProof.issuerAuthorityVerified=false},'OWNER_AUTHENTICATION_NOT_VERIFIED');
 denied(f=>{f.ownerProof.challengeId='different'},'OWNER_AUTHENTICATION_NOT_VERIFIED');
});
test('inactive or wrong-owner entitlement is refused',()=>{
 denied(f=>{f.entitlement.active=false},'ENTITLEMENT_NOT_VERIFIED');
 denied(f=>{f.entitlement.ownerId='different-owner'},'ENTITLEMENT_NOT_VERIFIED');
});
test('device identity mismatch and missing possession are refused',()=>{
 denied(f=>{f.deviceProof.deviceId='other-device'},'DEVICE_KEY_PROOF_NOT_VERIFIED');
 denied(f=>{f.deviceProof.proofOfPossessionVerified=false},'DEVICE_KEY_PROOF_NOT_VERIFIED');
});
test('revoked or unreadable revocation is refused',()=>{
 denied(f=>{f.revocation.revoked=true},'REVOCATION_STATE_UNVERIFIED');
 denied(f=>{f.revocation.authoritativeReadback=false},'REVOCATION_STATE_UNVERIFIED');
});
test('expired and unbounded challenges are refused',()=>{
 denied(f=>{f.request.expiresAtMs=NOW},'ENROLLMENT_REQUEST_EXPIRED_OR_UNBOUNDED');
 denied(f=>{f.request.expiresAtMs=NOW+300001},'ENROLLMENT_REQUEST_EXPIRED_OR_UNBOUNDED');
});
test('spent challenge, missing approval, incompatible platform all refuse',()=>{
 denied(f=>{f.request.challengeConsumed=true},'CHALLENGE_OR_PLATFORM_NOT_ADMITTED');
 denied(f=>{f.request.approvalCommitted=false},'CHALLENGE_OR_PLATFORM_NOT_ADMITTED');
 denied(f=>{f.request.platformCompatible=false},'CHALLENGE_OR_PLATFORM_NOT_ADMITTED');
});

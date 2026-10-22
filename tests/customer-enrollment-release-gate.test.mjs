/*
REGION: LEEWAY.POCKET.RELEASE_QUALIFICATION
TAG: CUSTOMER_ENROLLMENT_EVIDENCE_REGRESSION_V1
5WH: WHAT=Test composite release admission and fail-closed customer-enrollment scope.
WHY=Requirement presence must not be mistaken for actual customer/device acceptance.
WHO=LeeWay release qualification. WHERE=tests/customer-enrollment-release-gate.test.mjs.
WHEN=2026-10-08. HOW=Exercise real validator without fabricated real-world evidence.
AUTHORIZED_ROLES: Unit tests only. LICENSE: MIT.
*/
import test from 'node:test';
import assert from 'node:assert/strict';
import {composeCustomerGoldenPolicy,assessCustomerGoldenEvidence} from '../scripts/customer-enrollment-release-gate.mjs';
test('base 50 and new 16 are required simultaneously',()=>{
 const c=composeCustomerGoldenPolicy();
 assert.equal(c.cases.length,66);
 assert.equal(new Set(c.cases).size,66);
 assert.ok(c.cases.includes('digital-brain/revocation-local-brain-survival'));
 assert.ok(c.cases.includes('customer-enrollment/owner-approved-device-enrollment'));
 assert.ok(c.cases.includes('customer-enrollment/unauthorized-copy-denied'));
 assert.ok(c.cases.includes('customer-enrollment/offline-reconciliation-and-conflict-handling'));
});
test('composite policy is hash-bound to original and enrollment extensions',()=>{
 const c=composeCustomerGoldenPolicy();
 assert.match(c.baseSha256,/^[a-f0-9]{64}$/);
 assert.match(c.extensionSha256,/^[a-f0-9]{64}$/);
 assert.match(c.policySha256,/^[a-f0-9]{64}$/);
 assert.notEqual(c.policySha256,c.baseSha256);
});
test('empty physical evidence blocks release across all cases',async()=>{
 const c=composeCustomerGoldenPolicy();
 const result=await assessCustomerGoldenEvidence({artifactSha256:'a'.repeat(64),sourceCommit:'b'.repeat(40),expectedProfiles:['windows'],dossier:{evidence:[]}});
 assert.equal(result.status,'BLOCKED');
 assert.equal(result.requiredCaseCount,66);
 assert.equal(result.requiredEvidenceCount,66);
 assert.equal(result.verifiedEvidenceCount,0);
 assert.equal(result.releasePromoted,false);
 assert.ok(result.blockers.length>0);
});
test('adding second platform increases mandatory evidence and remains blocked',async()=>{
 const result=await assessCustomerGoldenEvidence({artifactSha256:'a'.repeat(64),sourceCommit:'b'.repeat(40),expectedProfiles:['windows','android'],dossier:{evidence:[]}});
 assert.equal(result.requiredEvidenceCount,132);
 assert.equal(result.status,'BLOCKED');
 assert.equal(result.verifiedEvidenceCount,0);
});

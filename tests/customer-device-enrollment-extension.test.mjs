/*
REGION: LEEWAY.POCKET.GOLDEN_ENROLLMENT_QUALIFICATION
TAG: CUSTOMER_ENROLLMENT_REQUIREMENT_GATE_V1
5WH: WHAT=Check additive customer/device enrollment release requirements.
WHY=No single-device Golden release can claim customer-wide portability without separate evidence.
WHO=LeeWay Pocket Golden release qualification. WHERE=tests/customer-device-enrollment-extension.test.mjs.
WHEN=2026-10-08. HOW=Read actual contracts, reject missing requirements, preserve old cases.
AUTHORIZED_ROLES: Read-only test runner. LICENSE: MIT.
*/
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const load = rel => JSON.parse(fs.readFileSync(new URL(rel, import.meta.url),'utf8').replace(/^\uFEFF/,''));
const ext = load('../contracts/customer-device-enrollment-extension.v1.json');
const base = load('../contracts/golden-system-release.v1.json');
test('existing Golden release groups remain intact',()=>{
 assert.equal(Object.keys(base.requiredGroups).length,10);
 assert.equal(base.distribution.customerReleaseFromDebugBuild,false);
 assert.equal(base.distribution.freshPerInstallationIdentity,true);
});
test('single-customer entitlement is distinct from device identities',()=>{
 assert.equal(ext.invariants.oneCustomerEntitlement,true);
 assert.equal(ext.invariants.oneAgentLeeIdentityPerCustomer,true);
 assert.equal(ext.invariants.perDeviceDigitalBrain,true);
 assert.equal(ext.invariants.deviceNamesNotOwnershipProof,true);
});
test('enrollment cannot grant permissions, duplicate keys or customer secrets',()=>{
 for(const key of ['noCustomerSecretsInDistributedMaster','allDevicesRequireExplicitEnrollment','noAutomaticPermissionEscalation']) assert.equal(ext.invariants[key],true);
 assert.ok(ext.requiredCases.includes('device-local-key-generated-not-copied'));
});
test('all sixteen added acceptance cases are unique and require physical evidence',()=>{
 assert.equal(ext.requiredCases.length,16);
 assert.equal(new Set(ext.requiredCases).size,16);
 assert.equal(ext.releasePolicy,'ALL_REQUIRED_CASES_PER_DECLARED_PLATFORM_PROFILE_WITH_PHYSICAL_EVIDENCE');
 assert.equal(ext.mergeState,'CANDIDATE_PENDING_CANONICAL_RELEASE_GATE_INTEGRATION');
});
test('old Golden distribution and offline-Brain protections are preserved',()=>{
 assert.ok(base.requiredGroups['digital-brain'].includes('revocation-local-brain-survival'));
 assert.ok(base.requiredGroups.continuum.includes('revocation-stops-egress-preserves-local-data'));
 assert.equal(ext.invariants.existing46CasesPreserved,true);
});

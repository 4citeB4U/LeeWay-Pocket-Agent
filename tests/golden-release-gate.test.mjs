/*
REGION: LEEWAY.POCKET.RELEASE_QUALIFICATION
TAG: GOLDEN_RELEASE_GATE_TEST
WHO: Agent Lee qualification
WHAT: Verify the release evidence validator, not customer features or physical-device behavior.
WHEN: Before integrating the validator into the existing release harness.
WHERE: Isolated temporary files; no APK installed and no synthetic record promoted to evidence.
WHY: Missing, stale, forged, skipped and unit-only evidence must not produce a golden release.
HOW: Deliberately synthetic records plus a TEST-ONLY verifier; test artifacts are removed afterward.
LICENSE: MIT
*/
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {assessGoldenEvidence,evaluateFreshnessTrace,loadGoldenPolicy,requiredCases,loadPlatformProfile} from '../scripts/golden-release-gate.mjs';
const sha=b=>createHash('sha256').update(b).digest('hex');
function fixture(t,profiles=['test-fixture-profile']){
 const root=fs.mkdtempSync(path.join(os.tmpdir(),'leeway-golden-gate-unit-'));t.after(()=>fs.rmSync(root,{recursive:true,force:true}));
 const {policy,sha256:policySha256}=loadGoldenPolicy();const artifactSha256='a'.repeat(64),sourceCommit='b'.repeat(40);
 const {profile:platformProfile,sha256:platformProfileSha256}=loadPlatformProfile(new URL('../contracts/platforms/android.v1.json',import.meta.url));
 const bytes=Buffer.from(JSON.stringify({TEST_FIXTURE_NOT_DEVICE_EVIDENCE:true,changes:[{authorized:true,clockDomain:'SOURCE_TO_DURABLE_COMMIT_MONOTONIC',sourceChangeObserved:true,versionCommitted:true,sourceChangeMs:1000,durableCommitMs:121000}]}));
 fs.writeFileSync(path.join(root,'fixture.json'),bytes);
 const evidence=profiles.flatMap(profileId=>requiredCases(policy).map(caseId=>({TEST_FIXTURE_NOT_DEVICE_EVIDENCE:true,profileId,caseId,platformId:platformProfile.id,platformProfileSha256,artifactSha256,sourceCommit,policySha256,status:'PASS',testsExecuted:true,skipped:0,failures:0,errors:0,assertions:1,executionKind:'PHYSICAL_DEVICE',evidenceSha256:sha(bytes),evidencePath:'fixture.json'})));
 return{policy,policySha256,platformProfile,platformProfileSha256,artifactSha256,sourceCommit,expectedProfiles:profiles,dossier:{evidence},evidenceRoot:root,verifyEvidence:async()=>true};
}
const has=(result,code)=>result.blockers.some(b=>b.code===code);
test('all ten groups and customer-retained Brain obligations remain required',()=>{
 const {policy}=loadGoldenPolicy();assert.equal(Object.keys(policy.requiredGroups).length,10);
 const cases=requiredCases(policy);for(const c of ['digital-brain/revocation-local-brain-survival','digital-brain/owner-export-restore','digital-brain/new-file-record-within-180s','continuum/revocation-stops-egress-preserves-local-data','devices-commander/revocation-denies-next-remote-command','distribution/customer-neutral-payload'])assert.ok(cases.includes(c));
 assert.equal(policy.ownerRetainedBrain.localBootRequiresSubscriptionOrRemoteLogin,false);
 assert.equal(policy.distribution.creatorOrDeviceAuthorityInheritedByRecipient,false);
});
test('complete synthetic metadata exercises gate logic only and never distributes',async t=>{
 const r=await assessGoldenEvidence(fixture(t));assert.equal(r.status,'EVIDENCE_QUALIFIED_NOT_DISTRIBUTED');assert.equal(r.releasePromoted,false);assert.equal(r.installed,false);assert.equal(r.requiredEvidenceCount,r.verifiedEvidenceCount);
});
test('self-reported PASS without existing Veritas verification is blocked',async t=>{const f=fixture(t);delete f.verifyEvidence;const r=await assessGoldenEvidence(f);assert.equal(r.status,'BLOCKED');assert.ok(has(r,'AUTHORIZED_EXISTING_VERITAS_VERIFIER_REQUIRED'));assert.equal(r.verifiedEvidenceCount,0)});
test('missing platform matrix cannot pass with zero required checks',async t=>{const f=fixture(t);f.expectedProfiles=[];assert.ok(has(await assessGoldenEvidence(f),'DECLARED_SUPPORTED_PLATFORM_MATRIX_REQUIRED'))});
test('one missing ownership/revocation case blocks release',async t=>{const f=fixture(t);f.dossier.evidence=f.dossier.evidence.filter(e=>e.caseId!=='digital-brain/revocation-local-brain-survival');const r=await assessGoldenEvidence(f);assert.equal(r.status,'BLOCKED');assert.ok(has(r,'MISSING_ACCEPTANCE_EVIDENCE'))});
test('a declared second supported profile cannot be silently omitted',async t=>{const f=fixture(t);f.expectedProfiles.push('second-test-profile');const r=await assessGoldenEvidence(f);assert.equal(r.requiredEvidenceCount,r.requiredCaseCount*2);assert.ok(has(r,'MISSING_ACCEPTANCE_EVIDENCE'))});
test('skipped, failed, errored or unexecuted proof does not pass',async t=>{for(const [field,value] of [['status','SKIPPED'],['skipped',1],['failures',1],['errors',1],['testsExecuted',false],['assertions',0]]){const f=fixture(t);f.dossier.evidence[0][field]=value;assert.ok(has(await assessGoldenEvidence(f),'MANDATORY_ACCEPTANCE_NOT_PASSED'),field)}});
test('another APK, source or contract hash cannot reuse a prior PASS',async t=>{for(const key of ['artifactSha256','sourceCommit','policySha256']){const f=fixture(t);f.dossier.evidence[0][key]='c'.repeat(key==='sourceCommit'?40:64);assert.ok(has(await assessGoldenEvidence(f),'EVIDENCE_IS_FOR_DIFFERENT_ARTIFACT_SOURCE_OR_POLICY'))}});
test('unit tests cannot pose as real device acceptance',async t=>{const f=fixture(t);f.dossier.evidence[0].executionKind='UNIT_TEST';assert.ok(has(await assessGoldenEvidence(f),'UNIT_OR_FIXTURE_IS_NOT_DEVICE_ACCEPTANCE'))});
test('duplicate result cannot hide which record was accepted',async t=>{const f=fixture(t);f.dossier.evidence.push({...f.dossier.evidence[0]});assert.ok(has(await assessGoldenEvidence(f),'DUPLICATE_ACCEPTANCE_EVIDENCE'))});
test('changed evidence bytes are detected',async t=>{const f=fixture(t);fs.appendFileSync(path.join(f.evidenceRoot,'fixture.json'),' ');assert.ok(has(await assessGoldenEvidence(f),'EVIDENCE_HASH_MISMATCH'))});
test('receipt path traversal is rejected',async t=>{const f=fixture(t);f.dossier.evidence[0].evidencePath='../other.json';assert.ok(has(await assessGoldenEvidence(f),'EVIDENCE_FILE_UNAVAILABLE_OR_UNSAFE'))});
test('receipt symlink cannot escape the scoped evidence directory',async t=>{const f=fixture(t);const outside=fs.mkdtempSync(path.join(os.tmpdir(),'leeway-outside-'));t.after(()=>fs.rmSync(outside,{recursive:true,force:true}));fs.writeFileSync(path.join(outside,'fixture.json'),fs.readFileSync(path.join(f.evidenceRoot,'fixture.json')));fs.symlinkSync(outside,path.join(f.evidenceRoot,'escape'),'junction');f.dossier.evidence[0].evidencePath='escape/fixture.json';assert.ok(has(await assessGoldenEvidence(f),'EVIDENCE_FILE_UNAVAILABLE_OR_UNSAFE'))});
test('Veritas refusal or exception blocks release',async t=>{const f=fixture(t);f.verifyEvidence=async()=>false;assert.ok(has(await assessGoldenEvidence(f),'VERITAS_EVIDENCE_REJECTED'));f.verifyEvidence=async()=>{throw Error('offline')};assert.ok(has(await assessGoldenEvidence(f),'VERITAS_EVIDENCE_VERIFICATION_FAILED'))});
test('freshness measures source event to durable commit, not poll-to-commit',()=>{assert.equal(evaluateFreshnessTrace([]).status,'BLOCKED');assert.equal(evaluateFreshnessTrace([{authorized:true,sourceChangeMs:0,durableCommitMs:10}]).status,'BLOCKED');const event={authorized:true,clockDomain:'SOURCE_TO_DURABLE_COMMIT_MONOTONIC',sourceChangeObserved:true,versionCommitted:true,sourceChangeMs:0,durableCommitMs:180000};assert.equal(evaluateFreshnessTrace([event]).status,'PASS');assert.equal(evaluateFreshnessTrace([{...event,durableCommitMs:180001}]).status,'FAIL');assert.equal(evaluateFreshnessTrace([{...event,sourceChangeObserved:false}]).status,'BLOCKED')});
test('late file trace cannot be hidden by a signed PASS label',async t=>{const f=fixture(t);const bytes=Buffer.from(JSON.stringify({changes:[{authorized:true,clockDomain:'SOURCE_TO_DURABLE_COMMIT_MONOTONIC',sourceChangeObserved:true,versionCommitted:true,sourceChangeMs:0,durableCommitMs:180001}]}));fs.writeFileSync(path.join(f.evidenceRoot,'late.json'),bytes);Object.assign(f.dossier.evidence.find(e=>e.caseId==='digital-brain/new-file-record-within-180s'),{evidencePath:'late.json',evidenceSha256:sha(bytes)});assert.ok(has(await assessGoldenEvidence(f),'FILE_FRESHNESS_ACCEPTANCE_NOT_MET'))});

test('master release policy is platform-neutral and retains all 46 cases',()=>{
 const {policy}=loadGoldenPolicy();assert.equal(policy.schemaVersion,'leeway.golden-system-release.v1');
 assert.equal('oneAndroidApk' in policy.distribution,false);assert.equal(requiredCases(policy).length,46);
 assert.equal(policy.architecture.core,'PLATFORM_NEUTRAL');
 const {profile}=loadPlatformProfile(new URL('../contracts/platforms/android.v1.json',import.meta.url));
 assert.equal(profile.packaging.oneAndroidApk,true);assert.equal(profile.platformTested,false);
});
test('old Android master schema is no longer accepted',()=>{
 const {policy}=loadGoldenPolicy();policy.schemaVersion='leeway.golden-apk-release.v1';assert.throws(()=>requiredCases(policy),/GOLDEN_POLICY_INVALID/);
});
test('missing platform adapter profile cannot admit the master product',async t=>{
 const f=fixture(t);delete f.platformProfile;assert.ok(has(await assessGoldenEvidence(f),'HASH_BOUND_PLATFORM_PROFILE_REQUIRED'));
});
test('platform policy change invalidates otherwise matching evidence',async t=>{
 const f=fixture(t);f.platformProfileSha256='d'.repeat(64);assert.ok(has(await assessGoldenEvidence(f),'EVIDENCE_IS_FOR_DIFFERENT_PLATFORM'));
});
test('Android evidence cannot certify another platform',async t=>{
 const f=fixture(t);f.platformProfile={...f.platformProfile,id:'different-platform-test-only',nativeArtifactType:'TEST'};
 assert.ok(has(await assessGoldenEvidence(f),'EVIDENCE_IS_FOR_DIFFERENT_PLATFORM'));
});
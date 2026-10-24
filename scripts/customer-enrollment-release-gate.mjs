/*
REGION: LEEWAY.POCKET.RELEASE_QUALIFICATION
TAG: CUSTOMER_ENROLLMENT_COMPOSITE_EVIDENCE_GATE_V1
5WH: WHAT=Extend the existing Golden evidence admission using the customer-enrollment contract.
WHY=An enrollment contract alone must never certify customer entitlement or multi-device release.
WHO=LeeWay Golden release verifier. WHERE=scripts/customer-enrollment-release-gate.mjs.
WHEN=2026-10-08. HOW=Load pinned contracts, compose unique mandatory cases, delegate evidence checking to existing gate.
AUTHORIZED_ROLES: Release assessment; no device changes, enrollment or promotion. LICENSE: MIT.
*/
import fs from 'node:fs';
import {createHash} from 'node:crypto';
import {loadGoldenPolicy,requiredCases,assessGoldenEvidence} from './golden-release-gate.mjs';
const hash = bytes => createHash('sha256').update(bytes).digest('hex');
export const enrollmentContractPath = new URL('../contracts/customer-device-enrollment-extension.v1.json',import.meta.url);
export function loadEnrollmentContract(){
 const bytes=fs.readFileSync(enrollmentContractPath);
 const extension=JSON.parse(bytes.toString('utf8').replace(/^\uFEFF/,''));
 if(extension.schemaVersion!=='leeway.golden.customer-device-enrollment-extension.v1' ||
 extension.parent!=='golden-system-release.v1.json' ||
 extension.scope!=='ADDITIVE_RELEASE_REQUIREMENTS_NOT_RUNTIME_IMPLEMENTATION' ||
 !Array.isArray(extension.requiredCases) || extension.requiredCases.length!==16 ||
 new Set(extension.requiredCases).size!==16 ||
 extension.requiredCases.some(id=>typeof id!=='string'||!/^[a-z0-9][a-z0-9._/-]{0,159}$/.test(id)) ||
 extension.releasePolicy!=='ALL_REQUIRED_CASES_PER_DECLARED_PLATFORM_PROFILE_WITH_PHYSICAL_EVIDENCE'){
 throw Error('CUSTOMER_ENROLLMENT_CONTRACT_INVALID');
 }
 return {extension,sha256:hash(bytes)};
}
export function composeCustomerGoldenPolicy(){
 const {policy,sha256:baseSha256}=loadGoldenPolicy();
 const {extension,sha256:extensionSha256}=loadEnrollmentContract();
 const base=requiredCases(policy);
 if(base.length!==50)throw Error('BASE_GOLDEN_REQUIREMENTS_UNRECONCILED');
 const overlap=extension.requiredCases.filter(id=>base.includes(id));
 if(overlap.length)throw Error('ENROLLMENT_DUPLICATES_BASE_CASES');
 const combined=structuredClone(policy);
 combined.requiredGroups['customer-enrollment']=[...extension.requiredCases];
 const cases=requiredCases(combined);
 if(cases.length!==66)throw Error('CUSTOMER_ENROLLMENT_CASE_COUNT_MISMATCH');
 const policySha256=hash(Buffer.from(JSON.stringify({baseSha256,extensionSha256,caseIds:cases})));
 return {policy:combined,policySha256,baseSha256,extensionSha256,cases,status:'CANDIDATE_EVIDENCE_GATE_NOT_PROMOTED'};
}
export async function assessCustomerGoldenEvidence(input={}){
 const composite=composeCustomerGoldenPolicy();
 const result=await assessGoldenEvidence({...input,policy:composite.policy,policySha256:composite.policySha256});
 return {...result,compositePolicySha256:composite.policySha256,basePolicySha256:composite.baseSha256,enrollmentContractSha256:composite.extensionSha256,customerEnrollmentGate:'CANDIDATE_NOT_PROMOTED'};
}

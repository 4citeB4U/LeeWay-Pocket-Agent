/*
REGION: LEEWAY.POCKET.RELEASE_QUALIFICATION
TAG: GOLDEN_SYSTEM_EVIDENCE_ADMISSION
WHO: Creator-authorized release qualification; existing Veritas supplies evidence verification.
WHAT: Extend the existing single-APK gate with mandatory customer-release evidence checks.
WHEN: Before naming or distributing an artifact as golden.
WHERE: Pocket release tooling, not a new runtime, registry, subscription server or Veritas engine.
WHY: Build success, a source score or self-labelled PASS cannot authorize customer distribution.
HOW: Bind every required profile/case to exact artifact/source/contract/evidence hashes; fail closed.
LICENSE: MIT
*/
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
const HASH=/^[a-f0-9]{64}$/i;
const COMMIT=/^[a-f0-9]{40}$/i;
const ID=/^[a-z0-9][a-z0-9._/-]{0,159}$/i;
const digest=b=>createHash('sha256').update(b).digest('hex');
const artifactCases=new Set(['distribution/release-signature-and-payload-integrity','distribution/customer-neutral-payload','distribution/complete-payload-compression-roundtrip','distribution/complete-dependency-bill-of-materials']);
export const policyPath=fileURLToPath(new URL('../contracts/golden-system-release.v1.json',import.meta.url));
export function loadGoldenPolicy(){const bytes=fs.readFileSync(policyPath);return{policy:JSON.parse(bytes.toString('utf8').replace(/^\uFEFF/,'')),sha256:digest(bytes)}}
export function loadPlatformProfile(profilePath){
 const bytes=fs.readFileSync(profilePath),profile=JSON.parse(bytes.toString('utf8').replace(/^\uFEFF/,''));
 if(profile.schemaVersion!=='leeway.platform-release-profile.v1'||!ID.test(profile.id||'')||typeof profile.nativeArtifactType!=='string'||!profile.nativeArtifactType.trim())throw Error('PLATFORM_PROFILE_INVALID');
 return{profile,sha256:digest(bytes)};
}
export function requiredCases(policy){
 if(policy.schemaVersion!=='leeway.golden-system-release.v1'||!policy.requiredGroups||Array.isArray(policy.requiredGroups))throw Error('GOLDEN_POLICY_INVALID');
 const result=[];
 for(const [group,cases] of Object.entries(policy.requiredGroups)){
  if(!ID.test(group)||!Array.isArray(cases)||!cases.length||cases.some(c=>typeof c!=='string'||!ID.test(c)))throw Error('GOLDEN_POLICY_CASE_INVALID');
  result.push(...cases.map(c=>group+'/'+c));
 }
 if(!result.length||new Set(result).size!==result.length)throw Error('GOLDEN_POLICY_DUPLICATE_OR_EMPTY');
 return result;
}
function safeEvidence(root,relative){
 if(typeof relative!=='string'||!relative||relative.length>512||path.isAbsolute(relative)||relative.split(/[\\/]/).some(p=>p==='..')||/^[A-Za-z]:/.test(relative))throw Error('EVIDENCE_PATH_INVALID');
 const base=fs.realpathSync(root),p=fs.realpathSync(path.resolve(base,relative)),rel=path.relative(base,p);
 if(path.isAbsolute(rel)||rel==='..'||rel.startsWith('..'+path.sep)||!fs.statSync(p).isFile())throw Error('EVIDENCE_PATH_OUTSIDE_ROOT');
 if(fs.statSync(p).size>32*1024*1024)throw Error('EVIDENCE_FILE_TOO_LARGE');
 return fs.readFileSync(p);
}
export function evaluateFreshnessTrace(trace){
 if(!Array.isArray(trace)||trace.length===0)return{status:'BLOCKED',reason:'FILE_CHANGE_TRACE_REQUIRED'};
 const lags=[];
 for(const e of trace){
  if(!e||e.authorized!==true||e.clockDomain!=='SOURCE_TO_DURABLE_COMMIT_MONOTONIC'||e.sourceChangeObserved!==true||e.versionCommitted!==true||!Number.isSafeInteger(e.sourceChangeMs)||!Number.isSafeInteger(e.durableCommitMs)||e.sourceChangeMs<0||e.durableCommitMs<e.sourceChangeMs)return{status:'BLOCKED',reason:'AUTHORITATIVE_SOURCE_TO_COMMIT_TRACE_REQUIRED'};
  lags.push(e.durableCommitMs-e.sourceChangeMs);
 }
 const maxMs=Math.max(...lags);
 return{status:maxMs<=180000?'PASS':'FAIL',samples:lags.length,maxMs,targetMs:120000,maximumMs:180000,misses:lags.filter(t=>t>180000).length};
}
export async function assessGoldenEvidence({policy,policySha256,artifactSha256,sourceCommit,expectedProfiles,dossier,evidenceRoot,verifyEvidence,platformProfile,platformProfileSha256}={}){
 const cases=requiredCases(policy),blockers=[],add=(code,detail)=>blockers.push({code,...detail});
 if(!HASH.test(policySha256||'')||!HASH.test(artifactSha256||'')||!COMMIT.test(sourceCommit||''))add('ARTIFACT_SOURCE_POLICY_IDENTITY_REQUIRED');
 const platformValid=platformProfile?.schemaVersion==='leeway.platform-release-profile.v1'&&ID.test(platformProfile?.id||'')&&typeof platformProfile?.nativeArtifactType==='string'&&platformProfile.nativeArtifactType.trim().length>0&&HASH.test(platformProfileSha256||'');
 if(!platformValid)add('HASH_BOUND_PLATFORM_PROFILE_REQUIRED');
 const profiles=Array.isArray(expectedProfiles)?expectedProfiles:[];
 if(!profiles.length||new Set(profiles).size!==profiles.length||profiles.some(p=>typeof p!=='string'||!ID.test(p)))add('DECLARED_SUPPORTED_PLATFORM_MATRIX_REQUIRED');
 const records=Array.isArray(dossier?.evidence)?dossier.evidence:[];
 if(typeof verifyEvidence!=='function')add('AUTHORIZED_EXISTING_VERITAS_VERIFIER_REQUIRED');
 const grouped=new Map();
 for(const e of records){
  if(!e||typeof e!=='object'||!profiles.includes(e.profileId)||!cases.includes(e.caseId)){add('UNEXPECTED_OR_INVALID_EVIDENCE_RECORD');continue;}
  const key=e.profileId+'::'+e.caseId;grouped.set(key,[...(grouped.get(key)||[]),e]);
 }
 let verified=0;
 for(const profileId of profiles){for(const caseId of cases){
  const meta={profileId,caseId},rows=grouped.get(profileId+'::'+caseId)||[];
  if(rows.length!==1){add(rows.length?'DUPLICATE_ACCEPTANCE_EVIDENCE':'MISSING_ACCEPTANCE_EVIDENCE',meta);continue;}
  const e=rows[0];
  if(e.status!=='PASS'||e.testsExecuted!==true||e.skipped!==0||e.failures!==0||e.errors!==0||!Number.isInteger(e.assertions)||e.assertions<1){add('MANDATORY_ACCEPTANCE_NOT_PASSED',meta);continue;}
  if(!platformValid||e.platformId!==platformProfile.id||e.platformProfileSha256!==platformProfileSha256){add('EVIDENCE_IS_FOR_DIFFERENT_PLATFORM',meta);continue;}
  if(e.artifactSha256!==artifactSha256||e.sourceCommit!==sourceCommit||e.policySha256!==policySha256){add('EVIDENCE_IS_FOR_DIFFERENT_ARTIFACT_SOURCE_OR_POLICY',meta);continue;}
  if(e.executionKind!=='PHYSICAL_DEVICE'&&!(artifactCases.has(caseId)&&e.executionKind==='ARTIFACT_INSPECTION')){add('UNIT_OR_FIXTURE_IS_NOT_DEVICE_ACCEPTANCE',meta);continue;}
  if(!HASH.test(e.evidenceSha256||'')){add('EVIDENCE_HASH_REQUIRED',meta);continue;}
  let bytes;try{bytes=safeEvidence(evidenceRoot,e.evidencePath);}catch{add('EVIDENCE_FILE_UNAVAILABLE_OR_UNSAFE',meta);continue;}
  if(digest(bytes)!==e.evidenceSha256){add('EVIDENCE_HASH_MISMATCH',meta);continue;}
  if(caseId==='digital-brain/new-file-record-within-180s'){
   let timing;try{timing=evaluateFreshnessTrace(JSON.parse(bytes.toString('utf8')).changes);}catch{timing={status:'BLOCKED'}}
   if(timing.status!=='PASS'){add('FILE_FRESHNESS_ACCEPTANCE_NOT_MET',meta);continue;}
  }
  if(typeof verifyEvidence!=='function')continue;
  try{
   // Authority is injected by the existing release/Veritas harness, NEVER loaded from a dossier module path.
   if(await verifyEvidence({record:structuredClone(e),bytes:Buffer.from(bytes),artifactSha256,sourceCommit,policySha256,platformId:platformProfile.id,platformProfileSha256})!==true){add('VERITAS_EVIDENCE_REJECTED',meta);continue;}
  }catch{add('VERITAS_EVIDENCE_VERIFICATION_FAILED',meta);continue;}
  verified++;
 }}
 return{schemaVersion:'leeway.golden-system-evidence-assessment.v1',status:blockers.length?'BLOCKED':'EVIDENCE_QUALIFIED_NOT_DISTRIBUTED',requiredCaseCount:cases.length,profileCount:profiles.length,requiredEvidenceCount:cases.length*profiles.length,verifiedEvidenceCount:verified,blockers,artifactSha256,sourceCommit,policySha256,platformId:platformValid?platformProfile.id:null,platformProfileSha256,releasePromoted:false,installed:false};
}
async function cli(){
 const args=process.argv.slice(2),values=new Map();for(let i=0;i<args.length;i+=2){if(!args[i]?.startsWith('--')||!args[i+1]||values.has(args[i]))throw Error('GOLDEN_GATE_ARGUMENTS_INVALID');values.set(args[i],args[i+1]);}
 for(const key of values.keys())if(!['--artifact','--platform-profile','--source-commit','--profiles','--dossier','--evidence-root','--out'].includes(key))throw Error('GOLDEN_GATE_ARGUMENT_UNKNOWN');
 const artifact=values.get('--artifact');if(!artifact)throw Error('ARTIFACT_PATH_REQUIRED');
 const {policy,sha256}=loadGoldenPolicy();
 if(!values.has('--platform-profile'))throw Error('PLATFORM_PROFILE_PATH_REQUIRED');
 const {profile:platformProfile,sha256:platformProfileSha256}=loadPlatformProfile(values.get('--platform-profile'));
 const dossier=values.has('--dossier')?JSON.parse(fs.readFileSync(values.get('--dossier'),'utf8').replace(/^\uFEFF/,'')):{evidence:[]};
 const result=await assessGoldenEvidence({policy,policySha256:sha256,platformProfile,platformProfileSha256,artifactSha256:digest(fs.readFileSync(artifact)),sourceCommit:values.get('--source-commit'),expectedProfiles:(values.get('--profiles')||'').split(',').filter(Boolean),dossier,evidenceRoot:values.get('--evidence-root')});
 const text=JSON.stringify(result,null,2)+'\n';if(values.has('--out'))fs.writeFileSync(values.get('--out'),text,{flag:'wx'});
 console.log(text);process.exitCode=result.status==='BLOCKED'?2:0;
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url))cli().catch(e=>{console.error(e.message);process.exitCode=1});

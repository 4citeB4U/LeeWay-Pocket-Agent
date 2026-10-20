/*
REGION: LEEWAY.POCKET.GOLDEN_ENROLLMENT
TAG: CUSTOMER_DEVICE_ADMISSION_BOUNDARY_V1
5WH: WHAT=Fail-closed pure admission validation for owner-approved device enrollment.
WHY=Application ownership and local Continuum body names are not customer authentication.
WHO=LeeWay Golden Package release candidate. WHERE=contracts/customer-device-admission.v1.mjs.
WHEN=2026-10-08. HOW=Validate external authenticated decisions and device proof without issuing credentials.
AUTHORIZED_ROLES: Candidate admission evaluator only; no installation, secrets or mutations. LICENSE: MIT.
*/
import {createHash} from 'node:crypto';
const sha = v=>createHash('sha256').update(JSON.stringify(v)).digest('hex');
const str=v=>typeof v==='string'&&v.length>0&&v.length<=256&&!/[\x00-\x1f]/.test(v);
const obj=v=>v&&typeof v==='object'&&!Array.isArray(v);
export const admissionSchema='leeway.customer-device-admission.v1';
export function evaluateEnrollmentAdmission({request,ownerProof,deviceProof,entitlement,revocation,nowMs=Date.now()}={}){
 const deny=(reason)=>({status:'BLOCKED',reason,authorized:false});
 if(!obj(request)||request.schemaVersion!==admissionSchema||!str(request.ownerId)||!str(request.agentId)||!str(request.deviceId)||!str(request.challengeId)||!str(request.platform))return deny('ENROLLMENT_REQUEST_INVALID');
 if(request.agentId!=='agent-lee')return deny('AGENT_IDENTITY_INVALID');
 if(!obj(ownerProof)||ownerProof.verified!==true||ownerProof.ownerId!==request.ownerId||ownerProof.challengeId!==request.challengeId||ownerProof.issuerAuthorityVerified!==true)return deny('OWNER_AUTHENTICATION_NOT_VERIFIED');
 if(!obj(entitlement)||entitlement.verified!==true||entitlement.ownerId!==request.ownerId||entitlement.active!==true||!str(entitlement.entitlementId)||entitlement.issuerAuthorityVerified!==true)return deny('ENTITLEMENT_NOT_VERIFIED');
 if(!obj(deviceProof)||deviceProof.verified!==true||deviceProof.deviceId!==request.deviceId||deviceProof.challengeId!==request.challengeId||deviceProof.ownerId!==request.ownerId||deviceProof.proofOfPossessionVerified!==true||!str(deviceProof.publicKeyFingerprint))return deny('DEVICE_KEY_PROOF_NOT_VERIFIED');
 if(!obj(revocation)||revocation.authoritativeReadback!==true||revocation.ownerId!==request.ownerId||revocation.deviceId!==request.deviceId||revocation.revoked!==false)return deny('REVOCATION_STATE_UNVERIFIED');
 if(!Number.isSafeInteger(nowMs)||!Number.isSafeInteger(request.expiresAtMs)||request.expiresAtMs<=nowMs||request.expiresAtMs>nowMs+300000)return deny('ENROLLMENT_REQUEST_EXPIRED_OR_UNBOUNDED');
 if(request.challengeConsumed!==false||request.approvalCommitted!==true||request.platformCompatible!==true)return deny('CHALLENGE_OR_PLATFORM_NOT_ADMITTED');
 const payload={schemaVersion:admissionSchema,ownerId:request.ownerId,agentId:request.agentId,deviceId:request.deviceId,platform:request.platform,entitlementId:entitlement.entitlementId,publicKeyFingerprint:deviceProof.publicKeyFingerprint,challengeId:request.challengeId,expiresAtMs:request.expiresAtMs};
 return {status:'CANDIDATE_ADMISSION_READY',authorized:false,reason:'AUTHORITY_REDEMPTION_AND_DURABLE_CONSUMPTION_REQUIRED',candidateSha256:sha(payload),payload};
}

import fs from "node:fs";
import assert from "node:assert/strict";

const root="app/src/main";
const main=fs.readFileSync(root+"/java/industries/leeway/pocket/MainActivity.kt","utf8");
const voice=fs.readFileSync(root+"/java/industries/leeway/pocket/PocketVoiceActivity.kt","utf8");
const overlay=fs.readFileSync(root+"/java/industries/leeway/pocket/PocketOverlayService.kt","utf8");
const bridge=fs.readFileSync(root+"/java/industries/leeway/pocket/DeviceBridgeClient.kt","utf8");
const bindings=fs.readFileSync(root+"/java/industries/leeway/pocket/EcosystemBindings.kt","utf8");
const manifest=fs.readFileSync(root+"/AndroidManifest.xml","utf8");
const bindingJson=JSON.parse(fs.readFileSync("docs/ecosystem-bindings.json","utf8"));

assert.doesNotMatch(main,/TextToSpeech/);
assert.doesNotMatch(voice,/TextToSpeech/);
assert.doesNotMatch(voice,/speechSynthesis/);
assert.match(voice,/SpeechRecognizer/);
assert.match(voice,/LeeWayAndroidVoice/);
assert.match(voice,/agent\.chat/);
assert.match(voice,/canonicalFormulaState/);
assert.match(voice,/DeviceBridgeClient/);
assert.match(overlay,/TYPE_APPLICATION_OVERLAY/);
assert.match(overlay,/PocketVoiceActivity/);
assert.match(bridge,/PocketBridgeActivity/);
assert.match(bridge,/POCKET_BOOTSTRAP/);
assert.match(bindings,/ecosystem-bindings\.json/);
assert.match(manifest,/SYSTEM_ALERT_WINDOW/);
assert.match(manifest,/PocketOverlayService/);
assert.match(manifest,/PocketBootReceiver/);
assert.equal(bindingJson.bindings.find(x=>x.id==="voice-fabric").voicePackageId,"agent-lee-voice-one");
assert.equal(bindingJson.bindings.find(x=>x.id==="formula").executionClaim,"NOT_EXECUTED_UNLESS_RECEIPT_RETURNED");
assert.match(bindingJson.bindings.find(x=>x.id==="agent-skills").phoneBinding,/REMOTE_MCP_PENDING/);

console.log("PASS Pocket secondary-workstation source contract");

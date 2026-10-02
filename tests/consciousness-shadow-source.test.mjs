import fs from "node:fs";
import assert from "node:assert/strict";

const root="app/src/main";
const shadow=fs.readFileSync(root+"/java/industries/leeway/pocket/ConsciousnessShadow.kt","utf8");
const voice=fs.readFileSync(root+"/java/industries/leeway/pocket/PocketVoiceActivity.kt","utf8");
const main=fs.readFileSync(root+"/java/industries/leeway/pocket/MainActivity.kt","utf8");
const manifest=fs.readFileSync(root+"/AndroidManifest.xml","utf8");
const network=fs.readFileSync(root+"/res/xml/network_security_config.xml","utf8");

assert.match(shadow,/http:\/\/localhost:8789\/api\/ask/);
assert.match(shadow,/L1_OBSERVE_ONLY/);
assert.match(shadow,/answerEffect",false/);
assert.match(shadow,/connectTimeout=800/);
assert.match(shadow,/readTimeout=1200/);
assert.match(shadow,/requestHash/);
assert.match(shadow,/recordActual/);
assert.match(shadow,/shadowAnswer/);
assert.doesNotMatch(shadow,/runtime\/formula\/v1\/evaluate/);

assert.match(voice,/ConsciousnessShadow\.observe\(applicationContext,request\)/);
assert.match(voice,/ConsciousnessShadow\.recordActual\(applicationContext,pendingRequest\.orEmpty\(\),response,true\)/);
assert.match(voice,/put\("prompt",request\.take\(600\)\)/);
assert.doesNotMatch(voice,/put\("prompt",[^\n]*shadow/i);
assert.match(main,/Consciousness shadow \(L1\)/);

assert.match(manifest,/networkSecurityConfig="@xml\/network_security_config"/);
assert.match(manifest,/usesCleartextTraffic="false"/);
assert.match(network,/base-config cleartextTrafficPermitted="false"/);
assert.match(network,/<domain includeSubdomains="false">localhost<\/domain>/);
assert.doesNotMatch(network,/<base-config cleartextTrafficPermitted="true"/);

console.log("PASS Pocket L1 consciousness shadow keeps zero answer authority");
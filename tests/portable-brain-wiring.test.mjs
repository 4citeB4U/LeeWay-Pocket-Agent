/* REGION: LEEWAY.BRAIN.QUALIFICATION; TAG: PORTABLE_CORE_WIRING
WHO: LeeWay; WHAT: Check compile inputs and no reintroduction of rejected implementation.
WHEN: Source qualification; WHERE: existing repository; WHY: A new file without callers is not a repair.
HOW: Inspect active sources and Gradle linkage; behavioral proof is the separate Kotlin suite.
LICENSE: MIT */
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const read=p=>fs.readFileSync(new URL('../'+p,import.meta.url),'utf8');
const main='app/src/main/java/industries/leeway/pocket/';
test('portable core has no native platform or LLM imports',()=>{
 const core=read('brain-core/src/main/kotlin/industries/leeway/brain/DigitalBrain.kt');
 assert.doesNotMatch(core,/^import\s+(android|androidx|java\.|kotlinx\.coroutines|com\.)/m);
 assert.doesNotMatch(core,/phone-fold6|FoldDigitalBrain|System\.getenv|openai|gemini|ollama/i);
 assert.ok(core.includes('interface BrainStore'));
});
test('existing Android entrypoint actually invokes the portable core adapter',()=>{
 assert.ok(read('settings.gradle.kts').includes('include(":brain-core")'));
 assert.ok(read('app/build.gradle.kts').includes('implementation(project(":brain-core"))'));
 assert.ok(read(main+'MainActivity.kt').includes('AndroidDigitalBrainAdapter.bootstrap(this)'));
 assert.ok(read(main+'AndroidDigitalBrainAdapter.kt').includes('DigitalBrain.bootstrap('));
});
test('fixed-body implementation and Android-only master policy are absent',()=>{
 assert.equal(fs.existsSync(new URL('../'+main+'FoldDigitalBrain.kt',import.meta.url)),false);
 assert.equal(fs.existsSync(new URL('../contracts/golden-apk-release.v1.json',import.meta.url)),false);
 for(const file of ['MainActivity.kt','PocketVoiceActivity.kt','LeeWayAutomation.kt','LeeWayDeviceService.kt','LeeWayBodyDatabases.kt','AndroidDigitalBrainAdapter.kt'])assert.doesNotMatch(read(main+file),/phone-fold6|FoldDigitalBrain|Fold6/);
});
test('native identity is sourced from internal Devices and mappings stay in Brain',()=>{
 assert.ok(read(main+'AndroidDigitalBrainAdapter.kt').includes('devices.DeviceIdentity'));
 assert.ok(read(main+'LeeWayBodyDatabases.kt').includes('brain_resource_bindings'));
 assert.ok(read(main+'devices/DeviceIdentity.kt').includes('AndroidKeyStore'));
 assert.ok(read(main+'devices/DeviceIdentity.kt').includes('UUID.randomUUID()'));
});
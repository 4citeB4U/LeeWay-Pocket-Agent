import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
const root=path.resolve(import.meta.dirname,'../app/src/main/assets/agent-vt');
const lock=JSON.parse(fs.readFileSync(path.join(root,'SOURCE.json'),'utf8'));
const gradle=fs.readFileSync(path.resolve(import.meta.dirname,'../app/build.gradle.kts'),'utf8');
test('Current Fold6 v43 package retained, not downgraded',()=>{
 assert.match(gradle,/versionCode = 43/);
 assert.match(gradle,/1\.0\.0-continuum-curved-rc16/);
});
test('Every Continuum resource has exact pinned bytes',()=>{
 const names=['index.html','continuum.css','continuum-app.mjs','continuum-sphere.mjs','continuum-depth.mjs','continuum-media.mjs','mammoth.browser.min.js','purify.min.js'];
 for(const name of names){
  const data=fs.readFileSync(path.join(root,'continuum',name));
  const expected=lock.resources['continuum/'+name];
  assert.ok(expected,name+' missing');
  assert.equal(data.length,expected.bytes,name+' length');
  assert.equal(crypto.createHash('sha256').update(data).digest('hex'),expected.sha256,name+' hash');
 }
});
test('Android owner-bound WebView keeps only local Continuum API and assets',()=>{
 const kt=fs.readFileSync(path.resolve(import.meta.dirname,'../app/src/main/java/industries/leeway/pocket/AgentTabletActivity.kt'),'utf8');
 assert.match(kt,/AndroidContinuumReadAdapter\.response/);
 assert.match(kt,/readVerifiedResource/);
 assert.match(kt,/media-src 'self' blob:/);
 assert.match(kt,/frame-src 'self' blob:/);
 assert.doesNotMatch(kt,/setAllowUniversalAccessFromFileURLs\(true\)/);
});
test('Bundled app uses the existing independent native owner DB',()=>{
 const kt=fs.readFileSync(path.resolve(import.meta.dirname,'../app/src/main/java/industries/leeway/pocket/AndroidContinuumReadAdapter.kt'),'utf8');
 assert.match(kt,/SQLiteDatabase\.OPEN_READONLY/);
 assert.match(kt,/CONTINUUM_BRAIN_OWNER_MISMATCH/);
 assert.match(kt,/CONTINUUM_VIEW_PROVIDER_UNBOUND/);
});

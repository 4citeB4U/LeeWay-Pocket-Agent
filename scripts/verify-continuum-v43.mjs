import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
const root=path.resolve(import.meta.dirname,'../app/src/main/assets/agent-vt');
const lock=JSON.parse(fs.readFileSync(path.join(root,'SOURCE.json'),'utf8'));
const gradle=fs.readFileSync(path.resolve(import.meta.dirname,'../app/build.gradle.kts'),'utf8');
test('Current Fold6 v44 package upgrades without replacing the v43 Continuum assets',()=>{
 assert.match(gradle,/versionCode = 44/);
 assert.match(gradle,/1\.0\.0-continuum-owner-views-rc17/);
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

test('Fold6 Knowledge and Devices read existing owner Brain nodes without creating or copying them',()=>{
 const kt=fs.readFileSync(path.resolve(import.meta.dirname,'../app/src/main/java/industries/leeway/pocket/AndroidContinuumReadAdapter.kt'),'utf8');
 for(const check of ['brainPredicate(view:String)','brainCount(bound:Bound,view:String)','brainRecords(bound:Bound','brainDescriptor(bound:Bound','SQLiteDatabase.OPEN_READONLY','CONTINUUM_BRAIN_OWNER_MISMATCH','CONTINUUM_RECORD_OUTSIDE_BODY','INDEX_REFERENCE','originalFileVerified', 'CONTINUUM_CURSOR_SCOPE_INVALID']){
   assert.ok(kt.includes(check),check);
 }
 assert.match(kt,/sourceView=="knowledge"\|\|sourceView=="devices"/);
 assert.match(kt,/val bound = Bound\(db, brain, identity, selected\)/);
 assert.doesNotMatch(kt,/android\.permission\.MANAGE_EXTERNAL_STORAGE/);
});
test('Existing phone retains exact asset hashes while native source views become readable',()=>{
 const kt=fs.readFileSync(path.resolve(import.meta.dirname,'../app/src/main/java/industries/leeway/pocket/AndroidContinuumReadAdapter.kt'),'utf8');
 assert.match(kt,/json\(200,brainDescriptor\(bound,id\)\.first\)/);
 assert.match(kt,/brainContent\(bound,id\)/);
 assert.match(kt,/retrievalMode","OWNER_LOCAL_SQLITE_SOURCE_ID_KEYSET"/);
});

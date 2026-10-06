/*
REGION: LEEWAY.BRAIN.VIEWER.QUALIFICATION
TAG: LOCAL_VIEWER_PROTOCOL_AND_LINEAGE_TESTS
WHO: LeeWay qualification; WHAT: Execute actual binding functions against explicit fixture envelopes.
WHEN: Before native acceptance; WHERE: Node VM, no device/data authority is implied.
WHY: A matching UI must still reject wrong-body, stale, malformed and unavailable sources.
HOW: Bounded fixture protocol, exact original-script/dependency hashes and no release promotion.
LICENSE: MIT
*/
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import {createHash} from 'node:crypto';
const root=new URL('../',import.meta.url);
const text=p=>fs.readFileSync(new URL(p,root),'utf8').replace(/^\uFEFF/,'');
const hash=b=>createHash('sha256').update(b).digest('hex').toUpperCase();
const script=text('app/src/main/assets/digital-brain/local-brain-binding.js');
const html=text('app/src/main/assets/digital-brain/brain.html');
const body='unit-viewer-fixture',rootId='brain:'+body;
const node=(id=rootId,parent=null)=>({id,parent_id:parent,label:id,type:'universe',status:'observed',child_count:0,metadata:{TEST_FIXTURE:true}});
function fixture(override){
 const mutations=[];const status={textContent:'',style:{}};const context=vm.createContext({console,JSON,Map,Set,Promise,Number,Error,Object,
  document:{getElementById:id=>id==='liveStatus'?status:null},window:{},Event:class{}});
 const envelope=(result,extra={})=>JSON.stringify({ok:true,result,source:'OWNER_LOCAL_BRAIN_SQLITE',bodyId:body,TEST_FIXTURE_NOT_DEVICE_EVIDENCE:true,...extra});
 const defaultQuery=(op,a)=>op==='root'?envelope({node:node()}):op==='children'?envelope({node:node(a.id,a.id===rootId?null:rootId),nodes:[],offset:a.offset,total:0,hasMore:false}):envelope({nodes:[]});
 context.window.LeeWayBrainView={query:(op,raw)=>override?override(op,JSON.parse(raw),defaultQuery,envelope):defaultQuery(op,JSON.parse(raw))};
 vm.runInContext(script,context);
 const renderer={upsert:n=>mutations.push(['node',n.id]),children:(...a)=>mutations.push(['children',...a]),enter:id=>mutations.push(['enter',id]),root:id=>mutations.push(['root',id]),select(){},relationships(){}};
 const binding=context.window.LeeWayLocalBrainBinding(renderer);
 return {binding,context,mutations,status,envelope};
}
test('original viewer classic scripts parse with no rendering emulation',()=>{
 let count=0;for(const [,attrs,code] of html.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script>/gi)){
  if(/\bsrc=|\btype=["'](?:module|importmap|application\/json)/i.test(attrs))continue;new vm.Script(code);count++;
 }assert.ok(count>=2);new vm.Script(script);
});
test('recovered original 3D script is retained byte-for-byte within the adapter extension',()=>{
 const lock=JSON.parse(text('contracts/original-brain-viewer-projection.v1.json'));
 const module=html.match(/<script type="module" id="brainUniverseRuntime">([\s\S]*?)<\/script>/)[1];
 const end=module.indexOf("\nwindow.addEventListener('leeway-enter-local-brain'");assert.ok(end>0);
 const recovered=module.slice(1,end);assert.equal(hash(Buffer.from(recovered)),lock.original3DScriptSha256);
});
test('original nucleus image and offline dependencies match their inspected hashes',()=>{
 const lock=JSON.parse(text('contracts/original-brain-viewer-projection.v1.json'));
 assert.equal(hash(fs.readFileSync(new URL('app/src/main/assets/digital-brain/nucleus.png',root))),lock.originalNucleusSha256);
 const dependencies=JSON.parse(text('contracts/original-brain-viewer-dependencies.v1.json'));
 for(const d of dependencies.dependencies)assert.equal(hash(fs.readFileSync(new URL('app/src/main/assets/digital-brain/'+d.target,root))),d.sha256);
});
test('customer viewer contains no embedded workstation graph or retired synthetic demos',()=>{
 assert.ok(html.includes('const DATA={nodes:Object.create(null),children:Object.create(null),rels:Object.create(null)};'));
 for(const value of ['FALLBACK_D_TOP_LEVEL','WD 8TB Diagnostics','lw22-demo-runtime','STAGED_FILE_FLOW','phone-fold6'])assert.equal(html.includes(value),false,value);
 assert.ok(html.includes("connect-src 'none'"));assert.equal(/"three":"https?:/.test(html),false);
});
test('root and child data flow through the real native binding interface',async()=>{const f=fixture();const r=await f.binding.start();assert.equal(r.rootId,rootId);assert.equal(f.binding.getActive().id,rootId);assert.ok(f.mutations.some(r=>r[0]==='enter'))});
test('missing native bridge fails rather than showing another installation',async()=>{const f=fixture();delete f.context.window.LeeWayBrainView;vm.runInContext(script,f.context);const b=f.context.window.LeeWayLocalBrainBinding({});await assert.rejects(()=>b.start(),/LOCAL_BRAIN_PROVIDER_UNBOUND/)});
test('unknown or untrusted data source cannot initialize viewer',async()=>{const f=fixture((op,a,d,e)=>e({node:node()},{source:'REMOTE_OR_UNKNOWN'}));await assert.rejects(()=>f.binding.start(),/VIEWER_SOURCE_REJECTED/);assert.equal(f.mutations.length,0)});
test('wrong-body root is rejected before rendering',async()=>{const f=fixture((op,a,d,e)=>e({node:node('brain:other')}));await assert.rejects(()=>f.binding.start(),/VIEWER_ROOT_IDENTITY_MISMATCH/);assert.equal(f.mutations.length,0)});
test('body identity cannot switch between root and children',async()=>{const f=fixture((op,a,d,e)=>op==='children'?e({node:node(),nodes:[],offset:0,total:0,hasMore:false},{bodyId:'other'}):d(op,a));await assert.rejects(()=>f.binding.start(),/VIEWER_BODY_CHANGED/);assert.equal(f.mutations.some(x=>x[0]==='children'),false)});
test('relationship neighbors cannot be admitted as child records',async()=>{const f=fixture((op,a,d,e)=>op==='children'?e({node:node(),nodes:[node(rootId+':child',rootId+':other')],offset:0,total:1,hasMore:false}):d(op,a));await assert.rejects(()=>f.binding.start(),/VIEWER_RELATION_IS_NOT_CHILD/)});
test('duplicate child records cannot inflate a page',async()=>{const child=node(rootId+':child',rootId);const f=fixture((op,a,d,e)=>op==='children'?e({node:node(),nodes:[child,child],offset:0,total:2,hasMore:false}):d(op,a));await assert.rejects(()=>f.binding.start(),/VIEWER_RELATION_IS_NOT_CHILD/)});
test('incorrect pagination claims fail closed',async()=>{const f=fixture((op,a,d,e)=>op==='children'?e({node:node(),nodes:[],offset:0,total:0,hasMore:true}):d(op,a));await assert.rejects(()=>f.binding.start(),/VIEWER_PAGE_COUNT_MISMATCH/)});
test('native query refusal is surfaced without synthetic fallback',async()=>{const f=fixture(()=>JSON.stringify({ok:false,error:'OWNER_REVOKED'}));await assert.rejects(()=>f.binding.start(),/OWNER_REVOKED/);assert.equal(f.mutations.length,0)});
test('superseded asynchronous page response cannot replace current navigation',async()=>{
 let release;
 const f=fixture((op,a,d)=>op==='children'&&a.id.endsWith(':slow')?new Promise(resolve=>release=()=>resolve(d(op,a))):d(op,a));
 await f.binding.start();const pending=f.binding.load(rootId+':slow');await f.binding.load(rootId+':current');release();await pending;
 assert.equal(f.binding.getActive().id,rootId+':current');assert.equal(f.mutations.some(x=>x[0]==='enter'&&x[1].endsWith(':slow')),false);
});
test('native viewer is non-exported and does not pause unrelated Voice WebViews',()=>{
 const manifest=text('app/src/main/AndroidManifest.xml'),activity=text('app/src/main/java/industries/leeway/pocket/DigitalBrainActivity.kt');
 assert.match(manifest,/<activity android:name="\.DigitalBrainActivity" android:exported="false"/);
 assert.ok(activity.includes('settings.allowFileAccess=false;settings.allowContentAccess=false'));
 assert.ok(activity.includes('name !in ALLOWED'));assert.equal(activity.includes('pauseTimers('),false);
 const sphere=text('app/src/main/assets/agent_lee_sphere_transparent.html');
 const fn=sphere.slice(sphere.indexOf('    function openDigitalBrain() {'),sphere.indexOf('    function openWorkstation()'));
 assert.ok(fn.includes('window.LeeWayAndroid.openDigitalBrain()'));assert.equal(fn.includes('document.body.innerHTML'),false);
});

// Execute the actual Android adapter SQL on host SQLite as an extra query-semantics check.
// This does not execute Android SQLiteOpenHelper, Keystore or the native JavaScript bridge.
import {DatabaseSync} from 'node:sqlite';
function queryFixture(t){
 const source=text('app/src/main/java/industries/leeway/pocket/AndroidBrainViewer.kt');
 const columns=source.match(/private val columns="([^"]+)"/)[1],active=source.match(/private val active="([^"]+)"/)[1];
 const children=source.match(/override fun children[^\n]+rows\("([^"]+)"/)[1].replace('$columns',columns).replace('$active',active);
 const rank=source.match(/override fun childIndex[^\n]+rawQuery\("([^"]+)"/)[1];
 const search=source.match(/override fun search[\s\S]*?rows\("([^"]+)"/)[1].replace('$columns',columns).replace('$active',active);
 const db=new DatabaseSync(':memory:');t.after(()=>db.close());
 db.exec('CREATE TABLE nodes(id TEXT PRIMARY KEY,parent_id TEXT,title TEXT,type TEXT,status TEXT,metadata_json TEXT,source_path TEXT)');
 const insert=db.prepare('INSERT INTO nodes VALUES (?,?,?,?,?,?,?)');
 const parent='brain:sql-fixture:parent';
 const add=(id,title,status='observed',p=parent)=>insert.run(id,p,title,'file',status,'{}',null);
 return{db,parent,add,children:db.prepare(children),rank:db.prepare(rank),search:db.prepare(search)};
}
test('actual SQL sibling rank matches paged child ordering, including null and equal-case titles',t=>{
 const f=queryFixture(t);for(const [id,title] of [['z',null],['b','alpha'],['a','Alpha'],['c','Beta']])f.add('brain:sql-fixture:'+id,title);
 f.add('brain:sql-fixture:gone','Aardvark','tombstoned');
 const rows=f.children.all(f.parent,128,0);assert.equal(rows.length,4);
 rows.forEach((row,index)=>assert.equal(Object.values(f.rank.get(row.id,f.parent))[0],index));
 assert.deepEqual(rows.map(x=>x.title),[null,'Alpha','alpha','Beta']);
});
test('actual SQL paginates more than 128 children without returning another parent',t=>{
 const f=queryFixture(t);for(let i=0;i<258;i++)f.add('brain:sql-fixture:'+i,'item-'+String(i).padStart(3,'0'));
 f.add('brain:other:item','item-000','observed','brain:other:parent');
 const rows=f.children.all(f.parent,128,256);assert.equal(rows.length,2);
 assert.equal(Object.values(f.rank.get(rows[0].id,f.parent))[0],256);
 assert.ok(rows.every(r=>r.parent_id===f.parent));
});
test('actual SQL search treats quotes as data and restricts results to the selected body',t=>{
 const f=queryFixture(t);f.add('brain:sql-fixture:quote',"Owner's record");f.add('brain:other:quote',"Owner's record",'observed','brain:other:parent');
 const own='brain:sql-fixture',prefix=own+':';
 const rows=f.search.all(own,prefix.length,prefix,"Owner's",32);assert.equal(rows.length,1);assert.equal(rows[0].id,'brain:sql-fixture:quote');
 assert.equal(f.search.all(own,prefix.length,prefix,"' OR 1=1 --",32).length,0);
});

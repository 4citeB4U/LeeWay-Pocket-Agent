/* REGION: LEEWAY.BRAIN.NATIVE_VIEW; TAG: EXISTING_PC_BRAIN_AND_DEVICE_ADAPTER
WHO: Owner-local Agent Lee. WHAT: Expose the existing recovered Brain through the shared viewer protocol.
WHEN: Actual installed Brain/Diagnostics buttons. WHERE: Existing carrier, no new service or Brain.
WHY: Event-only buttons cannot display records. HOW: Same-origin read routes, existing SQLite, native telemetry.
Physical resources come from the existing device binding's Brain manifest, never compiled drive assumptions.
LICENSE: MIT */
import fs from 'node:fs';import path from 'node:path';import os from 'node:os';import {fileURLToPath} from 'node:url';
import {DatabaseSync} from 'node:sqlite';import {execFile} from 'node:child_process';
const here=path.dirname(fileURLToPath(import.meta.url));let current=null;
function resolve(){
 const binding=JSON.parse(fs.readFileSync(new URL('../../host-commander.binding.v1.json',import.meta.url),'utf8').replace(/^\uFEFF/,''));
 if(!binding.brainManifest)throw Error('PC_BRAIN_MANIFEST_UNBOUND');
 const manifest=JSON.parse(fs.readFileSync(binding.brainManifest,'utf8').replace(/^\uFEFF/,''));
 const database=manifest.bodyLocal?.digitalBrainDatabase;
 if(!database||!fs.existsSync(database)||!manifest.bodyId)throw Error('PC_BRAIN_DATABASE_UNBOUND');
 if(current){if(current.database!==database||current.bodyId!==manifest.bodyId)throw Error('PC_BRAIN_BINDING_CHANGED_RESTART_REQUIRED');return current;}
 const db=new DatabaseSync(database);db.exec('PRAGMA busy_timeout=3000');
 const roots=db.prepare('SELECT id FROM nodes WHERE parent_id IS NULL').all().map(x=>x.id);
 if(!roots.includes('scope::hemi::L')||!roots.includes('scope::hemi::R'))throw Error('RECOVERED_BRAIN_ROOT_CONTRACT_MISMATCH');
 current={db,database,bodyId:manifest.bodyId,root:'brain:'+manifest.bodyId,lastSample:0};return current;
}
function ids(c){const hardware='device-current:'+c.bodyId+':hardware';return{hardware,to:(id)=>id==='scope::hemi::L'?c.root+':system':id==='scope::hemi::R'?c.root+':user':id===hardware?c.root+':system:hardware':id.startsWith(hardware+':')?c.root+':system:hardware:'+id.slice(hardware.length+1):c.root+':record:'+encodeURIComponent(id),from:(id)=>{
 if(id===c.root+':system')return'scope::hemi::L';if(id===c.root+':user')return'scope::hemi::R';if(id===c.root+':system:hardware')return hardware;if(id.startsWith(c.root+':system:hardware:'))return hardware+':'+id.slice((c.root+':system:hardware:').length);
 if(!id.startsWith(c.root+':record:')||id.length>2048)throw Error('PC_BRAIN_NODE_OUTSIDE_BODY');return decodeURIComponent(id.slice((c.root+':record:').length));}};}
async function sample(c){
 if(Date.now()-c.lastSample<5000)return;const at=Date.now();
 const metrics={cpu:{state:'OBSERVED',source:'node:os.cpus',logicalProcessors:os.cpus().length,model:os.cpus()[0]?.model,architecture:os.arch(),loadAverages:os.loadavg(),loadNote:'OS-reported; Windows load averages are not CPU utilization'},memory:{state:'OBSERVED',source:'node:os',totalBytes:os.totalmem(),availableBytes:os.freemem(),usedBytesDerived:os.totalmem()-os.freemem()},system:{state:'OBSERVED',source:'node:os',platform:os.platform(),release:os.release(),uptimeSeconds:os.uptime()}};
 if(process.platform==='win32'){
  try{const data=await new Promise((yes,no)=>execFile('powershell.exe',['-NoProfile','-File',path.join(here,'Read-DeviceDiagnostics.ps1')],{windowsHide:true,timeout:12000,maxBuffer:256000},(e,o)=>e?no(e):yes(o)));Object.assign(metrics,JSON.parse(String(data).replace(/^\uFEFF/,'')));}
  catch(e){metrics.nativeHardware={state:'UNAVAILABLE',reason:e.code||e.name};}
 }
 const {hardware}=ids(c);const insert=c.db.prepare('INSERT INTO nodes(id,parent_id,type,title,domain,status,metadata_json,created_at) VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET metadata_json=excluded.metadata_json,status=excluded.status,title=excluded.title');
 c.db.exec('BEGIN IMMEDIATE');try{
  insert.run(hardware,'scope::hemi::L','universe','DEVICE DIAGNOSTICS','system','observed',JSON.stringify({capturedAtMs:at,source:'LOCAL_NATIVE_DEVICE_ADAPTER',scope:'OBSERVATIONS_NOT_FULL_ELECTRICAL_TEST'}),new Date(at).toISOString());
  for(const [name,data]of Object.entries(metrics))insert.run(hardware+':'+name,hardware,'hardware-component',name,'system',data.state||'observed',JSON.stringify({...data,capturedAtMs:at}),new Date(at).toISOString());
  c.db.exec('COMMIT');c.lastSample=at;
 }catch(e){c.db.exec('ROLLBACK');throw e;}
}
function createQuery(c){const map=ids(c);const alive="COALESCE(status,'')!='tombstoned'";
 function row(id){return c.db.prepare('SELECT * FROM nodes WHERE id=? AND '+alive).get(id);}
 function encode(n){if(!n)throw Error('VIEWER_NODE_NOT_FOUND');const live=n.id===map.hardware||n.id.startsWith(map.hardware+':');const count=c.db.prepare('SELECT COUNT(*) n FROM nodes WHERE parent_id=? AND '+alive).get(n.id).n;
  return{id:map.to(n.id),parent_id:n.parent_id?map.to(n.parent_id):c.root,label:n.title||n.id,title:n.title||n.id,type:n.type==='scope'?'universe':n.type,status:live?n.status:'RECOVERED_RECORD_NOT_REOBSERVED',child_count:count,expandable:count>0||n.type==='scope'||n.type==='universe',source_path:n.source_path||'',metadata:{...JSON.parse(n.metadata_json||'{}'),sourceRecordId:n.id,observationScope:live?'CURRENT_NATIVE_SAMPLE':'RECOVERED_RECORD_NOT_FRESH_FILESYSTEM_PROOF'}};}
 const rootNode={id:c.root,parent_id:null,title:'Digital Brain',label:'Digital Brain',type:'universe',status:'READ_PROJECTION',child_count:2,expandable:true,metadata:{source:'EXISTING_BRAIN_SQLITE',bodyId:c.bodyId,rootRole:'PRESENTATION_OF_EXISTING_PERSISTED_ROOTS'}};
 function get(id){return id===c.root?rootNode:encode(row(map.from(id)));}
 return(operation,a)=>{
  if(operation==='root')return{node:rootNode,bodyId:c.bodyId};
  if(operation==='children'){
   const offset=a.offset??0,limit=a.limit??128;if(!Number.isInteger(offset)||offset<0||offset>1000000||!Number.isInteger(limit)||limit<1||limit>128)throw Error('VIEWER_PAGE_INVALID');
   const parent=get(a.id);const list=a.id===c.root?['scope::hemi::L','scope::hemi::R'].slice(offset,offset+limit).map(row):c.db.prepare('SELECT * FROM nodes WHERE parent_id=? AND '+alive+' ORDER BY COALESCE(title,\'\') COLLATE NOCASE,id LIMIT ? OFFSET ?').all(map.from(a.id),limit,offset);
   return{node:parent,nodes:list.map(encode),offset,total:parent.child_count,hasMore:offset+list.length<parent.child_count};
  }
  if(operation==='node'){
   const node=get(a.id);const ancestors=[node];let parent=node.parent_id;const seen=new Set([a.id]);while(parent){if(seen.has(parent)||seen.size>128)throw Error('VIEWER_ANCESTRY_INVALID');seen.add(parent);const p=get(parent);ancestors.unshift(p);parent=p.parent_id;}
   let pageOffset=0;if(node.parent_id&&node.parent_id!==c.root){const n=row(map.from(a.id));const rank=c.db.prepare("SELECT COUNT(*) n FROM nodes WHERE parent_id=? AND "+alive+" AND (COALESCE(title,'') COLLATE NOCASE < ? COLLATE NOCASE OR (COALESCE(title,'') COLLATE NOCASE = ? COLLATE NOCASE AND id<?))").get(n.parent_id,n.title||'',n.title||'',n.id).n;pageOffset=Math.floor(rank/128)*128;}
   return{node,ancestors,pageOffset,links:[],provenance:[{kind:'SOURCE_RECORD',description:node.metadata?.observationScope||'READ_PROJECTION',source:node.metadata?.sourceRecordId||c.database}]};
  }
  if(operation==='search'){const q=a.query?.trim();if(typeof q!=='string'||q.length<2||q.length>160)throw Error('VIEWER_SEARCH_QUERY_INVALID');return{nodes:c.db.prepare("SELECT * FROM nodes WHERE "+alive+" AND instr(lower(COALESCE(title,'')),lower(?))>0 ORDER BY title COLLATE NOCASE,id LIMIT 32").all(q).map(encode)};}
  throw Error('VIEWER_OPERATION_NOT_ALLOWED');
 };
}
const allowed=new Set(['brain.html','local-brain-binding.js','nucleus.png','vendor/three.module.js','vendor/OrbitControls.js','vendor/THREE-LICENSE.txt','agent-lee-emblem.png']);
const bridge=`<script>window.LeeWayBrainView={query:async function(operation,argumentsText){const r=await fetch('/brain/query',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({operation,arguments:JSON.parse(argumentsText)})});return JSON.stringify(await r.json())},close:function(){location.assign('/ui')},initialSurface:function(){return new URL(location.href).searchParams.get('surface')==='hardware'?'hardware':'brain'}};</script>`;
export async function handleLocalBrain(req,res,u){
 if(!u.pathname.startsWith('/brain-ui/')&&u.pathname!=='/brain/query')return false;
 const host=req.headers.host||'',origin='http://'+host;const valid=/^(127\.0\.0\.1|localhost):\d+$/.test(host)&&['127.0.0.1','::1','::ffff:127.0.0.1'].includes(req.socket.remoteAddress);
 const json=(status,x)=>{res.writeHead(status,{'content-type':'application/json','cache-control':'no-store','x-content-type-options':'nosniff'});res.end(JSON.stringify(x));};
 if(!valid||(req.headers.origin&&req.headers.origin!==origin)){json(403,{ok:false,error:'LOCAL_OWNER_ORIGIN_REQUIRED'});return true;}
 try{
  if(u.pathname.startsWith('/brain-ui/')&&req.method==='GET'){
   const name=u.pathname.slice('/brain-ui/'.length);if(!allowed.has(name))throw Error('VIEWER_ASSET_NOT_ALLOWED');
   const f=path.join(here,'digital-brain',name);let data=fs.readFileSync(f);if(name==='brain.html')data=Buffer.from(data.toString().replace('<script src="local-brain-binding.js"></script>',bridge+'\n<script src="local-brain-binding.js"></script>').replace("connect-src 'none'","connect-src 'self'"));
   const mime=name.endsWith('.html')?'text/html; charset=utf-8':name.endsWith('.js')?'text/javascript; charset=utf-8':name.endsWith('.png')?'image/png':'text/plain';res.writeHead(200,{'content-type':mime,'cache-control':'no-store','x-content-type-options':'nosniff'});res.end(data);return true;
  }
  if(req.method!=='POST'||!String(req.headers['content-type']).startsWith('application/json')){json(405,{ok:false,error:'VIEWER_JSON_POST_REQUIRED'});return true;}
  let text='';for await(const chunk of req){text+=chunk;if(text.length>8192)throw Error('VIEWER_REQUEST_LIMIT');}
  const input=JSON.parse(text);const c=resolve();if(input.operation==='root'||String(input.arguments?.id).includes(':system:hardware'))await sample(c);
  const result=createQuery(c)(input.operation,input.arguments||{});json(200,{ok:true,result,bodyId:c.bodyId,source:'OWNER_LOCAL_BRAIN_SQLITE',formulaExecution:'NOT_EXECUTED'});
 }catch(e){json(409,{ok:false,error:/^[A-Z_]+$/.test(e.message)?e.message:'LOCAL_BRAIN_QUERY_FAILED'});}
 return true;
}

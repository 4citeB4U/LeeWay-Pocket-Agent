/*
REGION: LEEWAY.BRAIN.BROWSER_QUALIFICATION
TAG: ORIGINAL_VIEWER_REAL_BROWSER_FIXTURE
WHO: Creator-authorized qualification; WHAT: Execute the packaged original renderer in Playwright.
WHEN: Before native phone acceptance; WHERE: temporary loopback test server, synthetic Brain records.
WHY: Parsing source is not rendered 3D or working recursive controls.
HOW: Real Chromium, actual packaged assets and UI clicks; fixture labels prohibit device-proof promotion.
LICENSE: MIT
*/
import fs from 'node:fs';
import path from 'node:path';
import http from 'node:http';
import {fileURLToPath,pathToFileURL} from 'node:url';
import {createHash} from 'node:crypto';
import assert from 'node:assert/strict';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const args=new Map();for(let i=2;i<process.argv.length;i+=2)args.set(process.argv[i],process.argv[i+1]);
const modulePath=args.get('--playwright');const output=args.get('--output');
if(!modulePath||!output)throw Error('PLAYWRIGHT_MODULE_AND_OUTPUT_REQUIRED');
const playwright=await import(pathToFileURL(modulePath).href);
fs.mkdirSync(output,{recursive:true});
const assets=path.join(root,'app/src/main/assets/digital-brain');
const contentTypes={'.html':'text/html','.js':'text/javascript','.png':'image/png','.txt':'text/plain'};
const requests=[];const results=[];const errors=[];let browser;
const server=http.createServer((req,res)=>{
 const url=new URL(req.url,'http://127.0.0.1');const relative=decodeURIComponent(url.pathname.slice(1));
 if(relative.includes('..')||path.isAbsolute(relative)||req.method!=='GET'){res.writeHead(403);res.end();return;}
 const file=path.join(assets,relative||'brain.html');
 if(!fs.existsSync(file)||!fs.statSync(file).isFile()){res.writeHead(404);res.end('not found');return;}
 res.writeHead(200,{'content-type':contentTypes[path.extname(file)]||'application/octet-stream','cache-control':'no-store'});res.end(fs.readFileSync(file));
});
await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
const address='http://127.0.0.1:'+server.address().port+'/brain.html';
function record(name,extra={}){results.push({name,status:'PASS',...extra});}
try {
 browser=await playwright.chromium.launch({channel:args.get('--channel')||'chrome',headless:true,args:['--enable-unsafe-swiftshader','--use-angle=swiftshader']});
 for(const viewport of [{width:1365,height:900,name:'desktop-browser'},{width:390,height:844,name:'phone-sized-browser-NOT-ANDROID'}]){
  const context=await browser.newContext({viewport:{width:viewport.width,height:viewport.height},deviceScaleFactor:1});
  const page=await context.newPage();const pageErrors=[];
  const openNode=async label=>{const item=page.locator('#nodes .node').filter({hasText:label});if(viewport.width<760)await item.click();else await item.dblclick();};
  page.on('pageerror',error=>{pageErrors.push(error.message);errors.push({viewport:viewport.name,error:error.message});});
  page.on('request',request=>requests.push({viewport:viewport.name,url:request.url()}));
  await page.addInitScript(()=>{
   const body='browser-fixture-not-device-proof',root='brain:'+body;
   const data=[];const byId=new Map();window.__viewerFixtureCalls=[];window.__viewerFixtureClose=0;window.__viewerInjection=0;
   function add(id,parent,title,type='universe',metadata={}){const n={id,parent_id:parent,title,label:title,type,status:'observed',metadata,source_path:'',child_count:0,expandable:true,origin:'TEST_FIXTURE'};data.push(n);byId.set(id,n);return n;}
   add(root,null,'Digital Brain · TEST FIXTURE');add(root+':system',root,'SYSTEM UNIVERSE');add(root+':user',root,'USER UNIVERSE');
   add(root+':system:hardware',root+':system','HARDWARE UNIVERSE','universe',{scope:'SIMULATED SENSOR NOT PHYSICAL OBSERVATION'});
   add(root+':system:applications',root+':system','APPLICATION UNIVERSE');add(root+':system:runtime',root+':system','RUNTIME UNIVERSE');
   add(root+':user:files',root+':user','FILESYSTEM UNIVERSE');add(root+':user:files:folder',root+':user:files','Fixture Documents','directory');
   for(let i=0;i<257;i++)add(root+':user:files:folder:file-'+i,root+':user:files:folder','fixture-'+String(i).padStart(3,'0')+'.txt','file',{contentState:'METADATA_ONLY_CONTENT_NOT_READ',testFixture:true});
   add(root+':user:files:folder:unsafe',root+':user:files:folder','<img src=x onerror="window.__viewerInjection=1">','file',{note:'<script>window.__viewerInjection=1</script>'});
   data.forEach(n=>{n.child_count=data.filter(c=>c.parent_id===n.id).length;n.expandable=n.child_count>0||n.type==='universe'||n.type==='directory';});
   function envelope(result){return JSON.stringify({ok:true,result,bodyId:body,source:'OWNER_LOCAL_BRAIN_SQLITE',TEST_FIXTURE_NOT_DEVICE_EVIDENCE:true,formulaExecution:'NOT_EXECUTED'});}
   window.LeeWayBrainView={close(){window.__viewerFixtureClose++;},query(operation,raw){
    const a=JSON.parse(raw);window.__viewerFixtureCalls.push({operation,args:a});
    if(operation==='root')return envelope({node:byId.get(root),bodyId:body});
    if(operation==='children'){const parent=byId.get(a.id);if(!parent)return JSON.stringify({ok:false,error:'VIEWER_NODE_NOT_FOUND'});const children=data.filter(n=>n.parent_id===a.id);return envelope({node:parent,nodes:children.slice(a.offset,a.offset+a.limit),offset:a.offset,total:children.length,hasMore:a.offset+a.limit<children.length});}
    if(operation==='node'){const node=byId.get(a.id);if(!node)return JSON.stringify({ok:false,error:'VIEWER_NODE_NOT_FOUND'});const ancestors=[];let p=node;while(p){ancestors.unshift(p);p=byId.get(p.parent_id);}const siblings=data.filter(n=>n.parent_id===node.parent_id);const pageOffset=node.parent_id?Math.floor(siblings.findIndex(n=>n.id===node.id)/128)*128:0;return envelope({node,ancestors,pageOffset,links:[],provenance:[{kind:'TEST_FIXTURE',description:'Browser behavior only; not phone/database evidence'}]});}
    if(operation==='search')return envelope({nodes:data.filter(n=>n.label.toLowerCase().includes(a.query.toLowerCase())).slice(0,32)});
    return JSON.stringify({ok:false,error:'VIEWER_OPERATION_NOT_ALLOWED'});
   }};
  });
  await page.goto(address,{waitUntil:'networkidle',timeout:30000});
  await page.waitForFunction(()=>window.__leewayLocalBrainReady && window.__leewayOriginalBrain3D?.ready,{timeout:15000});
  const canvas=await page.locator('#brain3DCanvas canvas').count();assert.equal(canvas,1);record(viewport.name+': original 3D renderer initialized');
  await page.evaluate(()=>{const mark=document.createElement('div');mark.textContent='BROWSER FIXTURE — NOT DEVICE ACCEPTANCE';mark.style.cssText='position:fixed;z-index:30000;right:6px;top:6px;padding:3px 5px;color:#ffdf80;background:#07131e;font:9px system-ui;pointer-events:none';document.body.appendChild(mark);});
  assert.equal(await page.evaluate(()=>getComputedStyle(document.body).backgroundColor),'rgba(0, 0, 0, 0)');record(viewport.name+': transparent body surface');
  await page.screenshot({path:path.join(output,viewport.name+'-brain.png'),omitBackground:true});
  await page.locator('#localEnter').click();await page.waitForFunction(()=>document.body.classList.contains('galaxy-active'));
  await openNode('USER UNIVERSE');
  await page.waitForFunction(()=>window.__leewayLocalBrain.getActive()?.id.endsWith(':user'));
  assert.equal(await page.locator('#nodes .node').filter({hasText:'HARDWARE UNIVERSE'}).count(),0);record(viewport.name+': strict child containment');
  await openNode('FILESYSTEM UNIVERSE');
  await page.waitForFunction(()=>window.__leewayLocalBrain.getActive()?.id.endsWith(':user:files'));
  await openNode('Fixture Documents');
  await page.waitForFunction(()=>window.__leewayLocalBrain.getActive()?.total===258);
  assert.equal((await page.evaluate(()=>window.__leewayLocalBrain.getActive())).size,128);record(viewport.name+': recursive directory and bounded page');
  await page.locator('#localNext').click();await page.waitForFunction(()=>window.__leewayLocalBrain.getActive()?.offset===128);
  await page.locator('#localNext').click();await page.waitForFunction(()=>window.__leewayLocalBrain.getActive()?.offset===256);
  assert.equal((await page.evaluate(()=>window.__leewayLocalBrain.getActive())).size,2);record(viewport.name+': complete multi-page navigation without flattening');
  await page.locator('#localSearch').fill('fixture-256');
  await page.locator('#localSearchResults button').filter({hasText:'fixture-256.txt'}).click();
  await page.waitForFunction(()=>document.querySelector('[data-local-brain-evidence]')?.textContent.includes('TEST_FIXTURE'));
  assert.equal(await page.evaluate(()=>window.__viewerInjection),0);assert.equal(await page.locator('#nodes .node.sel').filter({hasText:'fixture-256.txt'}).count(),1);record(viewport.name+': search selects the actual containing page and inspector');
  await page.screenshot({path:path.join(output,viewport.name+'-recursive.png'),omitBackground:true});
  await page.locator('#hud #closeHud').click();
  await page.locator('#backBtn').click();await page.waitForFunction(()=>window.__leewayLocalBrain.getActive()?.id.endsWith(':user:files'));
  await page.locator('#backBtn').click();await page.waitForFunction(()=>window.__leewayLocalBrain.getActive()?.id.endsWith(':user'));record(viewport.name+': back navigation has no history loop');
  await page.locator('#homeBtn').click();await page.waitForFunction(()=>window.__leewayLocalBrain.getActive()?.id===window.__leewayLocalBrainReady.rootId);record(viewport.name+': whole Brain returns to the same local identity');
  await page.locator('#localClose').click();assert.equal(await page.evaluate(()=>window.__viewerFixtureClose),1);record(viewport.name+': return to Agent Lee native boundary');
  assert.deepEqual(pageErrors,[]);record(viewport.name+': no uncaught browser errors');
  const geometry=await page.locator('#brainUniverseRuntime').textContent();assert.ok(geometry.includes('function createBrainHemisphereGeometry(isLeft)'));record(viewport.name+': original sculpted geometry retained');
  await context.close();
 }
 const outside=requests.filter(r=>!r.url.startsWith('http://127.0.0.1:'+server.address().port+'/')&&!r.url.startsWith('data:'));
 assert.deepEqual(outside,[]);record('No external requests from packaged viewer');
} catch(error){errors.push({error:error.stack||String(error)});process.exitCode=1;}
finally {
 await browser?.close();await new Promise(resolve=>server.close(resolve));
 const screenshots=fs.readdirSync(output).filter(n=>n.endsWith('.png')).map(name=>({name,sha256:createHash('sha256').update(fs.readFileSync(path.join(output,name))).digest('hex')}));
 const receipt={schemaVersion:'leeway.original-viewer-browser-qualification.v1',status:errors.length?'FAILED':'BROWSER_FIXTURE_PASS_NOT_DEVICE_ACCEPTANCE',tests:results,errors,requests,screenshots,fixtureRecords:true,androidRuntimeExecuted:false,sqliteRuntimeExecuted:false,phoneInstalled:false,formulaExecuted:false};
 fs.writeFileSync(path.join(output,'browser-receipt.json'),JSON.stringify(receipt,null,2));console.log(JSON.stringify(receipt,null,2));
}

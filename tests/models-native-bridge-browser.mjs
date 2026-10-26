/* Actual packaged HTML/bridge with narrow native API test fixtures.
 * Android compilation, certificate pinning and device playback are separate gates. */
import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {readFile,mkdir,writeFile} from 'node:fs/promises';
import http from 'node:http';
import {createRequire} from 'node:module';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const require=createRequire(import.meta.url);let playwright;
try{playwright=require('playwright');}catch(error){if(!process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES)throw error;playwright=require(path.join(process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES,'playwright'));}
const root=fileURLToPath(new URL('../app/src/main/assets/models-ui/',import.meta.url));
const lock=JSON.parse(await readFile(path.join(root,'SOURCE.json'),'utf8'));
assert.equal(lock.repository,'4citeB4U/Leeway-Runtime-Fabric');
const files={};for(const [name,record]of Object.entries(lock.files)){const bytes=await readFile(path.join(root,name));assert.equal(createHash('sha256').update(bytes).digest('hex'),record.sha256);files[name]=bytes;}
const html=files['index.html'].toString('utf8');
const document={schemaVersion:'leeway.model-inventory.v1',owner:{repository:'4citeB4U/Leeway-Runtime-Fabric',component:'model-execution-runtime',provider:'OLLAMA_LOCAL'},body:{kind:'PC',name:'Paired-PC-fixture'},scope:'PC_MODEL_SERVICE_INVENTORY',state:'OBSERVED',observedAt:'2026-10-07T10:00:00Z',installed:{state:'OBSERVED',count:1,totalSizeBytes:9608350718,models:[{name:'gemma4:e4b',digest:'a'.repeat(64),sizeBytes:9608350718,family:'gemma4',parameterSize:'8.0B',quantization:'Q4_K_M',format:'gguf',reportedCapabilities:['completion'],loaded:false,modifiedAt:'2026-09-27T15:34:11Z'}]},loaded:{state:'OBSERVED',count:0,models:[]}};
const requestLog=[],nativeCalls=[],errors=[],checks=[];
const server=http.createServer((req,res)=>{
  requestLog.push(req.url);
  if(req.url==='/android'){res.writeHead(200,{'content-type':'text/html'});return res.end(html.replace('</head>','<script src="native-models-bridge.js"></script></head>'));}
  if(req.url==='/pc'){res.writeHead(200,{'content-type':'text/html'});return res.end(html);}
  if(req.url==='/native-models-bridge.js'){res.writeHead(200,{'content-type':'application/javascript'});return res.end(files['native-models-bridge.js']);}
  if(req.url==='/api/models/inventory'){res.writeHead(200,{'content-type':'application/json'});return res.end(JSON.stringify(document));}
  res.writeHead(404);res.end();
});
await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
const origin=`http://127.0.0.1:${server.address().port}`;let browser,state='FAIL';
const record=name=>{checks.push({name,state:'PASS'});console.log('PASS',name);};
try{
  browser=await playwright.chromium.launch({headless:true});
  const context=await browser.newContext({viewport:{width:390,height:844}});
  await context.route('**/*',route=>new URL(route.request().url()).origin===origin?route.continue():route.abort());
  const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));
  await page.exposeFunction('fixtureRead',async id=>{nativeCalls.push(id);await page.evaluate(({id,document})=>window.__leewayModelsReply(id,JSON.stringify({status:200,body:document})),{id,document});});
  await page.addInitScript(()=>{window.__closeCount=0;window.LeeWayModels={readInventory(id){window.fixtureRead(id)},close(){window.__closeCount++}};});
  await page.goto(origin+'/android');await page.waitForFunction(()=>document.getElementById('installedCount').textContent==='1');
  assert.equal(await page.locator('#bodyName').textContent(),'PC · Paired-PC-fixture');
  assert.equal(nativeCalls.length,1);assert.ok(/^[0-9]{1,12}$/.test(nativeCalls[0]));
  assert.equal(requestLog.includes('/api/models/inventory'),false,'packaged Android view must use the fixed native pairing route');
  record('Hash-admitted shared HTML reads paired PC inventory through only the native read operation');
  const denied=await page.evaluate(async()=>{
    const attempts=[['/api/models/inventory',{method:'POST',body:'{}'}],['/api/models/inventory?model=other',{}],['/api/models/inventory#other',{}],['/api/generate',{}],['https://example.invalid/api/models/inventory',{}]];
    return await Promise.all(attempts.map(async([url,options])=>(await fetch(url,options)).status));
  });
  assert.deepEqual(denied,[403,403,403,403,403]);assert.equal(nativeCalls.length,1);
  record('Caller-supplied commands, paths, query strings and remote targets never reach the native bridge');
  await page.locator('#backToAgentLee').click();assert.equal(await page.evaluate(()=>window.__closeCount),1);assert.equal(page.url(),origin+'/android');
  record('Android Back control invokes activity close without navigating to another Agent Lee page');
  const pc=await context.newPage();pc.on('pageerror',error=>errors.push(error.message));
  await pc.addInitScript(()=>{window.__closeCount=0;window.pywebview={api:{close:async()=>{window.__closeCount++;return {state:'SURFACE_CLOSED_MAIN_AGENT_RETAINED'}}}};});
  await pc.goto(origin+'/pc');await pc.waitForFunction(()=>document.getElementById('installedCount').textContent==='1');
  await pc.locator('#backToAgentLee').click();assert.equal(await pc.evaluate(()=>window.__closeCount),1);assert.equal(pc.url(),origin+'/pc');
  record('PC Back control invokes the existing native surface close without replacing the main window');
  assert.deepEqual(errors,[]);state='PASS';
}finally{
  await browser?.close();server.closeAllConnections();await new Promise(resolve=>server.close(resolve));
  const directory=process.env.MODELS_NATIVE_BROWSER_ARTIFACTS;
  if(directory){await mkdir(directory,{recursive:true});await writeFile(path.join(directory,'models-native-bridge-proof.json'),JSON.stringify({schemaVersion:'leeway.models-native-bridge-proof.v1',state,scope:'REAL_PACKAGED_HTML_AND_BRIDGE_WITH_NATIVE_METHOD_FIXTURES',checks,nativeReadIds:nativeCalls,requests:requestLog,browserErrors:errors,assetHashes:lock.files,liveAndroidActions:'NOT_PERFORMED'},null,2)+'\n');}
}

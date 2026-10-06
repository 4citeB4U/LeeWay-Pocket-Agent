/* REGION: LEEWAY.DEPLOYMENT.VERIFICATION; TAG: ACTUAL_INSTALLED_UI_NO_FIXTURES
WHO: Owner-authorized engineering. WHAT: Click actual installed Brain/Diagnostics controls.
WHEN: In-place repair acceptance. WHERE: Existing localhost carrier, existing local DB.
WHY: Source and fixture tests do not establish working installed surfaces.
HOW: Playwright existing Chrome, actual native bridge, no substituted responses. LICENSE: MIT. */
import fs from 'node:fs';import path from 'node:path';import {pathToFileURL} from 'node:url';import assert from 'node:assert/strict';import crypto from 'node:crypto';
const args=new Map();for(let i=2;i<process.argv.length;i+=2)args.set(process.argv[i],process.argv[i+1]);
const {chromium}=await import(pathToFileURL(args.get('--playwright')).href);const out=args.get('--out');fs.mkdirSync(out,{recursive:true});
const origin=args.get('--origin');if(!/^http:\/\/127\.0\.0\.1:\d+$/.test(origin))throw Error('LOCAL_CARRIER_REQUIRED');
let browser;const checks=[];const faults=[];const data=[];
function pass(name,evidence={}){checks.push({name,status:'PASS',...evidence});}
try{
 browser=await chromium.launch({channel:'chrome',headless:true,args:['--enable-unsafe-swiftshader','--use-angle=swiftshader']});
 const context=await browser.newContext({viewport:{width:1200,height:850}});const page=await context.newPage();
 page.on('pageerror',e=>faults.push(e.message));
 await page.goto(origin+'/ui',{waitUntil:'networkidle'});await page.waitForFunction(()=>typeof THREE==='object');
 assert.equal(await page.locator('#agent-emblem img').evaluate(el=>el.complete&&el.naturalWidth>0),true);pass('Installed PC page shows the approved emblem');
 const filter0=await page.locator('#agent-emblem img').evaluate(el=>getComputedStyle(el).filter);await page.waitForTimeout(500);const filter1=await page.locator('#agent-emblem img').evaluate(el=>getComputedStyle(el).filter);assert.notEqual(filter0,filter1);pass('Installed PC emblem cycles colors',{filter0,filter1});
 await page.screenshot({path:path.join(out,'pc-agent-lee.png')});
 await page.locator('#hamburger-btn').click();await page.locator('[data-setting="digital-brain"]').click();
 await page.waitForURL(origin+'/brain-ui/brain.html');await page.waitForFunction(()=>window.__leewayLocalBrainReady&&window.__leewayOriginalBrain3D?.ready,null,{timeout:30000});
 const identity=await page.evaluate(()=>window.__leewayLocalBrainReady);assert.equal(identity.rootId,'brain:'+identity.bodyId);pass('Actual hamburger Digital Brain opens original renderer with existing local database',identity);
 assert.equal(await page.locator('#brain3DCanvas canvas').count(),1);await page.screenshot({path:path.join(out,'pc-digital-brain.png')});
 await page.locator('#localClose').click();await page.waitForURL(origin+'/ui');pass('Brain returns to same installed Agent Lee');
 await page.locator('#hamburger-btn').click();await page.locator('[data-setting="diagnostics"]').click();
 await page.waitForURL(origin+'/brain-ui/brain.html?surface=hardware');await page.waitForFunction(()=>window.__leewayLocalBrain?.getActive()?.id.endsWith(':system:hardware')&&document.body.classList.contains('galaxy-active'),null,{timeout:30000});
 pass('Actual Diagnostics button enters physical-device Brain universe');
 const section=await page.evaluate(async()=>JSON.parse(await window.LeeWayBrainView.query('children',JSON.stringify({id:window.__leewayLocalBrain.getActive().id,offset:0,limit:128}))));
 assert.equal(section.ok,true);assert.ok(section.result.nodes.length>=8);assert.ok(section.result.nodes.some(n=>n.label==='motherboard'));
 data.push({bodyId:section.bodyId,source:section.source,hardwareSections:section.result.nodes.map(n=>n.label)});
 const memory=section.result.nodes.find(n=>n.label==='memory');assert.ok(memory.metadata.totalBytes>0);assert.ok(memory.metadata.availableBytes>=0);assert.ok(Date.now()-memory.metadata.capturedAtMs<30000);pass('Device Brain contains current native memory observations',{totalBytes:memory.metadata.totalBytes,availableBytes:memory.metadata.availableBytes,capturedAtMs:memory.metadata.capturedAtMs});
 await page.locator('#nodes .node').filter({hasText:'motherboard'}).click();await page.waitForSelector('[data-local-brain-evidence]');assert.ok((await page.locator('[data-local-brain-evidence]').textContent()).includes('Win32_BaseBoard'));pass('Motherboard inspector shows native observation provenance');
 await page.screenshot({path:path.join(out,'pc-device-diagnostics.png')});
 await page.locator('#localClose').click();await page.waitForURL(origin+'/ui');
 const invalid=await fetch(origin+'/brain/query',{method:'POST',headers:{'content-type':'application/json','Origin':'https://outside.example'},body:JSON.stringify({operation:'root',arguments:{}})});assert.equal(invalid.status,403);pass('Foreign-origin Brain queries rejected');
 const get=await fetch(origin+'/brain/query');assert.equal(get.status,405);pass('Unstructured GET queries rejected');
 const outside=await fetch(origin+'/brain/query',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({operation:'node',arguments:{id:'brain:another-owner'}})});assert.equal((await outside.json()).ok,false);pass('Other-body node requests rejected');
 assert.equal(faults.length,0);pass('No uncaught JavaScript errors in actual installed navigation');
}catch(error){faults.push(error.stack||String(error));process.exitCode=1;}
finally{
 await browser?.close();const files=fs.readdirSync(out).filter(n=>n.endsWith('.png')).map(name=>({name,sha256:crypto.createHash('sha256').update(fs.readFileSync(path.join(out,name))).digest('hex')}));
 const receipt={schemaVersion:'leeway.installed-pc-surfaces.v1',status:faults.length?'FAILED':'VERIFIED_INSTALLED_PC_UI_AND_HARDWARE_SCOPE',fixtures:false,replacedNetworkResponses:false,checks,faults,data,files,phoneVisualVerification:false,fullGoldenRelease:false,formulaExecution:'NOT_EXECUTED'};
 fs.writeFileSync(path.join(out,'pc-live-acceptance.json'),JSON.stringify(receipt,null,2));console.log(JSON.stringify(receipt,null,2));
}

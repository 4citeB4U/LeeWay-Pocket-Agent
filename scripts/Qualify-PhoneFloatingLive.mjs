/* REGION: LEEWAY.DEVICE.LIVE_ACCEPTANCE; TAG: REAL_USB_OVERLAY_AND_SPEAKER
WHO: Creator-authorized Agent Lee. WHAT: Exercise the installed phone, no fixture UI/provider.
WHEN: After same-certificate in-place update. WHERE: selected ADB phone and its own WebView.
WHY: Source, APK and notification are not visible-overlay or speaker proof.
HOW: Existing Playwright Android WebView transport, real input, native state and screencap. LICENSE: MIT */
import fs from 'node:fs';import path from 'node:path';import {pathToFileURL} from 'node:url';import {execFileSync} from 'node:child_process';import assert from 'node:assert/strict';import {createHash} from 'node:crypto';
const args=new Map();for(let i=2;i<process.argv.length;i+=2)args.set(process.argv[i],process.argv[i+1]);
const { _android }=await import(pathToFileURL(args.get('--playwright')).href);const serial=args.get('--serial'),adb=args.get('--adb'),out=args.get('--out');fs.mkdirSync(out,{recursive:true});
const checks=[],errors=[],shots=[];let device,page;
function call(a,buffer=false){return execFileSync(adb,['-s',serial,...a],{encoding:buffer?undefined:'utf8',timeout:30000,windowsHide:true,maxBuffer:20*1024*1024});}
function pref(name,key){const xml=call(['exec-out','run-as','industries.leeway.pocket','cat','shared_prefs/'+name+'.xml']);const raw=xml.match(new RegExp('<string name="'+key+'">([\\s\\S]*?)</string>'))?.[1];if(!raw)throw Error('NATIVE_RECEIPT_NOT_READY');return JSON.parse(raw.replaceAll('&quot;','"').replaceAll('&lt;','<').replaceAll('&gt;','>').replaceAll('&amp;','&').replaceAll('&apos;',"'"));}
async function sleep(ms){await new Promise(r=>setTimeout(r,ms));}
async function until(fn,seconds=15){const start=Date.now();for(;;){try{const value=fn();if(value)return value;}catch{}if(Date.now()-start>seconds*1000)throw Error('LIVE_WAIT_TIMEOUT');await sleep(250);}}
function capture(name){const data=call(['exec-out','screencap','-p'],true);const start=data.indexOf(Buffer.from([137,80,78,71]));assert.ok(start>=0&&start<1024);const png=data.subarray(start);fs.writeFileSync(path.join(out,name+'.png'),png);shots.push({name:name+'.png',sha256:createHash('sha256').update(png).digest('hex')});}
function pass(name,evidence={}){checks.push({name,status:'PASS',...evidence});}
try{
 const devices=await _android.devices();device=devices.find(d=>d.serial()===serial);assert.ok(device,'USB_TARGET_REQUIRED');
 const view=await device.webView({pkg:'industries.leeway.pocket'});page=await view.page();
 await page.waitForFunction(()=>window.__leewayFloating);
 const p=call(['shell','dumpsys','package','industries.leeway.pocket']);assert.match(p,/versionCode=30 /);pass('Installed package is version 30, not the old candidate');
 const overlay=pref('leeway-pocket-overlay','status_json');const sphere=pref('leeway-floating-sphere','status_json');assert.equal(overlay.attached,true);assert.equal(overlay.permissionGranted,true);assert.equal(sphere.type,'TYPE_APPLICATION_OVERLAY');assert.ok(sphere.screenAreaRatio<=.15);
 assert.equal(await page.locator('#agent-emblem,.agent-side-tab').count(),0);pass('Separate native right-edge emblem and bounded sphere; no in-page duplicate',{emblem:overlay.geometry,sphere});
 call(['shell','input','keyevent','KEYCODE_HOME']);await sleep(1200);capture('phone-home-overlay');
 assert.match(call(['shell','dumpsys','activity','activities']),/topResumedActivity[^\n]*com.sec.android.app.launcher/);pass('Home screen stays active behind the sphere, not MainActivity');
 call(['shell','am','start','--user','0','-n','com.sec.android.app.popupcalculator/.Calculator']);await sleep(1000);
 assert.match(call(['shell','dumpsys','activity','activities']),/topResumedActivity[^\n]*com.sec.android.app.popupcalculator/);assert.equal(pref('leeway-pocket-overlay','status_json').attached,true);assert.equal(pref('leeway-floating-sphere','status_json').state,'NATIVE_SPHERE_ATTACHED');capture('phone-calculator-overlay');pass('Ordinary calculator runs while the external emblem and sphere remain');
 const count0=await page.evaluate(()=>window.__leewayInteraction.sphereTaps);
 await page.locator('#hamburger-btn').click();await page.waitForFunction(()=>document.getElementById('drawer').classList.contains('open'));
 assert.equal(await page.evaluate(()=>window.__leewayInteraction.sphereTaps),count0);pass('Hamburger opens without invoking sphere speech');
 await page.locator('[data-setting="voice"]').click();await page.getByRole('button',{name:'Test selected LeeWay Voice',exact:true}).waitFor();
 assert.equal(await page.evaluate(()=>window.__leewayInteraction.sphereTaps),count0);pass('Voice menu touch does not trigger a sphere tap');capture('phone-voice-test-control');
 if(args.get('--play')==='yes'){
  await page.getByRole('button',{name:'Test selected LeeWay Voice',exact:true}).click();
  const playing=await until(()=>{const r=pref('leeway-voice-output-test','receipt_json');if(r.state==='VOICE_OUTPUT_TEST_BLOCKED'||r.state==='NATIVE_PLAYBACK_FAILED')throw Error(JSON.stringify(r));return r.state==='PLAYING_BUILTIN_SPEAKER'?r:null;},20);
  assert.equal(playing.actualOutputType,2);assert.equal(playing.systemTtsUsed,false);assert.equal(playing.nativeIsPlaying,true);pass('Actual selected Voice Fabric audio playing on built-in phone speaker',{playing});capture('phone-voice-playing');
  const completed=await until(()=>{const r=pref('leeway-voice-output-test','receipt_json');return r.state==='NATIVE_PLAYBACK_COMPLETED'?r:null;},30);
  assert.equal(completed.voicePackageId,'kokoro-am_michael');assert.ok(completed.playedPositionMs>1000);pass('Phone speaker playback completes without changing selected voice or volume',{completed});
 }
 await page.getByRole('button',{name:'Back to Agent Lee',exact:true}).click();await page.waitForFunction(()=>!document.getElementById('surface-panel').classList.contains('open'));
 const before=pref('leeway-floating-sphere','status_json');const x=before.x+Math.round(before.physicalWidth/2),y=before.y+Math.round(before.physicalHeight/2);
 call(['shell','input','swipe',String(x),String(y),String(x-100),String(y+100),'500']);await sleep(400);const moved=pref('leeway-floating-sphere','status_json');assert.ok(moved.x!==before.x||moved.y!==before.y);assert.equal(await page.evaluate(()=>window.__leewayInteraction.sphereTaps),count0);pass('Actual drag moves native sphere without speaking',{before:{x:before.x,y:before.y},after:{x:moved.x,y:moved.y}});
 await page.locator('#shrink-agent').evaluate(el=>el.style.opacity='1');await page.locator('#shrink-agent').click();await sleep(350);const smaller=pref('leeway-floating-sphere','status_json');assert.ok(smaller.physicalWidth<moved.physicalWidth);assert.ok(smaller.screenAreaRatio<=.15);pass('Owner shrink changes actual native window size',{size:smaller.physicalWidth,screenAreaRatio:smaller.screenAreaRatio});
 call(['shell','input','keyevent','KEYCODE_HOME']);await sleep(800);capture('phone-final-home-floating');
}catch(error){errors.push(error.stack||String(error));process.exitCode=1;}
finally{
 // Disconnect the test transport only. Leave the owner's running overlay and selected voice intact.
 await device?.close().catch(()=>{});
 const receipt={schemaVersion:'leeway.actual-phone-floating-and-audio.v1',status:errors.length?'FAILED':'VERIFIED_INSTALLED_PHONE_SCOPED_UI_AND_OUTPUT',fixtures:false,checks,errors,screenshots:shots,humanAudibilityConfirmed:false,conversationExecutorQualified:false,crossDeviceSelectionSyncQualified:false,fullGoldenRelease:false,formulaExecution:'NOT_EXECUTED'};
 fs.writeFileSync(path.join(out,'phone-live-receipt.json'),JSON.stringify(receipt,null,2));console.log(JSON.stringify(receipt,null,2));
}

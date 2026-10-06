/*
REGION: POCKET.UI.QUALIFICATION
TAG: SPHERE-VOICE-BOUNDARY-TEST
WHO: Agent Lee qualification
WHAT: Execute actual UI functions against isolated DOM/native bridge fixtures.
WHEN: Before deployment; WHERE: Pocket qualification runtime.
WHY: Simulated microphone activity must never count as speech availability.
HOW: Parse all classic inline scripts, then exercise extracted boundary functions.
LICENSE: MIT
*/
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
const source=fs.readFileSync(new URL('../app/src/main/assets/agent_lee_sphere_transparent.html',import.meta.url),'utf8');
function actualFunction(start,end){
 const a=source.indexOf(start),b=source.indexOf(end,a+start.length);
 assert.ok(a>=0 && b>a,'Actual function boundary must exist');
 return source.slice(a,b);
}
function fixture(){
 const events=[];let stopped=0;
 const c={isListening:true,isSpeaking:true,voiceAmplitude:0.8,
  statusBadge:{style:{},classList:{add(){},remove(){}}},statusText:{textContent:''},
  stopListening(){stopped++;c.isListening=false;},
  window:{dispatchEvent(e){events.push(e);}},
  CustomEvent:class {constructor(type,options){this.type=type;this.detail=options.detail;}},
  setInterval(){throw Error('Fake microphone timer is forbidden');},
  setTimeout(){throw Error('Fake microphone completion is forbidden');}
 };
 return {context:vm.createContext(c),events,stopped:()=>stopped};
}
test('approved sphere inline JavaScript parses without executing rendering',()=>{
 let parsed=0;
 for(const [,attributes,code] of source.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script>/gi)){
  if(/\bsrc=|\btype=["'](?:module|importmap|application\/json)/i.test(attributes))continue;
  new vm.Script(code);parsed++;
 }
 assert.ok(parsed>0);
});
test('unbound microphone stops activity and emits an error, never simulated speech',()=>{
 const f=fixture();
 vm.runInContext(actualFunction('    function voiceUnavailable(', '    function agentLeeSpeak('),f.context);
 vm.runInContext("voiceUnavailable('MICROPHONE_PROVIDER_UNBOUND')",f.context);
 assert.equal(f.context.isListening,false);assert.equal(f.context.isSpeaking,false);
 assert.equal(f.context.voiceAmplitude,0);assert.equal(f.stopped(),1);
 assert.equal(f.events.length,1);assert.equal(f.events[0].detail.reason,'MICROPHONE_PROVIDER_UNBOUND');
 assert.equal(f.context.statusBadge.style.display,'flex');
});
test('sphere tap uses the existing Android handoff once',async()=>{
 const f=fixture();let calls=0;f.context.window.LeeWayAndroid={talk(){calls++;}};
 f.context.startListening=()=>{throw Error('Native handoff must not start duplicate browser recognition');};
 vm.runInContext(actualFunction('    async function toggleVoiceInteraction()', '    function openDigitalBrain()'),f.context);
 await vm.runInContext('toggleVoiceInteraction()',f.context);
 assert.equal(calls,1);assert.equal(f.stopped(),0);
});
test('missing recognition is rejected before requesting microphone access',async()=>{
 const f=fixture();f.context.navigator={mediaDevices:{getUserMedia(){throw Error('Must not request a mic without recognition');}}};
 vm.runInContext(actualFunction('    function voiceUnavailable(', '    function agentLeeSpeak('),f.context);
 vm.runInContext(actualFunction('    async function startListening()', '    function stopListening()'),f.context);
 await vm.runInContext('startListening()',f.context);
 assert.equal(f.events[0].detail.reason,'SPEECH_RECOGNITION_UNBOUND');
});

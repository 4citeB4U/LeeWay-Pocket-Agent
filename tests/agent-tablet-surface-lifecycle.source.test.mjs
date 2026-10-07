// Execute the production Kotlin handler at a fixture lifecycle boundary.
// This mechanical projection checks handler ordering and retained state only;
// Android intent delivery, Kotlin compilation and physical rendering need the device gate.
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
import test from 'node:test';

const source=readFileSync(new URL('../app/src/main/java/industries/leeway/pocket/AgentTabletActivity.kt',import.meta.url),'utf8');
const signature='override fun onNewIntent(intent: Intent) {';
const start=source.indexOf(signature);
assert.notEqual(start,-1,'production onNewIntent must exist');
assert.equal(source.indexOf(signature,start+1),-1,'one lifecycle override');
let end=start+signature.length,depth=1;
for(;end<source.length&&depth;end++){
  if(source[end]==='{')depth++;
  if(source[end]==='}')depth--;
}
assert.equal(depth,0,'complete production handler');
const kotlin=source.slice(start+signature.length,end-1);
const selector=source.match(/private val continuumMode get\(\) = ([^\r\n]+)/)?.[1];
assert.ok(selector,'production selector must exist');
const projected=kotlin.replace(/\bval\s+/g,'const ')
  .replace(/\b(continuumMode|continuumAdmitted|web)\b/g,'this.$1')
  .replace(/\b(setIntent|recreate)\(/g,'this.$1(');

const intent=surface=>({surface,getStringExtra:key=>key==='leeway_surface'?surface:null});
class Boundary{
  constructor(surface){
    this.intent=intent(surface);this.events=[];this._admitted=surface==='continuum';
    this.web={scroll:321,stopLoading:()=>this.events.push({op:'stop',admitted:this._admitted,oldSurface:this.intent.surface})};
  }
  get continuumAdmitted(){return this._admitted;}
  set continuumAdmitted(value){this._admitted=value;this.events.push({op:'admission',value});}
  onNewIntent(){this.events.push({op:'super'});}
  setIntent(value){this.intent=value;this.events.push({op:'intent',surface:value.surface});}
  recreate(){this.events.push({op:'recreate',surface:this.intent.surface});}
}
const Subject=new Function('Boundary','EXTRA_SURFACE',`return class extends Boundary {
  get continuumMode(){return ${selector.replace(/\bintent\b/g,'this.intent')};}
  onNewIntent(intent){${projected}}
}`)(Boundary,'leeway_surface');

for(const [label,from,to,expected] of [
  ['Continuum to default VT','continuum',null,false],
  ['VT to Continuum',null,'continuum',true],
  ['Continuum to explicit non-Continuum entry','continuum','workstation',false],
])test(label+' revokes the old reader before intent replacement and requests one recreation',()=>{
  const view=new Subject(from);const next=intent(to);view.onNewIntent(next);
  assert.deepEqual(view.events.map(e=>e.op),['super','admission','stop','intent','recreate']);
  assert.deepEqual(view.events[2],{op:'stop',admitted:false,oldSurface:from});
  assert.equal(view.intent,next);assert.equal(view.continuumMode,expected);
  assert.equal(view.continuumAdmitted,false,'new Continuum must be admitted again by normal onCreate');
});

for(const surface of [null,'continuum'])test('same-surface '+String(surface)+' retains its view and admission while accepting the new intent',()=>{
  const view=new Subject(surface),web=view.web,next=intent(surface);view.onNewIntent(next);
  assert.deepEqual(view.events.map(e=>e.op),['super','intent']);
  assert.equal(view.intent,next);assert.equal(view.web,web);assert.equal(web.scroll,321);
  assert.equal(view.continuumAdmitted,surface==='continuum');
});

test('a mode change with no remaining WebView still updates the intent and recreates once',()=>{
  const view=new Subject('continuum');view.web=null;view.onNewIntent(intent(null));
  assert.deepEqual(view.events.map(e=>e.op),['super','admission','intent','recreate']);
  assert.equal(view.continuumMode,false);assert.equal(view.continuumAdmitted,false);
});

console.log(JSON.stringify({scope:'EXTRACTED_PRODUCTION_HANDLER_FIXTURE_NOT_ANDROID_RUNTIME',
  sourceSha256:createHash('sha256').update(source).digest('hex'),
  remainingGate:'Compile the actual Android app and verify reused Activity navigation plus insets on device.'}));

"""REGION: LEEWAY.UI.REPAIR; TAG: SHARED_POINTER_AND_CONSUMER_MENU_PATCH
WHO: Creator-authorized engineering. WHAT: Repair current installed sphere pages without changing geometry.
WHEN: After exact inspection. WHERE: existing Runtime/Pocket source. WHY: duplicate tag and global input bugs.
HOW: Dry-run exact section replacement; one shared interaction payload and source hashes. LICENSE: MIT.
"""
import argparse,pathlib,re,json,hashlib
p=argparse.ArgumentParser();p.add_argument('--runtime',required=True);p.add_argument('--apply',action='store_true');a=p.parse_args()
phone=pathlib.Path(__file__).resolve().parents[1];runtime=pathlib.Path(a.runtime);mc=runtime/'machine-consciousness'
def read(p):return p.read_text(encoding='utf-8-sig')
def sha(data):return hashlib.sha256(data).hexdigest()
def once(s,x,y):assert s.count(x)==1,'EXACT_BOUNDARY:'+x[:65];return s.replace(x,y)
shared=read(mc/'agent-ui-interaction.js')
css='''<style id="leeway-floating-repair">
html,body{background:transparent!important;margin:0;overflow:hidden;color:#f6f9ff}
.hamburger-btn{top:10px!important;left:10px!important;right:auto!important;transform:none!important;width:40px;height:40px;background:rgba(9,19,32,.8);border:1px solid rgba(255,255,255,.4)}
.drawer{background:rgba(5,14,25,.96)!important;color:#f5f8ff!important;width:min(350px,96vw)!important;padding:18px!important;box-sizing:border-box;overflow:auto}
.drawer-title{font-size:22px!important;color:#fff!important}.drawer-subtitle{font-size:13px!important;color:#c1d1e6!important}
.element-card{color:#fff!important;background:rgba(32,48,70,.96)!important;padding:14px!important;min-height:58px;gap:12px;width:100%;border:1px solid rgba(179,205,231,.3)!important;box-sizing:border-box;text-align:left}
.element-card>span:first-child{min-width:0}.element-name{display:block!important;font-size:16px!important;line-height:1.35!important;color:#fff!important}
.element-motto{display:block!important;font-size:13px!important;line-height:1.5!important;color:#ccdaeb!important;margin-top:5px!important}
.element-action{font-size:12px!important;letter-spacing:.7px;color:#97dcff!important;flex-shrink:0}.settings-list{display:flex;flex-direction:column;gap:10px}
.drawer-overlay{background:transparent!important;backdrop-filter:none!important}.status-badge{font-size:11px;max-width:90vw;bottom:5px;pointer-events:none;background:rgba(4,12,21,.8);color:#fff;opacity:.85}
#agent-controls{position:fixed;right:8px;bottom:30px;display:flex;gap:3px;z-index:24;opacity:0;transition:opacity .15s}body:hover #agent-controls,#agent-controls:focus-within{opacity:1}
#agent-controls button{background:rgba(5,14,25,.86);border:1px solid #acc4d8;color:white;border-radius:7px;width:28px;height:28px;padding:0;font-size:14px;cursor:pointer}
#agent-resize-grip{position:fixed;right:2px;bottom:2px;width:20px;height:20px;z-index:23;cursor:nwse-resize;touch-action:none;color:#afd8f4;font-size:18px}
#surface-panel{display:none;position:fixed;inset:12px;background:rgba(5,14,25,.97);color:#ecf4ff;z-index:50;padding:24px;overflow:auto;border:1px solid #739bbb;border-radius:16px;font:15px/1.65 system-ui}
#surface-panel.open{display:block}#surface-panel button{font:inherit;color:#fff;background:#183752;border:1px solid #87b6d2;border-radius:8px;padding:8px 12px;margin:6px 3px}#surface-panel h2{font-size:23px;color:#fff;margin-top:10px}#surface-panel p{max-width:65ch}#surface-panel .state{border-left:3px solid #87b6d2;padding-left:12px;color:#d8e8f7}
</style>'''
menus=[('digital-brain','Digital Brain','Your authorized files, objects and knowledge'),('continuum','Continuum','Your local database universes'),('diagnostics','This device','Hardware observations and diagnostics'),('voice','Voice','One selected Agent Lee voice across your devices'),('workstation','Agent VT','Virtual Tablet · Notebook · Notepad · Sandbox'),('models','Models · optional','Core operation does not require an LLM'),('skills','Skills reference','Read-only capability inventory'),('authority','Authority & Permissions','Owner-controlled access and revocation'),('evidence','Evidence & Receipts','Your permitted activity and execution records'),('about','About LeeWay','Purpose, services and how Agent Lee works')]
menu='\n'.join(f'<button class="element-card setting-card" data-setting="{key}"><span><span class="element-name">{title}</span><span class="element-motto">{sub}</span></span><span class="element-action">OPEN</span></button>' for key,title,sub in menus)
controls='''<div id="agent-controls"><button id="shrink-agent" title="Shrink Agent Lee">−</button><button id="grow-agent" title="Enlarge Agent Lee">+</button><button id="expand-agent" title="Expand / restore">⤢</button><button id="stop-agent-voice" title="Stop speech">■</button></div><div id="agent-resize-grip" role="button" aria-label="Resize Agent Lee">⌟</div><section id="surface-panel" aria-live="polite"></section>'''
about='''<h2>About LeeWay</h2><p><strong>Logically Enhanced Engineering for Workload, Authority &amp; Yield.</strong></p><p>LeeWay is designed to increase human capability without replacing human responsibility. Agent Lee is the user-facing assistant and coordinator: a consistent identity and workspace across your authorized devices, with explicit permissions and inspectable results.</p><h3>Your device, your information</h3><p>Each device has its own Digital Brain to organize authorized file observations, relationships, provenance and physical-device information. The Brain should reflect what has actually been observed; missing permissions, unavailable sensors and incomplete indexing must remain visible.</p><p>Continuum is LeeWay’s governed database fabric, organized as bounded cells and nested universes. Each device maintains local durable state; approved federation may relate that state across devices without making another customer’s files or authority yours. Device working memory is separate and local.</p><h3>Models are optional</h3><p>The LeeWay architecture does not require an LLM to perform its implemented deterministic duties. An optional local or API-connected language model is a tool, not the owner of identity, data, permissions or voice. Zero-LLM operation does not mean every conversational request is already implemented: the assistant must disclose unsupported requests rather than invent completion.</p><h3>One Agent Lee voice</h3><p>Voice belongs to the shared LeeWay Voice system. Your selected voice should remain consistent across devices. Synchronization and audibility require their own verification; an unavailable provider must not silently become another speaker.</p><h3>Work and services</h3><p>The canonical Agent VT provides a sandboxed Virtual Tablet for documents, notebooks, shared notepad events and bounded work before promotion. Skills and authorized Device/Commander capabilities supply execution, while evidence and receipts record what actually happened. Optional ecosystem services and remote access remain permission-bound.</p><p class="state">This installation is under qualification. A component’s presence is not proof that every service is connected. Your private data, owner control, transparent failure reporting and tested recovery are part of the product requirements.</p>'''
extra='''
window.addEventListener('leeway:open-surface',async event=>{
 const key=event.detail?.surface;const panel=document.getElementById('surface-panel');
 if(key==='digital-brain'||key==='diagnostics'){
  if(window.LeeWayAndroid){if(key==='diagnostics')window.LeeWayAndroid.openDiagnostics();else window.LeeWayAndroid.openDigitalBrain();return;}
  const route='/brain-ui/brain.html'+(key==='diagnostics'?'?surface=hardware':'');
  if(window.pywebview?.api){await window.pywebview.api.open_surface(route);return;}location.assign(route);return;
 }
 panel.replaceChildren();const close=document.createElement('button');close.textContent='Back to Agent Lee';close.onclick=()=>{panel.classList.remove('open');window.__leewayFloating.host('panel',false);};panel.appendChild(close);
 const article=document.createElement('article');panel.appendChild(article);panel.classList.add('open');await window.__leewayFloating.host('panel',true);
 if(key==='about'){article.innerHTML=ABOUT_HTML;return;}
 const title=document.createElement('h2');title.textContent={voice:'LeeWay Voice',models:'Models · optional',skills:'Skills reference',authority:'Authority & Permissions',workstation:'Agent VT',continuum:'Continuum',evidence:'Evidence & Receipts'}[key]||key;article.appendChild(title);
 try{
  if(key==='skills'){const data=await fetch('/owner-ui').then(r=>r.json());const p=document.createElement('p');p.textContent='Discovered skill documents: '+data.skillCount+'. This is a read-only inventory, not a claim that every skill has a qualified live executor.';article.appendChild(p);}
  else if(key==='voice'){
   if(window.LeeWayAndroid?.testVoice){const b=document.createElement('button');b.textContent='Test selected LeeWay Voice';b.onclick=()=>window.LeeWayAndroid.testVoice();article.appendChild(b);const p=document.createElement('p');p.textContent='Plays a current, verified Voice Fabric waveform through this phone speaker. This tests audio output, not the unbound conversation executor.';article.appendChild(p);return;}
   const data=await fetch('/owner-ui').then(r=>r.json());const p=document.createElement('p');p.textContent='Current shared selection: '+(data.voiceBinding?.voicePackageId||'No voice selected')+'. Device-to-device update delivery is still unqualified; selection is not silently changed here.';article.appendChild(p);
   const b=document.createElement('button');b.textContent='Test current voice';b.onclick=()=>{panel.classList.remove('open');window.__leewayFloating.host('panel',false);window.__leewayFloating.testVoice();};article.appendChild(b);
  }else if(key==='models'){const p=document.createElement('p');p.textContent='No LLM is required by the core. Local-model discovery and API-provider connections must use the existing Model Execution Runtime and protected credential storage. That settings adapter is not yet bound here; no credentials are requested or stored in this page.';article.appendChild(p);}
  else if(key==='authority'){const p=document.createElement('p');p.textContent='Overlay, microphone, camera and filesystem permissions remain owner controlled. No permission is implied by opening this menu.';article.appendChild(p);if(window.LeeWayAndroid?.enableOverlay){const b=document.createElement('button');b.textContent='Floating overlay permission';b.onclick=()=>window.LeeWayAndroid.enableOverlay();article.appendChild(b);}}
  else{const p=document.createElement('p');p.className='state';p.textContent={workstation:'Canonical Agent VT located. Its live host binding is being restored; this is not a replacement workstation.',continuum:'The canonical local Continuum runtime is not yet bound to this view. No database cells or records are being invented.',evidence:'The owner-scoped receipt browser is not yet bound to this view. Stored engineering receipts remain preserved.'}[key]||'This surface is not bound.';article.appendChild(p);}
 }catch(error){const p=document.createElement('p');p.className='state';p.textContent='Unavailable: '+error.message;article.appendChild(p);}
});
(async()=>{try{const data=window.LeeWayAndroid?.deviceName?{deviceName:window.LeeWayAndroid.deviceName()}:await fetch('/owner-ui').then(r=>r.json());if(data.deviceName)document.querySelector('[data-setting="diagnostics"] .element-name').textContent=data.deviceName;}catch{}})();
'''.replace('ABOUT_HTML',json.dumps(about))
changes={}
for file in [mc/'pocket-voice.html',phone/'app/src/main/assets/agent_lee_sphere_transparent.html']:
 s=read(file)
 # Remove only the assistant-added in-page tab and its added styling; external native tab is untouched.
 s=re.sub(r'<style id="leeway-live-emblem">[\s\S]*?</style>','',s)
 s=re.sub(r'<style id="leeway-shared-emblem-style">[\s\S]*?</style>','',s)
 s=re.sub(r'<button[^>]+id="agent-emblem"[\s\S]*?</button>','',s)
 s=re.sub(r'<div class="agent-side-tab"[^>]*></div>','',s)
 s=re.sub(r'<script id="leeway-live-surfaces">[\s\S]*?</script>','',s)
 start=s.index('    // --- INTERACTIVE TOUCH & DRAG / CLICKABLE SPHERE ---');end=s.index('    // --- VOICE & TALKING ENGINE ---',start)
 s=s[:start]+'''    // Input is owned by the shared pointer-boundary adapter, not the whole window.
    let isDragging=false,yaw=0.35,pitch=0.12;

'''+s[end:]
 if 'class="settings-list"' in s:
  start=s.index('    <div class="settings-list">');end=s.index('\n  </div>\n\n  <script>',start);s=s[:start]+'    <div class="settings-list">\n'+menu+'\n    </div>'+s[end:]
 else:
  start=s.index('    <div style="font-size:10px;');end=s.index('\n  </div>\n\n  <script>',start);s=s[:start]+'<div class="settings-list">'+menu+'</div>'+s[end:]
  # Android previously used inline handlers. Attach the same explicit menu dispatch.
  extra_android="document.querySelectorAll('.setting-card').forEach(b=>b.addEventListener('click',()=>{closeDrawer();window.dispatchEvent(new CustomEvent('leeway:open-surface',{detail:{surface:b.dataset.setting}}));}));"
  s=once(s,'</body>','<script>'+extra_android+'</script>\n</body>')
 s=once(s,'</head>',css+'\n</head>')
 s=once(s,'</body>',controls+('\n<script src="/voice-fabric/src/browser-voice.js"></script><script src="/voice-fabric/src/speech-pipeline.js"></script>' if file==mc/'pocket-voice.html' else '')+'\n<script>\n'+shared+'\n'+extra+'\n</script>\n</body>')
 s=s.replace('One Consciousness · One Body','One Agent Lee · Your authorized devices')
 changes[file]=s
server=mc/'carrier-server.mjs';s=read(server)
needle="if(req.method==='GET'&&u.pathname==='/health')"
route="if(req.method==='GET'&&u.pathname==='/owner-ui')return json(res,200,{deviceName:os.hostname(),skillCount:countSkillDocs(SKILL_ROOT),voiceBinding:voiceState(),llmRequired:false,modelProvidersBound:0});"
s=once(s,needle,route+needle);changes[server]=s
plan={'status':'DRY_RUN','files':[str(p)for p in changes],'sharedInteractionSha256':sha(shared.encode()),'externalEmblemRemoved':False,'inPageEmblemRemoved':True,'geometryChanged':False,'serviceReadinessNotClaimedByMenu':True}
if a.apply:
 for file,s in changes.items():file.write_text(s,encoding='utf8',newline='\n')
 (mc/'agent-ui-interaction.js').write_text(shared,encoding='utf8',newline='\n')
 plan['status']='PATCHED_NATIVE_QUALIFICATION_REQUIRED';plan['after']={str(p):sha(p.read_bytes())for p in changes}
 (phone/'qualification/floating-interaction-20261006/page-patch.json').write_text(json.dumps(plan,indent=2))
print(json.dumps(plan,indent=2))

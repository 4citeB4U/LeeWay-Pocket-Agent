"""REGION: LEEWAY.DEPLOYMENT; TAG: INSTALLED_PC_BRAIN_ROUTING
WHO: Creator-authorized engineering. WHAT: Bind existing carrier UI to the existing PC Brain.
WHEN: Issue-specific live repair. WHERE: Explicitly inspected runtime and Brain manifest.
WHY: Event-only UI cannot open a Brain. HOW: Backup, exact match, same server, shared assets. LICENSE: MIT.
"""
import argparse,pathlib,hashlib,json,sqlite3,shutil
p=argparse.ArgumentParser();p.add_argument('--runtime',required=True);p.add_argument('--body-manifest',required=True);p.add_argument('--database',required=True);p.add_argument('--out',required=True);p.add_argument('--apply',action='store_true');a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1];runtime=pathlib.Path(a.runtime);mc=runtime/'machine-consciousness';out=pathlib.Path(a.out)
def sha(b):return hashlib.sha256(b).hexdigest()
def load(p):return p.read_text(encoding='utf-8-sig')
def once(s,x,y):assert s.count(x)==1, x[:70];return s.replace(x,y)
server=mc/'carrier-server.mjs';data=server.read_bytes();assert sha(data)=='bfd80e408fd757a20fb081899b38c65523c390bbbee3b62a8042802fe0237f8c'
s=load(server);s="import {handleLocalBrain} from './pc-brain-view.mjs';\n"+s
s=once(s,"const u=new URL(req.url,'http://127.0.0.1');","const u=new URL(req.url,'http://127.0.0.1');if(await handleLocalBrain(req,res,u))return;")
ui=mc/'pocket-voice.html';html=load(ui)
css='''<style id="leeway-live-emblem">#agent-emblem{position:fixed;right:14px;top:50%;width:68px;height:68px;transform:translateY(-50%);padding:0;border:0;background:transparent;z-index:24;cursor:pointer}#agent-emblem img{width:100%;height:100%;object-fit:contain;animation:leeway-live-colors 15s linear infinite}@keyframes leeway-live-colors{0%,100%{filter:hue-rotate(0deg)}33%{filter:hue-rotate(105deg)}66%{filter:hue-rotate(210deg)}85%{filter:grayscale(1)}}@media(prefers-reduced-motion:reduce){#agent-emblem img{animation:none}}</style>'''
html=once(html,'</head>',css+'\n</head>')
html=once(html,'</body>','''<button id="agent-emblem" aria-label="Open Agent Lee settings" onclick="openDrawer()"><img src="/brain-ui/agent-lee-emblem.png" alt="Agent Lee"></button>
<script id="leeway-live-surfaces">
window.addEventListener('leeway:open-surface',event=>{
 const surface=event.detail?.surface;
 if(surface==='digital-brain')location.assign('/brain-ui/brain.html');
 else if(surface==='diagnostics')location.assign('/brain-ui/brain.html?surface=hardware');
});
</script>
</body>''')
binding=runtime.parent/'host-commander.binding.v1.json';b=json.loads(load(binding));b['brainManifest']=str(pathlib.Path(a.body_manifest).resolve())
manifest=pathlib.Path(a.body_manifest);m=json.loads(load(manifest));m['bodyLocal']['digitalBrainDatabase']=str(pathlib.Path(a.database).resolve());m['commander']['state']='NATIVE_LISTENER_BOUND_CAPABILITY_TESTS_SEPARATE'
changes={server:s,ui:html,binding:json.dumps(b,indent=2),manifest:json.dumps(m,indent=2)}
result={'status':'DRY_RUN','paths':list(map(str,changes)),'serverBeforeSha256':sha(data),'voiceBindingChanged':False,'newServer':False,'newBrainDatabase':False}
if a.apply:
 out.mkdir(parents=True,exist_ok=False)
 for i,(file,text)in enumerate(changes.items()):
  shutil.copy2(file,out/(str(i)+'-'+file.name));file.write_text(text,encoding='utf8',newline='\n')
 source=sqlite3.connect(pathlib.Path(a.database).resolve().as_uri()+'?mode=ro',uri=True);dest=sqlite3.connect(out/'pc-brain-pre-repair.sqlite');source.backup(dest);dest.close();source.close()
 assets=root/'app/src/main/assets/digital-brain';target=mc/'digital-brain';assert not target.exists(),'EXISTING_PC_VIEWER_REQUIRES_HASH_RECONCILIATION'
 shutil.copytree(assets,target);shutil.copy2(root/'app/src/main/assets/agent-lee-emblem.png',target/'agent-lee-emblem.png')
 result['status']='APPLIED_RESTART_AND_LIVE_TEST_REQUIRED';result['after']={str(file):sha(file.read_bytes()) for file in changes}
 (out/'pc-repair.json').write_text(json.dumps(result,indent=2),encoding='utf8')
print(json.dumps(result,indent=2))

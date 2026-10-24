"""REGION: LEEWAY.INSTALLED.REPAIR; TAG: INPUT_AND_CANONICAL_VT_MINIMAL_REPAIR
WHO: Creator-authorized engineering. WHAT: Repair existing native input/geometry and VT launch.
WHEN: Actual installed-control failure. WHERE: inspected Device Bridge/Runtime/Pocket sources only.
WHY: Renderer appearance is not interaction. HOW: Exact boundaries, backups, one existing transport,
no model/voice selection change and no overwrite of unrelated uncommitted work. LICENSE: MIT.
"""
import argparse,pathlib,json,hashlib,shutil,subprocess,urllib.request
p=argparse.ArgumentParser();p.add_argument('--body',required=True);p.add_argument('--device',required=True);p.add_argument('--vt-preview-root',required=True);p.add_argument('--speech-assembly',required=True);p.add_argument('--out',required=True);p.add_argument('--apply',action='store_true');a=p.parse_args()
phone=pathlib.Path(__file__).resolve().parents[1];body=pathlib.Path(a.body);device=pathlib.Path(a.device);runtime=body/'runtime';out=pathlib.Path(a.out)
def text(p):return p.read_text(encoding='utf-8-sig')
def once(s,x,y):assert s.count(x)==1,'REPAIR_BOUNDARY_NOT_UNIQUE:'+x[:100];return s.replace(x,y)
def digest(b):return hashlib.sha256(b).hexdigest()
for r,repo in [(phone,'LeeWay-Pocket-Agent'),(runtime,'Leeway-Runtime-Fabric'),(device,'LEEWAY-DEVICE-BRIDGE')]:
 assert subprocess.check_output(['git','-C',str(r),'remote','get-url','origin'],text=True).strip()=='https://github.com/4citeB4U/'+repo+'.git'
bindingPath=body/'host-commander.binding.v1.json';binding=json.loads(text(bindingPath));host=pathlib.Path(binding['nativeAgentUi']['source'])
assert host.is_relative_to(device) and digest(host.read_bytes()).upper()==binding['nativeAgentUi']['sha256'],'LIVE_NATIVE_BINDING_SOURCE_MISMATCH'
lockPath=phone/'app/src/main/assets/agent-vt/SOURCE.json';lock=json.loads(text(lockPath));vtAssets=lockPath.parent
assert lock['repository']=='4citeB4U/LeeWay-Digital-Workforce' and digest((vtAssets/'standalone.html').read_bytes())==lock['sha256']
for name,entry in lock['resources'].items():
 assert '..' not in pathlib.PurePosixPath(name).parts and digest((vtAssets/name).read_bytes())==entry['sha256'],'VT_RESOURCE_MISMATCH'
vtRoot=pathlib.Path(a.vt_preview_root);assert subprocess.check_output(['git','-C',str(vtRoot),'remote','get-url','origin'],text=True).strip()=='https://github.com/4citeB4U/LeeWay-Digital-Workforce.git'
assert subprocess.check_output(['git','-C',str(vtRoot),'rev-parse','HEAD'],text=True).strip()==lock['sourceCommit'],'VT_SOURCE_ANCESTOR_MISMATCH'
vtTarget=vtRoot/'apps/agent-tablet/dist/agent-vt-embedded';assert not vtTarget.exists(),'VT_DEPLOYMENT_ALREADY_EXISTS_REVIEW_FIRST'
speech=pathlib.Path(a.speech_assembly);assert speech.is_file() and speech.name=='System.Speech.dll'
s=text(host)
s=once(s,"lock=kernel32.CreateMutexW(None,False,'Local\\\\LeeWayAgentLeeFloatingHost')\nif kernel32.GetLastError()==183:raise SystemExit(0)","""kernel32.CreateEventW.restype=ctypes.c_void_p
wake_name='Local\\\\LeeWayAgentLeeWake_'+hashlib.sha256(str(binding_path).casefold().encode()).hexdigest()[:24]
wake=kernel32.CreateEventW(None,False,False,wake_name)
lock=kernel32.CreateMutexW(None,False,'Local\\\\LeeWayAgentLeeFloatingHost')
if kernel32.GetLastError()==183:
 kernel32.SetEvent(ctypes.c_void_p(wake));raise SystemExit(0)""")
start=s.index('def style_native():');end=s.index('\ndef save():',start)
s=s[:start]+'''def style_native():
 global input_plane,input_handlers
 if not native_ready:return
 from System import Action
 from System.Drawing import Color,Region,Rectangle,Point
 from System.Drawing.Drawing2D import GraphicsPath
 from webview.platforms.winforms import BrowserView
 import System.Windows.Forms as Forms
 form=BrowserView.instances[window.uid]
 def update():
  global input_plane,input_handlers
  form.TopMost=True;form.AllowTransparency=True
  if panel or expanded:
   # Normal, focusable native WebView input for menus. Do not forward or overlay their clicks.
   if input_plane is not None:input_plane.Hide()
   form.TransparencyKey=Color.Empty;form.BackColor=Color.FromArgb(5,14,25);form.Region=None
   handle=form.Handle.ToInt64();user32.SetWindowLongW(handle,-20,user32.GetWindowLongW(handle,-20)&~0x08000000)
   return
  form.BackColor=Color.FromArgb(1,2,3);form.TransparencyKey=Color.FromArgb(1,2,3)
  form.webview.DefaultBackgroundColor=Color.Transparent
  w,h=form.ClientSize.Width,form.ClientSize.Height;scale=float(form._scale)
  shape=GraphicsPath();shape.AddEllipse(2,2,w-4,max(1,h-int(30*scale)))
  shape.AddRectangle(Rectangle(0,0,min(int(65*scale),w),min(int(65*scale),h)))
  shape.AddRectangle(Rectangle(0,max(0,h-int(75*scale)),w,min(h,int(75*scale))))
  old=form.Region;form.Region=Region(shape);shape.Dispose()
  if old is not None:old.Dispose()
  if input_plane is None:
   f=Forms.Form();f.Text='Agent Lee native input plane';f.FormBorderStyle=getattr(Forms.FormBorderStyle,'None')
   f.ShowInTaskbar=False;f.TopMost=True;f.BackColor=Color.Black;f.Opacity=.01
   handle=f.Handle.ToInt64();user32.SetWindowLongW(handle,-20,user32.GetWindowLongW(handle,-20)|0x08000000|0x80)
   gesture={}
   def forward(kind,event,pressed=False):
    if form.webview.CoreWebView2 is None:return
    cursor=Forms.Cursor.Position;pt=form.webview.PointToClient(cursor);sc=float(form._scale)
    payload={'type':kind,'x':pt.X/sc,'y':pt.Y/sc,'button':'left' if pressed else 'none','buttons':1 if pressed else 0}
    if kind in ('mousePressed','mouseReleased'):payload.update(clickCount=1,button='left')
    if kind=='mouseWheel':payload.update(deltaX=0,deltaY=-event.Delta)
    last_mouse.append({'event':kind,'cssX':payload['x'],'cssY':payload['y'],'screenX':cursor.X,'screenY':cursor.Y,'at':time.time()});del last_mouse[:-8]
    form.webview.CoreWebView2.CallDevToolsProtocolMethodAsync('Input.dispatchMouseEvent',json.dumps(payload))
   def down(sender,event):
    if str(event.Button)!='Left':return
    f.Capture=True;cursor=Forms.Cursor.Position;sc=float(form._scale)
    x,y=event.X/sc,event.Y/sc;vh=form.ClientSize.Height/sc;vw=form.ClientSize.Width/sc
    mode='resize' if x>vw-24 and y>vh-24 else ('move' if y>65 and y<vh-75 else 'control')
    gesture.update(x=cursor.X,y=cursor.Y,left=form.Left,top=form.Top,width=form.Width,height=form.Height,mode=mode,moved=False)
    forward('mousePressed',event,True)
   def move(sender,event):
    if not f.Capture or not gesture:forward('mouseMoved',event,False);return
    cursor=Forms.Cursor.Position;dx=cursor.X-gesture['x'];dy=cursor.Y-gesture['y'];sc=float(form._scale)
    if gesture['mode']!='control' and (gesture['moved'] or math.hypot(dx,dy)>7*sc):
     if not gesture['moved']:
      gesture['moved']=True
      form.webview.CoreWebView2.ExecuteScriptAsync("window.__leewayFloating?.cancel();window.__leewayInteraction.dragStarts++")
     if gesture['mode']=='move':
      left=max(int(work['x']*sc),min(int((work['x']+work['width'])*sc)-form.Width,gesture['left']+dx))
      top=max(int(work['y']*sc),min(int((work['y']+work['height'])*sc)-form.Height,gesture['top']+dy))
      form.Location=Point(left,top)
     else:
      from System.Drawing import Size
      side=max(int(120*sc),min(int(max_compact*sc),gesture['width']+dx));form.Size=Size(side,side)
     if f.Bounds!=form.Bounds:f.Bounds=form.Bounds
     return
    forward('mouseMoved',event,True)
   def up(sender,event):
    if str(event.Button)!='Left':return
    forward('mouseReleased',event,False);f.Capture=False
    moved=bool(gesture.get('moved'));gesture.clear()
    if moved:threading.Thread(target=lambda:(style_native(),save()),daemon=True).start()
   def wheel(sender,event):forward('mouseWheel',event,False)
   f.MouseDown+=down;f.MouseMove+=move;f.MouseUp+=up;f.MouseWheel+=wheel
   input_handlers=[down,move,up,wheel];input_plane=f
  if input_plane.Bounds!=form.Bounds:input_plane.Bounds=form.Bounds
  prior=input_plane.Region;input_plane.Region=form.Region.Clone()
  if prior is not None:prior.Dispose()
  if not input_plane.Visible:input_plane.Show(form)
 # Never synchronously re-enter the native UI thread while a geometry call owns the API lock.
 if form.InvokeRequired:form.BeginInvoke(Action(update))
 else:update()
''' +s[end:]
s=once(s,"   style_native();save();receipt('NATIVE_HOST_RUNNING');return", "   if action!='status':style_native();save()\n   receipt('NATIVE_HOST_RUNNING');return")
s=once(s,"  import clr;clr.AddReference('System.Speech')", """  import clr
  assembly=binding['nativeAgentUi'].get('speechAssembly')
  if not assembly or hashlib.sha256(pathlib.Path(assembly['path']).read_bytes()).hexdigest()!=assembly['sha256']:raise RuntimeError('NATIVE_INPUT_ASSEMBLY_NOT_ADMITTED')
  clr.AddReference(assembly['path'])""")
s=once(s," def open_surface(self,relative):", """ def open_workstation(self):
  global vt_window
  record=binding['nativeAgentUi'].get('agentVt')
  if not record:raise RuntimeError('CANONICAL_VT_NOT_BOUND')
  with urllib.request.urlopen(record['url'],timeout=8) as response:content=response.read(16777217)
  if len(content)>16777216 or hashlib.sha256(content).hexdigest()!=record['sha256']:raise RuntimeError('CANONICAL_VT_PAYLOAD_CHANGED')
  if vt_window is not None and vt_window in webview.windows:
   vt_window.show();vt_window.restore()
  else:
   # A screen within this existing Agent Lee host process, with NO native API bridge.
   vt_window=webview.create_window('Agent Lee - Agent VT',record['url'],width=min(1100,work['width']),height=min(780,work['height']),js_api=None,on_top=False,focus=True,text_select=True)
  receipt('CANONICAL_VT_OPENED',{'vtSourceSha256':record['sha256'],'vtNativeBridgeExposed':False})
  return {'state':'CANONICAL_VT_OPENED','sourceSha256':record['sha256'],'providerState':'NOT_PROMOTED_BY_UI_LAUNCH'}
 def open_surface(self,relative):""")
s=once(s,"api=HostApi()", "vt_window=None\napi=HostApi()")
s=once(s,"self.geometry('panel',True);window.load_url(origin+relative);return", "self.geometry('panel',True);threading.Timer(.2,lambda:window.load_url(origin+relative)).start();return")
s=once(s,"  native_ready=True;style_native()", "  native_ready=True\n  if window.get_current_url()==url and panel:api.geometry('panel',False)\n  style_native()")
s=once(s," global last_ui_state\n captured=False", " global last_ui_state\n captured=False")
s=once(s,"   if native_ready:\n    last_ui_state=", "   if kernel32.WaitForSingleObject(ctypes.c_void_p(wake),0)==0:\n    api.geometry('panel',False);window.show();window.restore();style_native()\n   if native_ready:\n    last_ui_state=")
s=once(s,"window.__leewayInteraction,status:document", "window.__leewayInteraction,shrink:document.getElementById('shrink-agent')?.getBoundingClientRect().toJSON(),grow:document.getElementById('grow-agent')?.getBoundingClientRect().toJSON(),expand:document.getElementById('expand-agent')?.getBoundingClientRect().toJSON(),workstation:document.querySelector('[data-setting=workstation]')?.getBoundingClientRect().toJSON(),url:location.href,status:document")
s=once(s," kernel32.CloseHandle(ctypes.c_void_p(lock))", " kernel32.CloseHandle(ctypes.c_void_p(wake));kernel32.CloseHandle(ctypes.c_void_p(lock))")
changes={host:s}
page=runtime/'machine-consciousness/pocket-voice.html';html=text(page)
html=once(html," const key=event.detail?.surface;const panel=document.getElementById('surface-panel');", """ const key=event.detail?.surface;const panel=document.getElementById('surface-panel');
 if(key==='workstation'){
  try{if(window.pywebview?.api?.open_workstation){await window.pywebview.api.open_workstation();return;}}catch(error){document.getElementById('status-text').textContent='Agent VT: '+error.message;return;}
 }
""")
html=once(html,'z-index:24;opacity:0;transition:opacity .15s','z-index:24;opacity:.85;transition:opacity .15s')
# Do not run a second Voice Fabric instance for native clicks. Keep the selected SDK as sole playback owner.
changes[page]=html
launcher=body/'Start-Agent-Lee.ps1';launch=text(launcher)
launch=once(launch,"if($existing.Count -eq 1){Write-Output ('EXISTING_NATIVE_AGENT_UI_PID='+$existing[0].ProcessId);return}","""if($existing.Count -eq 1){
 $hash=[Security.Cryptography.SHA256]::Create();try{$id=([BitConverter]::ToString($hash.ComputeHash([Text.Encoding]::UTF8.GetBytes($bindingPath.ToLowerInvariant())))).Replace('-','').ToLowerInvariant().Substring(0,24)}finally{$hash.Dispose()}
 $signal=[Threading.EventWaitHandle]::OpenExisting('Local\\LeeWayAgentLeeWake_'+$id);try{[void]$signal.Set()}finally{$signal.Dispose()}
 Write-Output ('EXISTING_NATIVE_AGENT_UI_RESTORED_PID='+$existing[0].ProcessId);return
}""");changes[launcher]=launch
binding['nativeAgentUi']['speechAssembly']={'path':str(speech),'sha256':digest(speech.read_bytes()),'role':'INPUT_RECOGNITION_NOT_VOICE_SELECTION'}
binding['nativeAgentUi']['agentVt']={'url':'http://127.0.0.1:4173/agent-vt-embedded/standalone.html','sha256':lock['sha256'],'repository':lock['repository'],'sourceCommit':lock['sourceCommit'],'sourceState':lock['sourceState'],'nativeBridge':False,'scope':'EXACT_EMBEDDED_VT_SURFACE_NOT_EXECUTION_GATE'}
binding['nativeAgentUi']['sha256']=digest(s.encode('utf8')).upper();changes[bindingPath]=json.dumps(binding,indent=2)
plan={'status':'DRY_RUN','scope':'CURRENT_NATIVE_HOST_AND_VT_LAUNCH','inputs':{str(f):digest(f.read_bytes()) for f in changes},'vtHash':lock['sha256'],'vtSourceState':lock['sourceState'],'voiceSelectionChanged':False,'newWorkstation':False,'phoneInstalled':False}
if a.apply:
 out.mkdir(parents=True,exist_ok=False)
 for i,(f,new)in enumerate(changes.items()):shutil.copy2(f,out/(str(i)+'-'+f.name));f.write_bytes(new.encode('utf8'))
 shutil.copytree(vtAssets,vtTarget)
 plan['status']='APPLIED_RESTART_AND_NATIVE_TESTS_REQUIRED';plan['outputs']={str(f):digest(f.read_bytes())for f in changes}
 (out/'patch.json').write_text(json.dumps(plan,indent=2))
print(json.dumps(plan,indent=2))

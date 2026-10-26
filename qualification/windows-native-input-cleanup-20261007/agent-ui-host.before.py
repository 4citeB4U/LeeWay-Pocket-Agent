


"""REGION: LEEWAY.DEVICES.NATIVE_UI; TAG: TRANSPARENT_OWNER_UI_ADAPTER
WHO: Owner-authorized Agent Lee. WHAT: Native floating host for the existing bound Agent Lee UI.
WHEN: Existing launcher opens. WHERE: Windows adapter under Device Bridge, not another Runtime.
WHY: Browser app mode cannot be the transparent, topmost desktop companion.
HOW: Installed pywebview/WebView2, bounded geometry API, one process lock, native speech recognition.
LICENSE: MIT
"""
import argparse,ctypes,hashlib,json,math,os,pathlib,threading,time,urllib.request,urllib.parse
import webview
p=argparse.ArgumentParser();p.add_argument('--binding',required=True);p.add_argument('--evidence',required=True);p.add_argument('--probe-seconds',type=int,default=0);a=p.parse_args()
binding_path=pathlib.Path(a.binding).resolve();binding=json.loads(binding_path.read_text(encoding='utf-8-sig'))
url=binding['agentUiUri'];parts=urllib.parse.urlsplit(url)
assert parts.scheme=='http' and parts.hostname in ('127.0.0.1','localhost') and parts.path=='/ui','LOCAL_OWNER_UI_BINDING_REQUIRED'
origin=parts.scheme+'://'+parts.netloc
with urllib.request.urlopen(origin+'/health',timeout=8) as r:health=json.load(r)
assert health.get('identity')=='LEEWAY_MACHINE_CONSCIOUSNESS','CARRIER_IDENTITY_MISMATCH'
output=pathlib.Path(a.evidence).resolve();output.mkdir(parents=True,exist_ok=True)
state_path=binding_path.parent/'agent-ui-geometry.json'
user32=ctypes.windll.user32;kernel32=ctypes.windll.kernel32
kernel32.CreateMutexW.restype=ctypes.c_void_p
kernel32.CreateEventW.restype=ctypes.c_void_p
wake_name='Local\\LeeWayAgentLeeWake_'+hashlib.sha256(str(binding_path).casefold().encode()).hexdigest()[:24]
wake=kernel32.CreateEventW(None,False,False,wake_name)
toggle_name='Local\\LeeWayAgentLeeToggle_'+hashlib.sha256(str(binding_path).casefold().encode()).hexdigest()[:24]
toggle=kernel32.CreateEventW(None,False,False,toggle_name)
if not wake or not toggle:raise OSError('NATIVE_CONTROL_EVENT_CREATION_FAILED')
lock=kernel32.CreateMutexW(None,False,'Local\\LeeWayAgentLeeFloatingHost')
if kernel32.GetLastError()==183:
 kernel32.SetEvent(ctypes.c_void_p(wake));raise SystemExit(0)
# Let the existing pywebview adapter establish DPI awareness and convert logical geometry.
screens=webview.screens;screen=next((item for item in screens if item.x==0 and item.y==0),screens[0])
left=min(s.x for s in screens);top=min(s.y for s in screens);right=max(s.x+s.width for s in screens);bottom=max(s.y+s.height for s in screens);work={'x':left,'y':top,'width':right-left,'height':bottom-top}
max_compact=int(math.sqrt(work['width']*work['height']*.15));size=min(340,max_compact,int(min(work['width'],work['height'])*.6));size=max(140,size)
initial={'width':size,'height':size,'x':work['x']+work['width']-size-88,'y':work['y']+int(work['height']*.2)}
if state_path.exists():
 try:
  saved=json.loads(state_path.read_text(encoding='utf-8-sig'));s=max(120,min(max_compact,int(saved['width'])));initial.update(width=s,height=s,x=int(saved['x']),y=int(saved['y']))
 except Exception:pass
initial['x']=max(work['x'],min(work['x']+work['width']-initial['width'],initial['x']));initial['y']=max(work['y'],min(work['y']+work['height']-initial['height'],initial['y']))
window=None;recognizer=None;native_handlers=[];panel=False;expanded=False;normal=dict(initial);closed=threading.Event();gate=threading.RLock();write_lock=threading.Lock();native_ready=False;input_plane=None;input_handlers=[];last_ui_state={};last_mouse=[];last_evidence_error=None;last_page_observed=0.0;visibility_observation={};page_probe={"task":None,"queued":False,"at":0.0,"error":None};page_probe_lock=threading.Lock()
def receipt(status,extra=None):
 global last_evidence_error
 value={'schemaVersion':'leeway.native-floating-host.v1','observedAt':time.time(),'status':status,'pid':os.getpid(),'provider':'LEEWAY_DEVICE_BRIDGE_NATIVE_UI','url':url,'screen':work,'topmostRequested':True,'transparentRequested':True,'framelessRequested':True,'nativeHitPlane':input_plane is not None,'panelOpen':panel,'expandedByOwner':expanded,'fixedPhysicalPathsInSource':False,'formulaExecution':'NOT_EXECUTED'}
 if window:value['geometry']={'x':window.x,'y':window.y,'width':window.width,'height':window.height,'screenAreaRatio':window.width*window.height/(work['width']*work['height'])}
 value['ownPage']=last_ui_state;value['ownMouse']=last_mouse[-8:]
 value['pageObservationAgeSeconds']=None if not last_page_observed else max(0,time.time()-last_page_observed)
 value['pageProbeError']=page_probe.get('error');value['visibilityObservation']=dict(visibility_observation)
 value['priorEvidenceWriteError']=last_evidence_error
 if extra:value.update(extra)
 with write_lock:
  temp=output/'host-receipt.tmp'
  try:
   temp.write_text(json.dumps(value,indent=2),encoding='utf-8')
   for attempt in range(4):
    try:os.replace(temp,output/'host-receipt.json');last_evidence_error=None;return True
    except OSError as error:
     if attempt==3 or getattr(error,'winerror',None) not in (5,32):raise
     time.sleep(.025*(attempt+1))
  except OSError as error:
   # This is a mutable status projection, not an immutable execution receipt.
   # A reader may deny replacement while still allowing writes. Honour normal file access;
   # never close its handles or weaken permissions. Readers must validate the snapshot digest.
   if os.name=='nt' and getattr(error,'winerror',None) in (5,32):
    target=output/'host-receipt.json'
    try:
     value['recordClass']='MUTABLE_NATIVE_OBSERVATION_NOT_VERITAS_RECEIPT'
     value['writeMode']='SHARED_FILE_IN_PLACE_CHECKSUM_READBACK'
     value['replacementFailureWinerror']=getattr(error,'winerror',None)
     value['snapshotSha256']=hashlib.sha256(json.dumps(value,sort_keys=True,separators=(',',':')).encode('utf-8')).hexdigest()
     data=json.dumps(value,indent=2).encode('utf-8')
     with target.open('r+b') as stream:
      stream.seek(0);stream.write(data);stream.truncate();stream.flush();os.fsync(stream.fileno())
     if target.read_bytes()!=data:raise OSError('NATIVE_STATUS_READBACK_MISMATCH')
     last_evidence_error=None;return True
    except OSError as fallback_error:error=fallback_error
   # Failure remains visible; an owner UI control is not an evidence-write transaction.
   last_evidence_error={'state':'FAILED_LATEST_PROJECTION','at':time.time(),'error':type(error).__name__,'winerror':getattr(error,'winerror',None),'pendingFile':str(temp)}
   return False
def emit(kind,detail):
 if window and not closed.is_set():
  try:window.evaluate_js('window.dispatchEvent(new CustomEvent('+json.dumps(kind)+',{detail:'+json.dumps(detail)+'}))')
  except Exception:pass
def style_native():
 global input_plane,input_handlers
 if not native_ready:return
 from System import Action
 from System.Drawing import Color,Region,Rectangle,Point
 from System.Drawing.Drawing2D import GraphicsPath
 import System.Windows.Forms as Forms
 from webview.platforms.winforms import BrowserView
 form=BrowserView.instances[window.uid]
 def update():
  global input_plane,input_handlers
  form.TopMost=True
  form.AllowTransparency=True;form.TransparencyKey=Color.Magenta;form.BackColor=Color.Magenta
  form.webview.DefaultBackgroundColor=Color.Transparent
  if panel or expanded:
   old=form.Region;form.Region=None
   if old is not None:old.Dispose()
  else:
   w,h=form.ClientSize.Width,form.ClientSize.Height;scale=float(form._scale)
   shape=GraphicsPath();margin=max(2,int(5*scale));hamb=max(48,int(62*scale))
   shape.AddEllipse(margin,margin,max(1,w-2*margin),max(1,h-2*margin));shape.AddRectangle(Rectangle(0,0,min(hamb,w),min(hamb,h)))
   old=form.Region;form.Region=Region(shape);shape.Dispose()
   if old is not None:old.Dispose()
  handle=form.Handle.ToInt64();style=user32.GetWindowLongW(handle,-20);user32.SetWindowLongW(handle,-20,style & ~0x20 & ~0x08000000)
  # The original failure reset the input-plane reference on every style refresh.
  # Retain one owned plane and handlers for this host lifetime; no global interception.
  if input_plane is None or input_plane.IsDisposed:
   f=Forms.Form();f.Text='Agent Lee native input plane';f.FormBorderStyle=getattr(Forms.FormBorderStyle,'None')
   f.ShowInTaskbar=False;f.TopMost=True;f.BackColor=Color.Black;f.Opacity=1.0/255.0
   f.AutoScaleMode=getattr(Forms.AutoScaleMode,'None');f.StartPosition=Forms.FormStartPosition.Manual
   h=f.Handle.ToInt64();user32.SetWindowLongW(h,-20,user32.GetWindowLongW(h,-20)|0x08000000|0x80)
   gesture={}
   def note(kind,extra=None):
    item={'event':kind,'at':time.time(),'source':'OWNED_WINFORMS_MOUSE_EVENT'}
    if extra:item.update(extra)
    last_mouse.append(item);del last_mouse[:-8]
   def event_point(event):
    # Event coordinates belong to the event, unlike the mutable system cursor.
    viewport=last_ui_state.get('viewport',{})
    sx=max(.1,float(f.ClientSize.Width)/max(1,float(viewport.get('width') or window.width)))
    sy=max(.1,float(f.ClientSize.Height)/max(1,float(viewport.get('height') or window.height)))
    return {'x':float(event.X)/sx,'y':float(event.Y)/sy},sx,sy
   def down(sender,event):
    try:
     if str(event.Button)!='Left':return
     cursor=f.PointToScreen(Point(event.X,event.Y))
     gesture.update(x=cursor.X,y=cursor.Y,left=form.Left,top=form.Top,moved=False,started=time.monotonic(),panel=panel)
     f.Capture=True;note('down')
    except Exception as error:gesture.clear();note('input-error',{'error':str(error)})
   def move(sender,event):
    try:
     if not gesture or not f.Capture:return
     cursor=f.PointToScreen(Point(event.X,event.Y));dx=cursor.X-gesture['x'];dy=cursor.Y-gesture['y'];scale=float(form._scale)
     if math.hypot(dx,dy)>7*scale:gesture['moved']=True
     if gesture['moved'] and not gesture['panel']:
      form.Location=Point(gesture['left']+dx,gesture['top']+dy);f.Bounds=form.Bounds
    except Exception as error:note('input-error',{'error':str(error)});gesture.clear();f.Capture=False
   def up(sender,event):
    try:
     if str(event.Button)!='Left' or not gesture:return
     moved=gesture['moved'];elapsed=time.monotonic()-gesture['started'];gesture.clear();f.Capture=False
     if moved:note('drag-complete');return
     if elapsed>.65:note('long-press-not-a-tap');return
     core=form.webview.CoreWebView2
     if core is None:note('input-unavailable',{'reason':'WEBVIEW_CORE_NOT_READY'});return
     data,sx,sy=event_point(event)
     note('nativeTap',{'cssX':data['x'],'cssY':data['y']})
     core.ExecuteScriptAsync('window.__leewayFloating?.nativeTap('+json.dumps(data)+')')
    except Exception as error:note('input-error',{'error':str(error)});gesture.clear();f.Capture=False
   def wheel(sender,event):
    try:
     if not panel:return
     core=form.webview.CoreWebView2
     if core is None:return
     data,sx,sy=event_point(event);data['dy']=-event.Delta/sy
     code='((p)=>{let n=document.elementFromPoint(p.x,p.y);while(n){const s=getComputedStyle(n);if(/auto|scroll/.test(s.overflowY)&&n.scrollHeight>n.clientHeight){n.scrollBy(0,p.dy);return;}n=n.parentElement;}})('
     core.ExecuteScriptAsync(code+json.dumps(data)+')');note('wheel',{'delta':event.Delta})
    except Exception as error:note('input-error',{'error':str(error)})
   f.MouseDown+=down;f.MouseMove+=move;f.MouseUp+=up;f.MouseWheel+=wheel
   input_plane=f;input_handlers=[down,move,up,wheel]
  input_plane.Bounds=form.Bounds
  prior_region=input_plane.Region;input_plane.Region=form.Region.Clone() if form.Region is not None else None
  if prior_region is not None:prior_region.Dispose()
  if form.Visible:
   if not input_plane.Visible:input_plane.Show(form)
   input_plane.BringToFront()
  else:input_plane.Hide()
 if form.InvokeRequired:form.BeginInvoke(Action(update))
 else:update()
def save():
 if not panel and not expanded and window:
  data={'x':window.x,'y':window.y,'width':window.width,'height':window.height};temp=state_path.with_suffix('.tmp');temp.write_text(json.dumps(data));os.replace(temp,state_path)
class HostApi:
 def geometry(self,action,value=0,second=0):
  global panel,expanded,normal
  with gate:
   if action=='move':
    dx=float(value);dy=float(second)
    if not math.isfinite(dx+dy) or abs(dx)>300 or abs(dy)>300:raise ValueError('MOVE_DELTA_INVALID')
    window.move(max(work['x'],min(work['x']+work['width']-window.width,int(window.x+dx))),max(work['y'],min(work['y']+work['height']-window.height,int(window.y+dy))))
   elif action=='resize':
    amount=int(value)
    if panel:return {'state':'PANEL_OWNS_GEOMETRY'}
    side=max(120,min(max_compact,amount));expanded=False;window.resize(side,side)
   elif action=='expand':
    if not expanded:normal={'x':window.x,'y':window.y,'width':window.width,'height':window.height};window.move(work['x'],work['y']);window.resize(work['width'],work['height']);expanded=True
    else:window.resize(normal['width'],normal['height']);window.move(normal['x'],normal['y']);expanded=False
   elif action=='panel':
    enabled=bool(value)
    if enabled and not panel:
     if not expanded:normal={'x':window.x,'y':window.y,'width':window.width,'height':window.height}
     panel=True;window.resize(min(760,work['width']),min(800,work['height']));window.move(max(work['x'],min(window.x,work['x']+work['width']-window.width)),max(work['y'],min(window.y,work['y']+work['height']-window.height)))
    elif not enabled and panel:
     panel=False
     if not expanded:window.resize(normal['width'],normal['height']);window.move(normal['x'],normal['y'])
   elif action=='status':pass
   else:raise ValueError('HOST_GEOMETRY_ACTION_NOT_ALLOWED')
   if action!='status':style_native();save()
   receipt('NATIVE_HOST_RUNNING');return {'x':window.x,'y':window.y,'width':window.width,'height':window.height,'maxCompact':max_compact,'panel':panel,'expanded':expanded}
 def listen(self,enabled=True):
  global recognizer,native_handlers
  if not enabled:
   if recognizer:
    try:recognizer.RecognizeAsyncCancel()
    except Exception:pass
   return {'state':'STOPPED'}
  if panel:return {'state':'BLOCKED_MENU_OPEN'}
  import clr
  assembly=binding['nativeAgentUi'].get('speechAssembly')
  if not assembly or hashlib.sha256(pathlib.Path(assembly['path']).read_bytes()).hexdigest()!=assembly['sha256']:raise RuntimeError('NATIVE_INPUT_ASSEMBLY_NOT_ADMITTED')
  clr.AddReference(assembly['path'])
  from System.Speech.Recognition import SpeechRecognitionEngine,DictationGrammar,RecognizeMode
  if recognizer is None:
   installed=list(SpeechRecognitionEngine.InstalledRecognizers());english=next((r for r in installed if str(r.Culture.Name)=='en-US'),None)
   if english is None:raise RuntimeError('NATIVE_SPEECH_RECOGNIZER_UNAVAILABLE')
   recognizer=SpeechRecognitionEngine(english);recognizer.LoadGrammar(DictationGrammar())
   def on_result(sender,event):emit('leeway:native-transcript',{'text':str(event.Result.Text),'confidence':float(event.Result.Confidence),'source':'WINDOWS_INSTALLED_SPEECH_RECOGNIZER'})
   def on_end(sender,event):emit('leeway:native-listening-ended',{'cancelled':bool(event.Cancelled)})
   recognizer.SpeechRecognized+=on_result;recognizer.RecognizeCompleted+=on_end;native_handlers=[on_result,on_end]
  recognizer.SetInputToDefaultAudioDevice();recognizer.RecognizeAsync(RecognizeMode.Single);return {'state':'LISTENING_OWNER_TAP'}
 def open_workstation(self):
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
 def open_voice_studio(self):
  record=binding['nativeAgentUi'].get('voiceStudio')
  if not record:raise RuntimeError('CANONICAL_VOICE_STUDIO_NOT_BOUND')
  parsed=urllib.parse.urlsplit(record['url'])
  if parsed.scheme!='http' or parsed.hostname not in ('127.0.0.1','localhost') or parsed.path!='/studio.html':raise RuntimeError('VOICE_STUDIO_LOCAL_ORIGIN_REQUIRED')
  with urllib.request.urlopen(record['url'],timeout=8) as response:content=response.read(1048577)
  if len(content)>1048576 or hashlib.sha256(content).hexdigest()!=record['sha256']:raise RuntimeError('VOICE_STUDIO_SOURCE_CHANGED')
  current=surface_windows.get('voice-studio')
  if current is not None and current in webview.windows:current.show();current.restore()
  else:surface_windows['voice-studio']=webview.create_window('Agent Lee - LeeWay Voice Studio',record['url'],width=min(1080,work['width']),height=min(820,work['height']),resizable=True,on_top=False,focus=True,text_select=True,js_api=None)
  receipt('CANONICAL_VOICE_STUDIO_OPENED',{'studioSha256':record['sha256'],'nativeBridgeExposed':False,'selectionChanged':False})
  return {'state':'CANONICAL_VOICE_STUDIO_OPENED','selectionSynchronization':'SEPARATE_ACCEPTANCE_REQUIRED'}
 def open_surface(self,relative):
  global surface_windows
  allowed={
   '/brain-ui/brain.html':('Agent Lee - Digital Brain',1040,820),
   '/brain-ui/brain.html?surface=hardware':('Agent Lee - Device Diagnostics',1040,820),
   '/settings-ui':('Agent Lee - Settings',900,720),
   '/voice-fabric/studio.html':('Agent Lee - LeeWay Voice',980,760)
  }
  if relative not in allowed:raise ValueError('SURFACE_NOT_ALLOWLISTED')
  current=surface_windows.get(relative)
  if current is not None and current in webview.windows:
   current.show();current.restore();return {'state':'RESTORED_EXISTING_SURFACE'}
  title,w,h=allowed[relative]
  class SurfaceApi:
   def close(self):
    current=surface_windows.pop(relative,None)
    if current is not None:
     try:current.destroy()
     except Exception:pass
    return {'state':'SURFACE_CLOSED_MAIN_AGENT_RETAINED'}
  surface_api=SurfaceApi()
  surface_windows[relative]=webview.create_window(title,origin+relative,width=min(w,work['width']),height=min(h,work['height']),resizable=True,frameless=False,on_top=True,focus=True,text_select=True,js_api=surface_api)
  receipt('SURFACE_WINDOW_OPENED',{'surface':relative,'mainAgentLeeReplaced':False})
  return {'state':'OPENED_SEPARATE_SURFACE_WINDOW'}
vt_window=None
surface_windows={}
api=HostApi()
window=webview.create_window('Agent Lee - Floating',url,js_api=api,width=initial['width'],height=initial['height'],x=initial['x'],y=initial['y'],frameless=True,easy_drag=False,resizable=True,min_size=(120,120),shadow=False,transparent=True,background_color='#ff00ff',on_top=True,focus=False)
def loaded():
 global native_ready
 try:
  first=not native_ready;native_ready=True
  if first:window.resize(initial['width'],initial['height'])
  if window.get_current_url()==url and panel:api.geometry('panel',False)
  style_native()
  window.evaluate_js("document.documentElement.dataset.nativeFloating='true';window.dispatchEvent(new Event('leeway:native-ready'))")
  receipt('NATIVE_PAGE_LOADED',{'pageBackground':window.evaluate_js('getComputedStyle(document.body).backgroundColor'),'nativeBackend':'edgechromium'})
 except Exception as e:receipt('NATIVE_PAGE_LOAD_FAILED',{'error':str(e)})
window.events.loaded+=loaded
window.events.closed+=lambda:closed.set()
window.events.resized+=lambda *args:style_native() if native_ready else None
def qualify_snapshot():
 import ctypes
 from ctypes import wintypes
 from PIL import ImageGrab
 from webview.platforms.winforms import BrowserView
 hwnd=BrowserView.instances[window.uid].Handle.ToInt64()
 prior=user32.SetThreadDpiAwarenessContext(ctypes.c_void_p(-4))
 r=wintypes.RECT();user32.GetWindowRect(hwnd,ctypes.byref(r))
 # Whole desktop here is only a capture source; retain the bounded overlay rectangle and nearby background.
 shot=ImageGrab.grab(all_screens=True)
 left=user32.GetSystemMetrics(76);top=user32.GetSystemMetrics(77)
 crop=shot.crop((r.left-left-10,r.top-top-10,r.right-left+10,r.bottom-top+10));crop.save(output/'native-floating-observed.png');user32.SetThreadDpiAwarenessContext(ctypes.c_void_p(prior))
 if native_ready:
  data=dict(last_ui_state);data['scope']='NATIVE_SCREEN_CAPTURE_WITH_ASYNC_PAGE_READBACK'
  (output/'page-observation.json').write_text(json.dumps(data,indent=2))
SNAPSHOT_SCRIPT = "({viewport:{width:innerWidth,height:innerHeight,dpr:devicePixelRatio},menu:document.getElementById('drawer')?.classList.contains('open'),hamburger:document.getElementById('hamburger-btn')?.getBoundingClientRect().toJSON(),close:document.getElementById('close-drawer')?.getBoundingClientRect().toJSON(),interaction:window.__leewayInteraction,shrink:document.getElementById('shrink-agent')?.getBoundingClientRect().toJSON(),grow:document.getElementById('grow-agent')?.getBoundingClientRect().toJSON(),expand:document.getElementById('expand-agent')?.getBoundingClientRect().toJSON(),workstation:document.querySelector('[data-setting=workstation]')?.getBoundingClientRect().toJSON(),controls:Object.fromEntries([...document.querySelectorAll('[data-setting]')].map(e=>[e.dataset.setting,e.getBoundingClientRect().toJSON()])),background:getComputedStyle(document.body).backgroundColor,bridgeReady:!!window.pywebview?.api,url:location.href,status:document.getElementById('status-text')?.textContent})"
def post_visibility(action):
 from System import Action
 from System.Windows.Forms import FormWindowState
 from webview.platforms.winforms import BrowserView
 form=BrowserView.instances[window.uid]
 def apply():
  try:
   before=bool(form.Visible);target=True if action=='show' else not before
   if target:form.Show();form.WindowState=FormWindowState.Normal;form.BringToFront()
   else:form.Hide()
   style_native()
   visibility_observation.update(before=before,after=bool(form.Visible),requested=target,at=time.time(),state='NATIVE_VISIBILITY_READBACK')
  except Exception as error:visibility_observation.update(state='FAILED',error=str(error),at=time.time())
 form.BeginInvoke(Action(apply))
def poll_page_async():
 global last_ui_state,last_page_observed
 from System import Action
 from webview.platforms.winforms import BrowserView
 form=BrowserView.instances[window.uid]
 with page_probe_lock:
  task=page_probe['task']
  if task is not None:
   if not task.IsCompleted:
    if time.monotonic()-page_probe['at']>8:page_probe['error']='PAGE_OBSERVATION_TIMED_OUT_OWNER_CONTROLS_REMAIN_AVAILABLE'
    return
   try:
    if task.IsFaulted or task.IsCanceled:raise RuntimeError('WEBVIEW_SNAPSHOT_TASK_FAILED')
    result=json.loads(str(task.Result))
    if not isinstance(result,dict):raise RuntimeError('WEBVIEW_SNAPSHOT_NOT_OBJECT')
    last_ui_state=result;last_page_observed=time.time();page_probe['error']=None
   except Exception as error:page_probe['error']=str(error)
   page_probe['task']=None
  if page_probe['queued']:return
  page_probe['queued']=True;page_probe['at']=time.monotonic()
 def launch():
  with page_probe_lock:
   try:
    if form.Visible and form.webview.CoreWebView2 is not None:
     page_probe['task']=form.webview.CoreWebView2.ExecuteScriptAsync(SNAPSHOT_SCRIPT);page_probe['at']=time.monotonic()
   except Exception as error:page_probe['error']=str(error)
   finally:page_probe['queued']=False
 try:form.BeginInvoke(Action(launch))
 except Exception:
  with page_probe_lock:page_probe['queued']=False
  raise
def observe():
 captured=False
 while not closed.wait(.25):
  try:
   if kernel32.WaitForSingleObject(ctypes.c_void_p(wake),0)==0:post_visibility('show')
   if kernel32.WaitForSingleObject(ctypes.c_void_p(toggle),0)==0:post_visibility('toggle')
   if native_ready and time.monotonic()-page_probe.get('lastPoll',0)>=1:
    page_probe['lastPoll']=time.monotonic();poll_page_async();receipt('NATIVE_HOST_RUNNING')
   if native_ready and last_page_observed and not captured and time.monotonic()-started>6:qualify_snapshot();captured=True
  except Exception as error:
   page_probe['error']=type(error).__name__+': '+str(error);receipt('NATIVE_HOST_OBSERVATION_FAILED')
  if a.probe_seconds and time.monotonic()-started>a.probe_seconds:window.destroy();break
started=time.monotonic()
webview.settings['ALLOW_FILE_URLS']=False
try:webview.start(observe,gui='edgechromium',private_mode=False,storage_path=str(binding_path.parent/'native-ui-webview-state'),debug=False)
finally:
 closed.set()
 if recognizer:
  try:recognizer.Dispose()
  except Exception:pass
 kernel32.CloseHandle(ctypes.c_void_p(toggle));kernel32.CloseHandle(ctypes.c_void_p(wake));kernel32.CloseHandle(ctypes.c_void_p(lock))




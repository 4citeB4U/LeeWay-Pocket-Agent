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
lock=kernel32.CreateMutexW(None,False,'Local\\LeeWayAgentLeeFloatingHost')
if kernel32.GetLastError()==183:
 kernel32.SetEvent(ctypes.c_void_p(wake));raise SystemExit(0)
# Let the existing pywebview adapter establish DPI awareness and convert logical geometry.
screens=webview.screens;screen=next((item for item in screens if item.x==0 and item.y==0),screens[0])
work={'x':screen.x,'y':screen.y,'width':screen.width,'height':screen.height}
max_compact=int(math.sqrt(work['width']*work['height']*.15));size=min(340,max_compact,int(min(work['width'],work['height'])*.6));size=max(140,size)
initial={'width':size,'height':size,'x':work['x']+work['width']-size-88,'y':work['y']+int(work['height']*.2)}
if state_path.exists():
 try:
  saved=json.loads(state_path.read_text(encoding='utf-8-sig'));s=max(120,min(max_compact,int(saved['width'])));initial.update(width=s,height=s,x=int(saved['x']),y=int(saved['y']))
 except Exception:pass
initial['x']=max(work['x'],min(work['x']+work['width']-initial['width'],initial['x']));initial['y']=max(work['y'],min(work['y']+work['height']-initial['height'],initial['y']))
window=None;recognizer=None;native_handlers=[];panel=False;expanded=False;normal=dict(initial);closed=threading.Event();gate=threading.RLock();write_lock=threading.Lock();native_ready=False;input_plane=None;input_handlers=[];last_ui_state={};last_mouse=[]
def receipt(status,extra=None):
 value={'schemaVersion':'leeway.native-floating-host.v1','status':status,'pid':os.getpid(),'provider':'LEEWAY_DEVICE_BRIDGE_NATIVE_UI','url':url,'screen':work,'topmostRequested':True,'transparentRequested':True,'framelessRequested':True,'nativeHitPlane':input_plane is not None,'panelOpen':panel,'expandedByOwner':expanded,'fixedPhysicalPathsInSource':False,'formulaExecution':'NOT_EXECUTED'}
 if window:value['geometry']={'x':window.x,'y':window.y,'width':window.width,'height':window.height,'screenAreaRatio':window.width*window.height/(work['width']*work['height'])}
 value['ownPage']=last_ui_state;value['ownMouse']=last_mouse[-8:]
 if extra:value.update(extra)
 with write_lock:
  temp=output/'host-receipt.tmp';temp.write_text(json.dumps(value,indent=2));os.replace(temp,output/'host-receipt.json')
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
    last_mouse.append({'event':'nativeDown','screenX':cursor.X,'screenY':cursor.Y,'mode':mode,'at':time.time()});del last_mouse[:-8]
    gesture.update(x=cursor.X,y=cursor.Y,left=form.Left,top=form.Top,width=form.Width,height=form.Height,mode=mode,moved=False,started=time.monotonic())
   def move(sender,event):
    if not gesture:return
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
    # Native pointer owns the gesture; browser events are not replayed while capture is active.
    return
   def up(sender,event):
    if str(event.Button)!='Left':return
    last_mouse.append({'event':'nativeUp','moved':bool(gesture.get('moved')),'at':time.time()});del last_mouse[:-8]
    moved=bool(gesture.get('moved'));elapsed=time.monotonic()-gesture.get('started',time.monotonic())
    if not moved and elapsed<.65:
     cursor=Forms.Cursor.Position;pt=form.webview.PointToClient(cursor);sc=float(form._scale)
     data={'x':pt.X/sc,'y':pt.Y/sc}
     last_mouse.append({'event':'nativeTap','cssX':data['x'],'cssY':data['y'],'at':time.time()});del last_mouse[:-8]
     form.webview.CoreWebView2.ExecuteScriptAsync('window.__leewayFloating?.nativeTap('+json.dumps(data)+')')
    f.Capture=False;gesture.clear()
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
 def open_surface(self,relative):
  if relative not in ['/brain-ui/brain.html','/brain-ui/brain.html?surface=hardware','/settings-ui','/voice-fabric/studio.html']:raise ValueError('SURFACE_NOT_ALLOWLISTED')
  self.geometry('panel',True);threading.Timer(.2,lambda:window.load_url(origin+relative)).start();return {'state':'OPENED_EXISTING_SURFACE'}
vt_window=None
api=HostApi()
window=webview.create_window('Agent Lee - Floating',url,js_api=api,width=initial['width'],height=initial['height'],x=initial['x'],y=initial['y'],frameless=True,easy_drag=False,resizable=True,min_size=(120,120),shadow=False,transparent=True,background_color='#000000',on_top=True,focus=False)
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
  data=window.evaluate_js('({title:document.title,background:getComputedStyle(document.body).backgroundColor,duplicateTabs:document.querySelectorAll("#agent-emblem,.agent-side-tab").length,pointerAdapter:Boolean(window.__leewayFloating)})')
  (output/'page-observation.json').write_text(json.dumps(data,indent=2))
def observe():
 global last_ui_state
 captured=False
 while not closed.wait(2):
  try:
   if kernel32.WaitForSingleObject(ctypes.c_void_p(wake),0)==0:
    api.geometry('panel',False);window.show();window.restore();style_native()
   if native_ready:
    last_ui_state=window.evaluate_js("({viewport:{width:innerWidth,height:innerHeight,dpr:devicePixelRatio},menu:document.getElementById('drawer')?.classList.contains('open'),hamburger:document.getElementById('hamburger-btn')?.getBoundingClientRect().toJSON(),close:document.getElementById('close-drawer')?.getBoundingClientRect().toJSON(),interaction:window.__leewayInteraction,shrink:document.getElementById('shrink-agent')?.getBoundingClientRect().toJSON(),grow:document.getElementById('grow-agent')?.getBoundingClientRect().toJSON(),expand:document.getElementById('expand-agent')?.getBoundingClientRect().toJSON(),workstation:document.querySelector('[data-setting=workstation]')?.getBoundingClientRect().toJSON(),url:location.href,status:document.getElementById('status-text')?.textContent})")
   receipt('NATIVE_HOST_RUNNING')
   if native_ready and not captured and time.monotonic()-started>6:qualify_snapshot();captured=True
  except Exception:pass
  if a.probe_seconds and time.monotonic()-started>a.probe_seconds:window.destroy();break
started=time.monotonic()
webview.settings['ALLOW_FILE_URLS']=False
try:webview.start(observe,gui='edgechromium',private_mode=False,storage_path=str(binding_path.parent/'native-ui-webview-state'),debug=False)
finally:
 closed.set()
 if recognizer:
  try:recognizer.Dispose()
  except Exception:pass
 kernel32.CloseHandle(ctypes.c_void_p(wake));kernel32.CloseHandle(ctypes.c_void_p(lock))

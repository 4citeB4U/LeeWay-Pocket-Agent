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
lock=kernel32.CreateMutexW(None,False,'Local\\LeeWayAgentLeeFloatingHost')
if kernel32.GetLastError()==183:raise SystemExit(0)
# Let the existing pywebview adapter establish DPI awareness and convert logical geometry.
screens=webview.screens;screen=next((item for item in screens if item.x==0 and item.y==0),screens[0])
work={'x':screen.x,'y':screen.y,'width':screen.width,'height':screen.height}
max_compact=int(math.sqrt(work['width']*work['height']*.15));size=min(340,max_compact,int(min(work['width'],work['height'])*.6));size=max(140,size)
initial={'width':size,'height':size,'x':work['x']+work['width']-size-88,'y':work['y']+int(work['height']*.2)}
if state_path.exists():
 try:
  saved=json.loads(state_path.read_text());s=max(120,min(max_compact,int(saved['width'])));initial.update(width=s,height=s,x=int(saved['x']),y=int(saved['y']))
 except Exception:pass
initial['x']=max(work['x'],min(work['x']+work['width']-initial['width'],initial['x']));initial['y']=max(work['y'],min(work['y']+work['height']-initial['height'],initial['y']))
window=None;recognizer=None;native_handlers=[];panel=False;expanded=False;normal=dict(initial);closed=threading.Event();gate=threading.RLock();write_lock=threading.Lock();native_ready=False
def receipt(status,extra=None):
 value={'schemaVersion':'leeway.native-floating-host.v1','status':status,'pid':os.getpid(),'provider':'LEEWAY_DEVICE_BRIDGE_NATIVE_UI','url':url,'screen':work,'topmostRequested':True,'transparentRequested':True,'framelessRequested':True,'panelOpen':panel,'expandedByOwner':expanded,'fixedPhysicalPathsInSource':False,'formulaExecution':'NOT_EXECUTED'}
 if window:value['geometry']={'x':window.x,'y':window.y,'width':window.width,'height':window.height,'screenAreaRatio':window.width*window.height/(work['width']*work['height'])}
 if extra:value.update(extra)
 with write_lock:
  temp=output/'host-receipt.tmp';temp.write_text(json.dumps(value,indent=2));os.replace(temp,output/'host-receipt.json')
def emit(kind,detail):
 if window and not closed.is_set():
  try:window.evaluate_js('window.dispatchEvent(new CustomEvent('+json.dumps(kind)+',{detail:'+json.dumps(detail)+'}))')
  except Exception:pass
def style_native():
 if not native_ready:return
 from System import Action
 from System.Drawing import Color,Region,Rectangle
 from System.Drawing.Drawing2D import GraphicsPath
 from webview.platforms.winforms import BrowserView
 form=BrowserView.instances[window.uid]
 def update():
  form.AllowTransparency=True;form.BackColor=Color.FromArgb(1,2,3);form.TransparencyKey=Color.FromArgb(1,2,3);form.TopMost=True
  form.webview.DefaultBackgroundColor=Color.Transparent
  if panel or expanded:form.Region=None
  else:
   w,h=form.ClientSize.Width,form.ClientSize.Height
   shape=GraphicsPath();shape.AddEllipse(2,2,w-4,h-30)
   shape.AddRectangle(Rectangle(0,0,min(70,w),min(70,h)))
   shape.AddRectangle(Rectangle(0,max(0,h-75),w,75))
   old=form.Region;form.Region=Region(shape);shape.Dispose()
   if old is not None:old.Dispose()
 form.Invoke(Action(update))
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
   style_native();save();receipt('NATIVE_HOST_RUNNING');return {'x':window.x,'y':window.y,'width':window.width,'height':window.height,'maxCompact':max_compact,'panel':panel,'expanded':expanded}
 def listen(self,enabled=True):
  global recognizer,native_handlers
  if not enabled:
   if recognizer:
    try:recognizer.RecognizeAsyncCancel()
    except Exception:pass
   return {'state':'STOPPED'}
  if panel:return {'state':'BLOCKED_MENU_OPEN'}
  import clr;clr.AddReference('System.Speech')
  from System.Speech.Recognition import SpeechRecognitionEngine,DictationGrammar,RecognizeMode
  if recognizer is None:
   installed=list(SpeechRecognitionEngine.InstalledRecognizers());english=next((r for r in installed if str(r.Culture.Name)=='en-US'),None)
   if english is None:raise RuntimeError('NATIVE_SPEECH_RECOGNIZER_UNAVAILABLE')
   recognizer=SpeechRecognitionEngine(english);recognizer.LoadGrammar(DictationGrammar())
   def on_result(sender,event):emit('leeway:native-transcript',{'text':str(event.Result.Text),'confidence':float(event.Result.Confidence),'source':'WINDOWS_INSTALLED_SPEECH_RECOGNIZER'})
   def on_end(sender,event):emit('leeway:native-listening-ended',{'cancelled':bool(event.Cancelled)})
   recognizer.SpeechRecognized+=on_result;recognizer.RecognizeCompleted+=on_end;native_handlers=[on_result,on_end]
  recognizer.SetInputToDefaultAudioDevice();recognizer.RecognizeAsync(RecognizeMode.Single);return {'state':'LISTENING_OWNER_TAP'}
 def open_surface(self,relative):
  if relative not in ['/brain-ui/brain.html','/brain-ui/brain.html?surface=hardware','/settings-ui','/voice-fabric/studio.html']:raise ValueError('SURFACE_NOT_ALLOWLISTED')
  self.geometry('panel',True);window.load_url(origin+relative);return {'state':'OPENED_EXISTING_SURFACE'}
api=HostApi()
window=webview.create_window('Agent Lee - Floating',url,js_api=api,width=initial['width'],height=initial['height'],x=initial['x'],y=initial['y'],frameless=True,easy_drag=False,resizable=True,min_size=(120,120),shadow=False,transparent=True,background_color='#000000',on_top=True,focus=False)
def loaded():
 global native_ready
 try:
  native_ready=True;style_native()
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
 captured=False
 while not closed.wait(2):
  try:
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
 kernel32.CloseHandle(ctypes.c_void_p(lock))

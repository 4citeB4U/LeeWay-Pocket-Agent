"""REGION: LEEWAY.UI.LIVE_TEST; TAG: ACTUAL_NATIVE_POINTER_NOT_DOM_FIXTURE
WHO: Creator-authorized qualification. WHAT: Probe actual installed windows with bounded OS input.
WHEN: Before/after the input repair. WHERE: Only the PID in the existing native host receipt.
WHY: API calls and browser fixtures do not prove user clicks. HOW: Verified WindowFromPoint, cursor input,
readback from own host, bounded screenshots. No commands, credentials or user documents are entered.
LICENSE: MIT.
"""
import argparse,ctypes,json,pathlib,time
from ctypes import wintypes
from PIL import ImageGrab
p=argparse.ArgumentParser();p.add_argument('--receipt',required=True);p.add_argument('--out',required=True);p.add_argument('--action',choices=['observe','menu','close','shrink','grow','drag','expand','sphere','workstation'],default='observe');a=p.parse_args()
u=ctypes.windll.user32
u.SetProcessDpiAwarenessContext(ctypes.c_void_p(-4));u.WindowFromPoint.argtypes=[wintypes.POINT];u.WindowFromPoint.restype=wintypes.HWND;u.GetAncestor.argtypes=[wintypes.HWND,wintypes.UINT];u.GetAncestor.restype=wintypes.HWND;u.GetWindowThreadProcessId.argtypes=[wintypes.HWND,ctypes.POINTER(wintypes.DWORD)]
receipt=pathlib.Path(a.receipt);out=pathlib.Path(a.out);out.mkdir(parents=True,exist_ok=True)
def state():
 for _ in range(20):
  try:return json.loads(receipt.read_text())
  except Exception:time.sleep(.1)
 raise RuntimeError('CURRENT_HOST_STATE_UNREADABLE')
def find(pid,title='Agent Lee - Floating'):
 matches=[]
 @ctypes.WINFUNCTYPE(wintypes.BOOL,wintypes.HWND,wintypes.LPARAM)
 def cb(h,unused):
  n=wintypes.DWORD();u.GetWindowThreadProcessId(h,ctypes.byref(n));s=ctypes.create_unicode_buffer(512);u.GetWindowTextW(h,s,512)
  if n.value==pid and s.value==title and u.IsWindowVisible(h):matches.append(h)
  return True
 u.EnumWindows(cb,0);assert len(matches)==1,'VISIBLE_NATIVE_WINDOW_AMBIGUOUS_OR_MISSING';return matches[0]
def rectangle(h):
 r=wintypes.RECT();u.GetWindowRect(h,ctypes.byref(r));return {'left':r.left,'top':r.top,'right':r.right,'bottom':r.bottom}
def point(h,x,y,view):
 r=wintypes.RECT();u.GetWindowRect(h,ctypes.byref(r));return int(r.left+x*(r.right-r.left)/view['width']),int(r.top+y*(r.bottom-r.top)/view['height'])
def click(x,y,pid):
 u.SetCursorPos(x,y);time.sleep(.15);h=u.WindowFromPoint(wintypes.POINT(x,y));n=wintypes.DWORD();u.GetWindowThreadProcessId(u.GetAncestor(h,2),ctypes.byref(n));assert n.value==pid,'TARGET_POINT_NOT_IN_VERIFIED_AGENT_HOST_WINDOW_TREE'
 u.mouse_event(2,0,0,0,0);time.sleep(.08);u.mouse_event(4,0,0,0,0)
before=state();pid=before['pid'];h=find(pid);rect=rectangle(h);checks=[];error=None
try:
 v=before['ownPage']['viewport'];action=a.action
 if action in ['menu','close','shrink','grow','expand','workstation']:
  key={'menu':'hamburger','close':'close','shrink':'shrink','grow':'grow','expand':'expand','workstation':'workstation'}[action];r=before['ownPage'].get(key)
  if not r:
   if action in ['shrink','grow','expand']:
    pos={'shrink':v['width']-8-4*31+14,'grow':v['width']-8-3*31+14,'expand':v['width']-8-2*31+14}[action];r={'x':pos-14,'y':v['height']-58,'width':28,'height':28}
   else:raise RuntimeError('OBSERVED_CONTROL_BOUNDS_REQUIRED')
  assert 0<=r['x']<v['width'] and 0<=r['y']<v['height'],'CONTROL_OFFSCREEN'
  xy=point(h,r['x']+r['width']/2,r['y']+r['height']/2,v);click(*xy,pid);checks.append({'action':action,'actualOsClick':xy})
 elif action=='sphere':
  xy=point(h,v['width']/2,v['height']/2,v);click(*xy,pid);checks.append({'action':'sphere','actualOsClick':xy})
 elif action=='drag':
  x,y=point(h,v['width']/2,v['height']/2,v);u.SetCursorPos(x,y);time.sleep(.2);owner=wintypes.DWORD();u.GetWindowThreadProcessId(u.WindowFromPoint(wintypes.POINT(x,y)),ctypes.byref(owner));assert owner.value==pid
  u.mouse_event(2,0,0,0,0)
  try:
   for i in range(1,17):u.SetCursorPos(x+int(120*i/16),y+int(60*i/16));time.sleep(.045)
  finally:u.mouse_event(4,0,0,0,0)
  checks.append({'action':'drag','physicalDelta':[120,60]})
 time.sleep(3);after=state();h=find(pid);newrect=rectangle(h)
 if action=='menu':assert after['ownPage']['menu'] and after['panelOpen'],'MENU_DID_NOT_OPEN'
 if action=='close':assert not after['ownPage']['menu'] and not after['panelOpen'],'MENU_DID_NOT_CLOSE'
 if action=='drag':assert 70<=newrect['left']-rect['left']<=160 and 20<=newrect['top']-rect['top']<=100,'DRAG_NOT_TRACKING_ACTUAL_CURSOR'
 if action=='shrink':assert after['geometry']['width']<before['geometry']['width'],'SHRINK_FAILED'
 if action=='grow':assert after['geometry']['width']>before['geometry']['width'],'GROW_FAILED'
 if action=='sphere':assert after['ownPage']['interaction']['sphereTaps']>before['ownPage']['interaction']['sphereTaps'],'SPHERE_CLICK_NOT_RECEIVED'
except Exception as e:error=str(e);after=state();newrect=rectangle(find(pid))
shot=ImageGrab.grab(all_screens=True);left=u.GetSystemMetrics(76);top=u.GetSystemMetrics(77);shot.crop((newrect['left']-left-8,newrect['top']-top-8,newrect['right']-left+8,newrect['bottom']-top+8)).save(out/(a.action+'.png'))
result={'scope':'ACTUAL_OS_POINTER_ON_INSTALLED_AGENT','status':'FAILED' if error else 'PASS_SCOPED_NATIVE_ACTION','action':a.action,'before':before,'after':after,'rectBefore':rect,'rectAfter':newrect,'checks':checks,'error':error,'fixtureInput':False,'phoneTested':False}
(out/(a.action+'.json')).write_text(json.dumps(result,indent=2));print(json.dumps({'status':result['status'],'action':a.action,'checks':checks,'error':error,'rectBefore':rect,'rectAfter':newrect,'menu':after.get('ownPage',{}).get('menu')},indent=2))
raise SystemExit(1 if error else 0)

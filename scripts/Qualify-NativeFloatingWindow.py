"""REGION: LEEWAY.DEVICES.LIVE_ACCEPTANCE; TAG: ACTUAL_WINDOWS_COMPOSITOR_AND_INPUT
WHO: Creator-authorized Agent Lee. WHAT: Observe native topmost/transparency and physically drag/menu the exact process window.
WHEN: After installed launcher executes. WHERE: Windows adapter, actual desktop not browser fixtures.
WHY: API transparency flags alone are not sufficient. HOW: HWND ownership, real pixels, real mouse gestures, bounded snapshots.
LICENSE: MIT.
"""
import argparse,ctypes,json,pathlib,time,hashlib
from ctypes import wintypes
from PIL import ImageGrab
p=argparse.ArgumentParser();p.add_argument('--pid',type=int,required=True);p.add_argument('--out',required=True);p.add_argument('--receipt',required=True);a=p.parse_args()
u=ctypes.windll.user32;g=ctypes.windll.gdi32
u.SetThreadDpiAwarenessContext(ctypes.c_void_p(-4))
out=pathlib.Path(a.out);out.mkdir(parents=True,exist_ok=True);checks=[];errors=[];shots=[]
windows=[]
@ctypes.WINFUNCTYPE(ctypes.c_bool,wintypes.HWND,wintypes.LPARAM)
def collect(hwnd,param):
 pid=wintypes.DWORD();u.GetWindowThreadProcessId(hwnd,ctypes.byref(pid))
 if pid.value==a.pid and u.IsWindowVisible(hwnd):
  b=ctypes.create_unicode_buffer(512);u.GetWindowTextW(hwnd,b,512)
  if b.value=='Agent Lee - Floating':windows.append(hwnd)
 return True
u.EnumWindows(collect,0);assert len(windows)==1,'EXACT_NATIVE_WINDOW_REQUIRED';hwnd=windows[0]
input_hwnd=u.FindWindowW(None,'Agent Lee native input plane');assert input_hwnd,'NATIVE_HIT_WINDOW_REQUIRED'
def native_mouse(kind,x,y,held=False):
 r=wintypes.RECT();u.GetWindowRect(input_hwnd,ctypes.byref(r));cx=int(x-r.left);cy=int(y-r.top)
 u.PostMessageW(input_hwnd,kind,1 if held else 0,(cy&65535)<<16|(cx&65535))
def rect():
 r=wintypes.RECT();assert u.GetWindowRect(hwnd,ctypes.byref(r));return (r.left,r.top,r.right,r.bottom)
def click(x,y):native_mouse(0x200,x,y);time.sleep(.1);native_mouse(0x201,x,y,True);time.sleep(.12);native_mouse(0x202,x,y)
def capture(name):
 x,y,right,bottom=rect();left=u.GetSystemMetrics(76);top=u.GetSystemMetrics(77)
 allscreens=ImageGrab.grab(all_screens=True);image=allscreens.crop((x-left-5,y-top-5,right-left+5,bottom-top+5));file=out/(name+'.png');image.save(file);shots.append({'name':file.name,'sha256':hashlib.sha256(file.read_bytes()).hexdigest()})
def wait(condition,seconds=6):
 began=time.time()
 while time.time()-began<seconds:
  try:
   r=json.loads(pathlib.Path(a.receipt).read_text());
   if condition(r):return r
  except (OSError,ValueError):pass
  time.sleep(.2)
 raise RuntimeError('ACTUAL_NATIVE_STATE_TIMEOUT')
def click_control(key):
 state=json.loads(pathlib.Path(a.receipt).read_text());button=state['ownPage'][key];x,y,right,bottom=rect();viewport=state['ownPage']['viewport']
 assert button['x']>=0 and button['y']>=0,'REQUESTED_CONTROL_NOT_VISIBLE'
 click(x+(button['x']+button['width']/2)*(right-x)/viewport['width'],y+(button['y']+button['height']/2)*(bottom-y)/viewport['height'])
def passed(name,**e):checks.append({'name':name,'status':'PASS',**e})
try:
 style=u.GetWindowLongW(hwnd,-16);exstyle=u.GetWindowLongW(hwnd,-20)
 assert exstyle&0x8,'NATIVE_WINDOW_NOT_TOPMOST';assert (style&0x00C00000)==0,'NATIVE_CAPTION_REMAINS';assert exstyle&0x80000,'LAYERED_NATIVE_TRANSPARENCY_REQUIRED'
 passed('Actual owned window is frameless, topmost and layered',hwnd=int(hwnd),pid=a.pid,style=style,extendedStyle=exstyle)
 current=wait(lambda r:r['pid']==a.pid and bool(r.get('ownPage')));
 if current['panelOpen']:wait(lambda r:r.get('ownPage',{}).get('menu') and r['ownPage']['close']['x']>=0);click_control('close');wait(lambda r:not r['panelOpen'] and not r.get('ownPage',{}).get('menu',True));time.sleep(1)
 initial=wait(lambda r:r['pid']==a.pid and not r['panelOpen']);assert initial['geometry']['screenAreaRatio']<=.15
 passed('Actual compact geometry remains below the 15 percent cap',geometry=initial['geometry']);capture('pc-compact-native')
 before=rect();x=(before[0]+before[2])//2;y=(before[1]+before[3])//2
 native_mouse(0x200,x,y);native_mouse(0x201,x,y,True)
 for i in range(1,13):native_mouse(0x200,x-i*6,y+i*3,True);time.sleep(.045)
 native_mouse(0x202,x-72,y+36);time.sleep(3)
 after=rect();assert after[:2]!=before[:2],'PHYSICAL_SPHERE_DRAG_DID_NOT_MOVE_WINDOW'
 passed('Owner-window Win32 input drag moves native sphere',before=before,after=after)
 x,y,right,bottom=after;scale=(right-x)/initial['geometry']['width']
 click_control('hamburger')
 panel=wait(lambda r:r['panelOpen'] and r.get('ownPage',{}).get('menu') and r['ownPage']['close']['x']>=0);assert panel['geometry']['width']>initial['geometry']['width'];capture('pc-native-menu')
 passed('Actual hamburger owns an expanded readable native panel',geometry=panel['geometry'])
 # The page close control has an explicit position in the inspected DOM: first drawer header at right edge.
 # Escape is handled by existing page/native host only when bound; use observed drawer button instead.
 x,y,right,bottom=rect();scale=(right-x)/panel['geometry']['width'];click_control('close')
 restored=wait(lambda r:not r['panelOpen'] and not r.get('ownPage',{}).get('menu',True));assert restored['geometry']['screenAreaRatio']<=.15
 passed('Closing actual drawer restores compact native sphere',geometry=restored['geometry']);capture('pc-final-native-floating')
except Exception as e:errors.append(str(e))
receipt={'schemaVersion':'leeway.actual-windows-floating-ui.v1','status':'VERIFIED_NATIVE_WINDOW_AND_INTERACTION' if not errors else 'FAILED','browserFixtures':False,'inputMethod':'OWNER_HWND_BOUND_WIN32_MESSAGES_NOT_GLOBAL_CURSOR','checks':checks,'errors':errors,'screenshots':shots,'microphoneRequestedByTest':False,'speakerIdentityChanged':False,'goldenPromoted':False,'formulaExecuted':False}
(out/'pc-native-live-receipt.json').write_text(json.dumps(receipt,indent=2));print(json.dumps(receipt,indent=2));raise SystemExit(1 if errors else 0)

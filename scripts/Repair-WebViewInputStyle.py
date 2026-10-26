"""REGION: LEEWAY.DEVICE.UI.REPAIR; TAG: SINGLE_WEBVIEW_INPUT_STYLE
WHO: Creator-authorized engineering. WHAT: Restore mouse input to Agent Lee's existing WebView child.
WHEN: Physical hit-test observed Chrome_RenderWidgetHostHWND with WS_EX_TRANSPARENT.
WHERE: Existing LeeWay Desktop Commander native UI host only.
WHY: Visible UI cannot be interactive while its render child is transparent to hit testing.
HOW: During the existing composition pass, enumerate descendants of the verified Agent Lee form and
clear only WS_EX_TRANSPARENT (0x20). No second window or hit plane is created.
LICENSE: MIT
"""
import argparse,pathlib,hashlib,shutil,json
p=argparse.ArgumentParser();p.add_argument('--host',required=True);p.add_argument('--binding',required=True);p.add_argument('--out',required=True);p.add_argument('--apply',action='store_true');a=p.parse_args()
host=pathlib.Path(a.host);binding=pathlib.Path(a.binding);out=pathlib.Path(a.out)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
b=json.loads(binding.read_text(encoding='utf-8-sig'))
assert pathlib.Path(b['nativeAgentUi']['source']).resolve()==host.resolve(),'BOUND_HOST_MISMATCH'
assert sha(host)==b['nativeAgentUi']['sha256'],'BOUND_HOST_HASH_MISMATCH'
s=host.read_text(encoding='utf-8-sig')
anchor="def configure_composition(target,topmost=False,on_complete=None):\n"
helper=r'''def restore_webview_input(form,observation=None):
 parent=wintypes.HWND(form.Handle.ToInt64());changed=[];seen=[]
 enum_type=ctypes.WINFUNCTYPE(wintypes.BOOL,wintypes.HWND,wintypes.LPARAM)
 @enum_type
 def visit(hwnd,_):
  cls=ctypes.create_unicode_buffer(256);user32.GetClassNameW(hwnd,cls,256)
  before=user32.GetWindowLongW(hwnd,-20)&0xffffffff;after=before
  if before&0x20:
   after=before&~0x20
   user32.SetWindowLongW(hwnd,-20,ctypes.c_long(after).value)
  seen.append({'hwnd':int(hwnd),'class':cls.value,'before':hex(before),'after':hex(after)})
  if after!=before:changed.append(int(hwnd))
  return True
 user32.EnumChildWindows(parent,visit,0)
 if observation is not None:
  observation['inputStyleReadback']=seen
  observation['inputTransparentChildrenCleared']=changed
 return {'children':seen,'changed':changed}
'''
if 'def restore_webview_input(form,observation=None):' not in s:
    if s.count(anchor)!=1:raise SystemExit('COMPOSITION_ANCHOR_CHANGED')
    s=s.replace(anchor,helper+'\n'+anchor)
old="observation['stage']='INPUT_AND_INVALIDATE';user32.SetWindowLongW(handle,-20,user32.GetWindowLongW(handle,-20)&~0x08000000)\n   form.Invalidate();form.webview.Invalidate()"
new="observation['stage']='INPUT_AND_INVALIDATE';user32.SetWindowLongW(handle,-20,user32.GetWindowLongW(handle,-20)&~0x08000000);restore_webview_input(form,observation)\n   form.Invalidate();form.webview.Invalidate()"
if old in s:s=s.replace(old,new,1)
elif 'restore_webview_input(form,observation)' not in s:raise SystemExit('INPUT_STAGE_BOUNDARY_CHANGED')
plan={'status':'DRY_RUN','before':sha(host),'binding':sha(binding),'voiceAuthorityChanged':False,'newWindow':False,'clearsOnly':'WS_EX_TRANSPARENT_0x20_ON_EXISTING_CHILDREN'}
if a.apply:
 out.mkdir(parents=True,exist_ok=False);shutil.copy2(host,out/'agent-ui-host.before.py');shutil.copy2(binding,out/'binding.before.json')
 host.write_text(s,encoding='utf8',newline='\n');b['nativeAgentUi']['sha256']=sha(host);binding.write_text(json.dumps(b,indent=2),encoding='utf8')
 plan.update(status='APPLIED_RESTART_REQUIRED',after=sha(host));(out/'receipt.json').write_text(json.dumps(plan,indent=2),encoding='utf8')
print(json.dumps(plan,indent=2))
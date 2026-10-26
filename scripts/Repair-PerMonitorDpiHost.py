"""REGION: LEEWAY.DEVICE.UI.REPAIR; TAG: PER_MONITOR_V2_INPUT_GEOMETRY
WHO: Creator-authorized engineering. WHAT: Put WebView2 and native geometry in one DPI coordinate contract.
WHEN: Physical cursor hits are displaced on a 150% multi-monitor Windows display.
WHERE: Existing LeeWay Desktop Commander native host only.
WHY: pywebview defaults to SetProcessDPIAware (system-DPI); LeeWay uses physical/per-monitor-v2 geometry.
HOW: Set PROCESS_PER_MONITOR_DPI_AWARE_V2 before any WebView window is created.
LICENSE: MIT
"""
import argparse,pathlib,hashlib,shutil,json
p=argparse.ArgumentParser();p.add_argument('--host',required=True);p.add_argument('--binding',required=True);p.add_argument('--out',required=True);p.add_argument('--apply',action='store_true');a=p.parse_args()
host=pathlib.Path(a.host);binding=pathlib.Path(a.binding);out=pathlib.Path(a.out)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
b=json.loads(binding.read_text(encoding='utf-8-sig'));assert pathlib.Path(b['nativeAgentUi']['source']).resolve()==host.resolve();assert sha(host)==b['nativeAgentUi']['sha256']
s=host.read_text(encoding='utf-8-sig')
anchor="user32.SetThreadDpiAwarenessContext.argtypes=[ctypes.c_void_p]\nuser32.SetThreadDpiAwarenessContext.restype=ctypes.c_void_p\n"
insert=anchor+"user32.SetProcessDpiAwarenessContext.argtypes=[ctypes.c_void_p]\nuser32.SetProcessDpiAwarenessContext.restype=wintypes.BOOL\n# Establish one DPI contract before pywebview creates WinForms/WebView2.\nif not user32.SetProcessDpiAwarenessContext(ctypes.c_void_p(-4)):\n err=ctypes.get_last_error()\n if err not in (0,5):raise OSError('PROCESS_PER_MONITOR_V2_DPI_UNAVAILABLE:'+str(err))\n"
if 'PROCESS_PER_MONITOR_V2_DPI_UNAVAILABLE' not in s:
 if s.count(anchor)!=1:raise SystemExit('DPI_ANCHOR_CHANGED')
 s=s.replace(anchor,insert,1)
plan={'status':'DRY_RUN','before':sha(host),'method':'PROCESS_PER_MONITOR_DPI_AWARE_V2','voiceAuthorityChanged':False,'newWindow':False}
if a.apply:
 out.mkdir(parents=True,exist_ok=False);shutil.copy2(host,out/'agent-ui-host.before.py');shutil.copy2(binding,out/'binding.before.json')
 host.write_text(s,encoding='utf8',newline='\n');b['nativeAgentUi']['sha256']=sha(host);binding.write_text(json.dumps(b,indent=2),encoding='utf8')
 plan.update(status='APPLIED_RESTART_REQUIRED',after=sha(host));(out/'receipt.json').write_text(json.dumps(plan,indent=2),encoding='utf8')
print(json.dumps(plan,indent=2))
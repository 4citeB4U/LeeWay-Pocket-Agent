"""REGION: LEEWAY.DEVICE.UI.REPAIR; TAG: COLORKEY_SINGLE_WEBVIEW_HOST
WHO: Creator-authorized engineering. WHAT: Make the existing transparent Windows Agent Lee WebView hit-testable.
WHEN: DWM-transparent WebView renders correctly but receives no real mouse input.
WHERE: Existing LeeWay Desktop Commander native host only; no second window.
WHY: WebView2 transparency/hit-testing is unreliable in a fully transparent DWM-hosted client.
HOW: Use one opaque chroma background on the form/WebView and Windows TransparencyKey to remove it visually.
All content pixels remain visible, and input remains on the same WebView hierarchy.
LICENSE: MIT
"""
import argparse,pathlib,hashlib,shutil,json
p=argparse.ArgumentParser();p.add_argument('--host',required=True);p.add_argument('--binding',required=True);p.add_argument('--out',required=True);p.add_argument('--apply',action='store_true');a=p.parse_args()
host=pathlib.Path(a.host);binding=pathlib.Path(a.binding);out=pathlib.Path(a.out)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
b=json.loads(binding.read_text(encoding='utf-8-sig'));assert pathlib.Path(b['nativeAgentUi']['source']).resolve()==host.resolve();assert sha(host)==b['nativeAgentUi']['sha256']
s=host.read_text(encoding='utf-8-sig')
old="""   observation['stage']='FORM_STYLE';form.TopMost=topmost;form.SetStyle(ControlStyles.SupportsTransparentBackColor,True)
   # DWM composition preserves WebView alpha and real input. A color key makes the input plane disappear.
   observation['stage']='BACKGROUND';form.BackColor=Color.Black;form.webview.DefaultBackgroundColor=Color.Transparent
   handle=wintypes.HWND(form.Handle.ToInt64())
   observation['stage']='DWM_FRAME' if target.frameless else 'DWM_FRAMED_ALPHA'
   apply_native_alpha(handle,bool(target.frameless),observation)
   observation['stage']='INPUT_AND_INVALIDATE';user32.SetWindowLongW(handle,-20,user32.GetWindowLongW(handle,-20)&~0x08000000);restore_webview_input(form,observation)
"""
new="""   observation['stage']='FORM_STYLE';form.TopMost=topmost;form.SetStyle(ControlStyles.SupportsTransparentBackColor,True)
   handle=wintypes.HWND(form.Handle.ToInt64())
   if target is window:
    # One-window chroma-key transparency. This avoids the transparent-WebView hit-test failure.
    chroma=Color.FromArgb(1,2,3)
    observation['stage']='COLORKEY_BACKGROUND';form.AllowTransparency=True;form.BackColor=chroma;form.TransparencyKey=chroma
    form.webview.BackColor=chroma;form.webview.DefaultBackgroundColor=chroma
    observation['compositionMethod']='WINFORMS_COLORKEY_SINGLE_HOST'
   else:
    observation['stage']='BACKGROUND';form.BackColor=Color.Black;form.webview.DefaultBackgroundColor=Color.Transparent
    observation['stage']='DWM_FRAME' if target.frameless else 'DWM_FRAMED_ALPHA';apply_native_alpha(handle,bool(target.frameless),observation)
   observation['stage']='INPUT_AND_INVALIDATE';user32.SetWindowLongW(handle,-20,user32.GetWindowLongW(handle,-20)&~0x08000000);restore_webview_input(form,observation)
"""
if old not in s:raise SystemExit('COMPOSITION_BLOCK_CHANGED')
s=s.replace(old,new,1)
# Extend readback so acceptance can prove the key in use.
old2="webViewControlBackgroundArgb=int(form.webview.BackColor.ToArgb()),webViewControlBackgroundAlpha=int(form.webview.BackColor.A),topmost=bool(form.TopMost),windowVisible=bool(form.Visible))"
new2="webViewControlBackgroundArgb=int(form.webview.BackColor.ToArgb()),webViewControlBackgroundAlpha=int(form.webview.BackColor.A),transparencyKeyArgb=int(form.TransparencyKey.ToArgb()),topmost=bool(form.TopMost),windowVisible=bool(form.Visible))"
if old2 not in s:raise SystemExit('READBACK_BLOCK_CHANGED')
s=s.replace(old2,new2,1)
plan={'status':'DRY_RUN','before':sha(host),'voiceAuthorityChanged':False,'newWindow':False,'method':'WINFORMS_COLORKEY_SINGLE_HOST'}
if a.apply:
 out.mkdir(parents=True,exist_ok=False);shutil.copy2(host,out/'agent-ui-host.before.py');shutil.copy2(binding,out/'binding.before.json')
 host.write_text(s,encoding='utf8',newline='\n');b['nativeAgentUi']['sha256']=sha(host);binding.write_text(json.dumps(b,indent=2),encoding='utf8')
 plan.update(status='APPLIED_RESTART_REQUIRED',after=sha(host));(out/'receipt.json').write_text(json.dumps(plan,indent=2),encoding='utf8')
print(json.dumps(plan,indent=2))
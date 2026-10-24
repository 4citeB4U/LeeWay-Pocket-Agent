"""REGION: LEEWAY.NATIVE.INPUT; TAG: OWNER_GESTURE_TO_SHARED_UI_ACTION
WHO: Creator-authorized repair. WHAT: Replace unreliable transparent WebView mouse replay with native tap dispatch.
WHEN: Native click test failure. WHERE: Existing input adapter and shared UI, not test instrumentation.
WHY: Real native down/up must produce exactly one logical control action; moving cannot become speech.
HOW: Native capture/move/cancel, observed client coordinates, shared hit test. LICENSE: MIT.
"""
import argparse,pathlib,json,hashlib,shutil
p=argparse.ArgumentParser();p.add_argument('--body',required=True);p.add_argument('--out',required=True);p.add_argument('--apply',action='store_true');a=p.parse_args()
body=pathlib.Path(a.body);bpath=body/'host-commander.binding.v1.json';b=json.loads(bpath.read_text(encoding='utf-8-sig'));host=pathlib.Path(b['nativeAgentUi']['source']);page=body/'runtime/machine-consciousness/pocket-voice.html';out=pathlib.Path(a.out)
def once(s,x,y):assert s.count(x)==1,x[:100];return s.replace(x,y)
s=host.read_text();assert hashlib.sha256(host.read_bytes()).hexdigest().upper()==b['nativeAgentUi']['sha256']
s=once(s,"gesture.update(x=cursor.X,y=cursor.Y,left=form.Left,top=form.Top,width=form.Width,height=form.Height,mode=mode,moved=False)\n    forward('mousePressed',event,True)","gesture.update(x=cursor.X,y=cursor.Y,left=form.Left,top=form.Top,width=form.Width,height=form.Height,mode=mode,moved=False,started=time.monotonic())")
s=once(s,"    forward('mouseMoved',event,True)","    # Native pointer owns the gesture; browser events are not replayed while capture is active.\n    return")
s=once(s,"    forward('mouseReleased',event,False);f.Capture=False\n    moved=bool(gesture.get('moved'));gesture.clear()", """    moved=bool(gesture.get('moved'));elapsed=time.monotonic()-gesture.get('started',time.monotonic())
    if not moved and elapsed<.65:
     cursor=Forms.Cursor.Position;pt=form.webview.PointToClient(cursor);sc=float(form._scale)
     data={'x':pt.X/sc,'y':pt.Y/sc}
     last_mouse.append({'event':'nativeTap','cssX':data['x'],'cssY':data['y'],'at':time.time()});del last_mouse[:-8]
     form.webview.CoreWebView2.ExecuteScriptAsync('window.__leewayFloating?.nativeTap('+json.dumps(data)+')')
    f.Capture=False;gesture.clear()""")
h=page.read_text(encoding='utf-8-sig');h=once(h,"window.__leewayFloating={host,blocked,cancel,runText:agentLeeSpeak,testVoice,metrics:()=>voice?.metrics()||null};", """function nativeTap(point){
 const x=Number(point.x),y=Number(point.y);if(!Number.isFinite(x+y)||x<0||y<0||x>innerWidth||y>innerHeight)return;
 const target=document.elementFromPoint(x,y);const control=target?.closest('button');
 if(control){counts.nativeControlTaps=(counts.nativeControlTaps||0)+1;counts.lastNativeTarget=control.id||control.dataset.setting;control.click();return;}
 if(!blocked()&&target===canvas&&hit({clientX:x,clientY:y})){counts.sphereTaps++;counts.lastNativeTarget='sphere';Promise.resolve(toggleVoiceInteraction()).catch(e=>report(e.message));}
}
window.__leewayFloating={host,blocked,cancel,nativeTap,runText:agentLeeSpeak,testVoice,metrics:()=>voice?.metrics()||null};""")
changes={host:s,page:h};b['nativeAgentUi']['sha256']=hashlib.sha256(s.encode()).hexdigest().upper();changes[bpath]=json.dumps(b,indent=2)
plan={'status':'DRY_RUN','files':{str(f):hashlib.sha256(f.read_bytes()).hexdigest()for f in changes},'nativePhysicalEventsStillRequired':True,'menuUsesNativeWebViewWhenOpen':True}
if a.apply:
 out.mkdir(parents=True,exist_ok=False)
 for i,(file,content)in enumerate(changes.items()):shutil.copy2(file,out/(str(i)+'-'+file.name));file.write_bytes(content.encode())
 plan['status']='APPLIED_RETEST_REQUIRED';(out/'patch.json').write_text(json.dumps(plan,indent=2))
print(json.dumps(plan,indent=2))

"""REGION: LEEWAY.UI.REPAIR; TAG: GESTURE_RESIZE_AND_EMBLEM_TOGGLE
WHO: Creator-authorized engineering. WHAT: Remove obsolete resize controls and make emblem open/collapse.
WHEN: Screenshot-confirmed defect. WHERE: Existing Android overlay + shared PC/phone UI only.
WHY: Floating Agent Lee must behave like a movable system companion, not a framed application.
HOW: Native overlay gestures, exact HTML cleanup, canonical VT launch, no Voice/Brain authority changes.
LICENSE: MIT
"""
import argparse,pathlib,re,json,hashlib,shutil
p=argparse.ArgumentParser();p.add_argument('--phone',required=True);p.add_argument('--runtime',required=True);p.add_argument('--out',required=True);p.add_argument('--apply',action='store_true');a=p.parse_args()
phone=pathlib.Path(a.phone);runtime=pathlib.Path(a.runtime);out=pathlib.Path(a.out)
def read(p):return p.read_text(encoding='utf-8-sig')
def once(s,x,y):assert s.count(x)==1,'BOUNDARY:'+x[:90];return s.replace(x,y)
def h(b):return hashlib.sha256(b).hexdigest()
changes={}
# Shared visual cleanup: no white/glass card backing, no obsolete +/-/expand/stop strip, no artificial resize grip.
for f in [phone/'app/src/main/assets/agent_lee_sphere_transparent.html',runtime/'machine-consciousness/pocket-voice.html']:
 s=read(f)
 s=re.sub(r'#agent-controls\{[^}]*\}body:hover #agent-controls,#agent-controls:focus-within\{[^}]*\}\s*#agent-controls button\{[^}]*\}\s*#agent-resize-grip\{[^}]*\}','',s)
 s=re.sub(r'<div id="agent-controls">[\s\S]*?<section id="surface-panel"','<section id="surface-panel"',s,count=1)
 s=s.replace('background:rgba(9,19,32,.8);border:1px solid rgba(255,255,255,.4)','background:transparent;border:0;box-shadow:none;backdrop-filter:none;-webkit-backdrop-filter:none')
 s=s.replace('background: rgba(255, 255, 255, 0.08);','background: transparent;').replace('border: 1px solid rgba(255, 255, 255, 0.28);','border: 0;').replace('backdrop-filter: blur(18px) saturate(150%);','backdrop-filter: none;').replace('-webkit-backdrop-filter: blur(18px) saturate(150%);','-webkit-backdrop-filter: none;')
 s=s.replace('background: rgba(255,255,255,0.055);','background: transparent;').replace('border: 1px solid rgba(255,255,255,0.20);','border: 0;').replace('box-shadow: 0 10px 36px rgba(0,0,0,0.12), inset 0 0 24px rgba(56,189,248,0.06);','box-shadow: none;')
 s=re.sub(r"for\(const \[id,action,delta\]of\[\['shrink-agent'[\s\S]*?let resizing=null;const grip=\$\('agent-resize-grip'\);grip\?\.addEventListener\('pointerdown'[\s\S]*?grip\?\.addEventListener\('pointercancel',\(\)=>resizing=null\);",'',s,count=1)
 # Workstation is a real system surface on Android, not a placeholder panel.
 if f.name=='agent_lee_sphere_transparent.html':
  marker=" const key=event.detail?.surface;const panel=document.getElementById('surface-panel');"
  inject=marker+"\n if(key==='workstation'&&window.LeeWayAndroid?.openWorkstation){window.LeeWayAndroid.openWorkstation();return;}"
  s=once(s,marker,inject)
 changes[f]=s
# Android bridge exposes the already-existing AgentTabletActivity from the floating sphere.
k=phone/'app/src/main/java/industries/leeway/pocket/FloatingSphereWindow.kt';s=read(k)
s=once(s,'@JavascriptInterface fun openDigitalBrain(){open(false)}','@JavascriptInterface fun openWorkstation(){main.post{service.startActivity(Intent(service,AgentTabletActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}\n        @JavascriptInterface fun openDigitalBrain(){open(false)}')
# Two-finger pinch owns size; one-finger drag owns position. The web content remains responsible for taps/menu.
s=once(s,'web=view;manager.addView(view,lp);view.post{report()}','''
        var dragPointer=-1;var startRawX=0f;var startRawY=0f;var startX=lp.x;var startY=lp.y
        var pinchStart=0f;var pinchSize=lp.width;var gestureMoved=false
        view.setOnTouchListener{_,event->
            when(event.actionMasked){
                MotionEvent.ACTION_DOWN->{dragPointer=event.getPointerId(0);startRawX=event.rawX;startRawY=event.rawY;startX=lp.x;startY=lp.y;gestureMoved=false;false}
                MotionEvent.ACTION_POINTER_DOWN->{if(event.pointerCount==2){pinchStart=kotlin.math.hypot(event.getX(0)-event.getX(1),event.getY(0)-event.getY(1));pinchSize=lp.width;gestureMoved=true;true}else false}
                MotionEvent.ACTION_MOVE->{
                    if(event.pointerCount>=2&&pinchStart>0f){val d=kotlin.math.hypot(event.getX(0)-event.getX(1),event.getY(0)-event.getY(1));val side=(pinchSize*(d/pinchStart)).toInt().coerceIn(dp(110).coerceAtMost(maximum()),maximum());lp.width=side;lp.height=side;expanded=false;gestureMoved=true;relayout();true}
                    else if(dragPointer>=0&&event.pointerCount==1){val dx=event.rawX-startRawX;val dy=event.rawY-startRawY;if(kotlin.math.hypot(dx,dy)>ViewConfiguration.get(service).scaledTouchSlop){gestureMoved=true;lp.x=startX+dx.toInt();lp.y=startY+dy.toInt();relayout();true}else false}else false
                }
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{if(gestureMoved){prefs.edit().putInt("x",lp.x).putInt("y",lp.y).putInt("width",lp.width).apply();dragPointer=-1;pinchStart=0f;true}else{dragPointer=-1;pinchStart=0f;false}}
                else->false
            }
        }
        web=view;manager.addView(view,lp);view.post{report()}''')
changes[k]=s
# Right-edge emblem toggles sphere visibility instead of only opening it.
p=phone/'app/src/main/java/industries/leeway/pocket/PocketOverlayService.kt';s=read(p)
s=once(s,'private fun showSphereOverlay(){if(sphere==null)sphere=FloatingSphereWindow(this);sphere?.show()}','private fun toggleSphereOverlay(){if(sphere?.isVisible()==true){sphere?.close();sphere=null}else{sphere=FloatingSphereWindow(this);sphere?.show()}}\n    private fun showSphereOverlay(){if(sphere==null)sphere=FloatingSphereWindow(this);sphere?.show()}')
s=once(s,'if(click){badge.performClick();showSphereOverlay()}','if(click){badge.performClick();toggleSphereOverlay()}')
changes[p]=s
# Visibility query for toggle.
s=changes[k];s=once(s,'fun close(){web?.let{it.removeJavascriptInterface("LeeWayAndroid");manager.removeView(it);it.destroy()};web=null;params=null}','fun isVisible()=web?.isAttachedToWindow==true\n    fun close(){web?.let{it.removeJavascriptInterface("LeeWayAndroid");manager.removeView(it);it.destroy()};web=null;params=null}')
changes[k]=s
plan={'status':'DRY_RUN','files':{str(f):h(f.read_bytes()) for f in changes},'voiceAuthorityChanged':False,'brainAuthorityChanged':False,'removesThreeButtons':True,'emblemToggle':True,'androidPinchResize':True}
if a.apply:
 out.mkdir(parents=True,exist_ok=False)
 for i,(f,c) in enumerate(changes.items()):shutil.copy2(f,out/(str(i)+'-'+f.name));f.write_text(c,encoding='utf8',newline='\n')
 plan['status']='APPLIED_BUILD_AND_INSTALL_REQUIRED';plan['after']={str(f):h(f.read_bytes()) for f in changes};(out/'patch.json').write_text(json.dumps(plan,indent=2))
print(json.dumps(plan,indent=2))
"""REGION: LEEWAY.REPAIR; TAG: USE_EXISTING_OVERLAY_NOT_ACTIVITY
WHO: Creator-authorized Agent Lee. WHAT: Bind existing launcher/service to approved floating sphere and Voice-owned test.
WHEN: Actual USB repair. WHERE: existing Pocket candidate; no new runtime, package, identity or voice default.
WHY: A built candidate is not deployed behavior. HOW: Exact edits, source manifests, compile then in-place install.
LICENSE: MIT.
"""
import pathlib,json,hashlib,argparse,subprocess
p=argparse.ArgumentParser();p.add_argument('--apply',action='store_true');a=p.parse_args();root=pathlib.Path(__file__).resolve().parents[1]
assert subprocess.check_output(['git','-C',str(root),'rev-parse','HEAD'],text=True).strip()=='64cf4be8daf85284d6a81bb38fb40534c1072597'
k='app/src/main/java/industries/leeway/pocket/';changes={}
def read(p):return(root/p).read_text(encoding='utf-8-sig')
def once(s,x,y):assert s.count(x)==1,x[:80];return s.replace(x,y)
s=read(k+'PocketOverlayService.kt')
s=once(s,'    private var foregroundReady=false','    private var foregroundReady=false\n    private var sphere:FloatingSphereWindow?=null\n    private fun showSphereOverlay(){if(sphere==null)sphere=FloatingSphereWindow(this);sphere?.show()}')
s=once(s,'        AndroidBrainIngestion.start(this)','        AndroidBrainIngestion.start(this)\n        if(intent?.action=="LEEWAY_SHOW_FLOATING_SPHERE")showSphereOverlay()')
s=once(s,'        AndroidBrainIngestion.stop();detach()','        sphere?.close();sphere=null;AndroidBrainIngestion.stop();detach()')
s=once(s,'super.onConfigurationChanged(configuration);tab?.let{applyPosition(it)}','super.onConfigurationChanged(configuration);tab?.let{applyPosition(it)};sphere?.relayout()')
s=once(s,'badge.performClick();startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))','badge.performClick();showSphereOverlay()')
s=once(s,'        fun isEnabled(context:Context)=','''        fun showSphere(context:Context){if(isEnabled(context)&&Settings.canDrawOverlays(context))ContextCompat.startForegroundService(context,Intent(context,PocketOverlayService::class.java).setAction("LEEWAY_SHOW_FLOATING_SPHERE"))}
        fun isEnabled(context:Context)=''');changes[k+'PocketOverlayService.kt']=s
s=read(k+'MainActivity.kt')
s=once(s,'        bootstrapDeviceBrain()','''        bootstrapDeviceBrain()
        if(Settings.canDrawOverlays(this)&&PocketOverlayService.isEnabled(this)&&intent?.getStringExtra("leeway_action").isNullOrBlank()){
            PocketOverlayService.showSphere(this);finish();return
        }''')
s=once(s,'            PocketOverlayService.setEnabled(this,true)\n        }else if', '            PocketOverlayService.setEnabled(this,true);PocketOverlayService.showSphere(this);finish()\n        }else if')
s=once(s,'        @JavascriptInterface fun talk()', '''        @JavascriptInterface fun testVoice(){runOnUiThread{PocketVoiceHost.playVerifiedOutputSample(this@MainActivity)}}
        @JavascriptInterface fun stopVoice(){runOnUiThread{PocketVoiceHost.stopVerifiedOutputSample()}}
        @JavascriptInterface fun deviceName():String=android.os.Build.MODEL
        @JavascriptInterface fun talk()''');changes[k+'MainActivity.kt']=s
s=read(k+'PocketVoiceHost.kt')
s=once(s,'    fun description()=', '''    fun playVerifiedOutputSample(context:Context){stopPlayback();VoiceOutputSample.play(context)}
    fun stopVerifiedOutputSample(){VoiceOutputSample.stop()}
    fun description()=''')
s=once(s,'    private fun stopPlayback() {','    private fun stopPlayback() {\n        VoiceOutputSample.stop()');changes[k+'PocketVoiceHost.kt']=s
s=read('app/build.gradle.kts');s=once(s,'versionCode = 29','versionCode = 30');s=once(s,'versionName = "1.0.0-overlay-recovery-rc2"','versionName = "1.0.0-floating-live-rc3"');changes['app/build.gradle.kts']=s
s=read('scripts/Observe-PhoneUi.py');s=once(s,"image=call(['exec-out','screencap','-p']);assert image.startswith(b'\\x89PNG');", "image=call(['exec-out','screencap','-p']);index=image.find(b'\\x89PNG');assert 0<=index<1024;image=image[index:];");changes['scripts/Observe-PhoneUi.py']=s
result={'state':'DRY_RUN','sourceBase':'64cf4be8daf85284d6a81bb38fb40534c1072597','files':list(changes),'voiceAuthorityChanged':False,'dataCleared':False}
if a.apply:
 for path,text in changes.items():(root/path).write_bytes(text.encode('utf-8'))
 result['state']='SOURCE_APPLIED_BUILD_REQUIRED';result['hashes']={p:hashlib.sha256((root/p).read_bytes()).hexdigest()for p in changes}
 (root/'qualification/live-both-20261006T155546390Z/source-patch.json').write_text(json.dumps(result,indent=2))
print(json.dumps(result,indent=2))

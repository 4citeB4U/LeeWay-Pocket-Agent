"""REGION: LEEWAY.DEPLOYMENT.REPAIR; TAG: REAL_UI_NAVIGATION_AND_BRANDING
WHO: Creator-authorized engineering. WHAT: Repair the existing native UI paths and upgrade precondition.
WHEN: Before signed in-place upgrade. WHERE: Canonical Pocket repair branch.
WHY: Built does not mean installed. HOW: Exact replacements, dry-run, source hashes. LICENSE: MIT.
"""
import argparse,pathlib,hashlib,json,re,subprocess
p=argparse.ArgumentParser();p.add_argument('--apply',action='store_true');p.add_argument('--artwork',required=True);p.add_argument('--three',required=True);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1]
assert subprocess.check_output(['git','-C',str(root),'branch','--show-current'],text=True).strip()=='repair/installed-brain-diagnostics'
def sha(b):return hashlib.sha256(b).hexdigest()
def read(p):return (root/p).read_text(encoding='utf-8-sig')
def once(s,old,new):
 assert s.count(old)==1,'REPAIR_BOUNDARY_NOT_UNIQUE: '+old[:90]
 return s.replace(old,new)
changes={};k='app/src/main/java/industries/leeway/pocket/'
s=read(k+'LeeWayBodyDatabases.kt');s=once(s,'    init {','    init {\n        OwnerDatabaseUpgrade.apply(context)');changes[k+'LeeWayBodyDatabases.kt']=s
s=read(k+'MainActivity.kt');s=once(s,'        if(intent?.getStringExtra("leeway_action")=="TALK_TO_AGENT_LEE") startActivity(PocketVoiceActivity.launchIntent(this))','''        when(intent?.getStringExtra("leeway_action")){
            "TALK_TO_AGENT_LEE"->startActivity(PocketVoiceActivity.launchIntent(this))
            "OPEN_DIGITAL_BRAIN"->openBrain(false)
            "OPEN_DIAGNOSTICS"->openBrain(true)
        }''')
s=once(s,'    override fun onResume(){','''    private fun openBrain(hardware:Boolean){startActivity(Intent(this,DigitalBrainActivity::class.java).putExtra("hardware",hardware))}
    override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);when(intent.getStringExtra("leeway_action")){"OPEN_DIGITAL_BRAIN"->openBrain(false);"OPEN_DIAGNOSTICS"->openBrain(true)}}
    override fun onResume(){''')
s=once(s,'        AndroidDigitalBrainAdapter.bootstrap(this)','        AndroidDigitalBrainAdapter.bootstrap(this)\n        industries.leeway.pocket.devices.DeviceDiagnostics.refresh(this,true)')
s=once(s,'@JavascriptInterface fun openDigitalBrain(){runOnUiThread{startActivity(Intent(this@MainActivity,DigitalBrainActivity::class.java))}}','@JavascriptInterface fun openDigitalBrain(){runOnUiThread{openBrain(false)}}\n        @JavascriptInterface fun openDiagnostics(){runOnUiThread{openBrain(true)}}')
s=once(s,'        val prefs=getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE)','''        val prefs=getSharedPreferences("leeway-pocket-overlay",MODE_PRIVATE)
        if(Settings.canDrawOverlays(this))PocketOverlayService.setEnabled(this,true)''')
changes[k+'MainActivity.kt']=s
s=read(k+'AndroidBrainViewer.kt');s=once(s,'            val identity=AndroidDigitalBrainAdapter.identity(context)','''            val identity=AndroidDigitalBrainAdapter.identity(context)
            if(operation=="root" || args.optString("id").contains(":system:hardware"))industries.leeway.pocket.devices.DeviceDiagnostics.refresh(context)''');changes[k+'AndroidBrainViewer.kt']=s
s=read(k+'DigitalBrainActivity.kt');s=once(s,'        @JavascriptInterface fun close()', '''        @JavascriptInterface fun initialSurface():String=if(intent.getBooleanExtra("hardware",false))"hardware" else "brain"
        @JavascriptInterface fun close()''');changes[k+'DigitalBrainActivity.kt']=s
s=read(k+'PocketOverlayService.kt');start=s.index('        val badge=TextView(this).apply');end=s.index('\n        val lp=',start)
s=s[:start]+'''        val badge=ElementalEmblemView(this).apply{contentDescription="Open Agent Lee"}
'''+s[end:]
s=once(s,'WindowManager.LayoutParams(dp(58),dp(58)','WindowManager.LayoutParams(dp(68),dp(68)')
s=once(s,'startActivity(PocketVoiceActivity.launchIntent(this,true))','startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))')
s=once(s,';attach();if(isEnabled(this))AndroidBrainIngestion.start(this)',';if(isEnabled(this)){attach();AndroidBrainIngestion.start(this)}')
changes[k+'PocketOverlayService.kt']=s
s=read(k+'PocketVoiceActivity.kt')
s=s.replace('status.text="Listening";','ElementalEmblemView.visualState="LISTENING";status.text="Listening";').replace('status.text="Thinking"','ElementalEmblemView.visualState="THINKING";status.text="Thinking"').replace('status.text="Speaking"','ElementalEmblemView.visualState="SPEAKING";status.text="Speaking"')
s=s.replace('override fun onComplete(){status.text="Ready"}','override fun onComplete(){ElementalEmblemView.visualState="IDLE";status.text="Ready"}').replace('override fun onError(message:String){status.text','override fun onError(message:String){ElementalEmblemView.visualState="BLOCKED";status.text').replace('override fun onDestroy(){recognizer','override fun onDestroy(){ElementalEmblemView.visualState="IDLE";recognizer')
changes[k+'PocketVoiceActivity.kt']=s
s=read('app/src/main/AndroidManifest.xml');s=once(s,'        android:label="Agent Lee"','        android:label="Agent Lee"\n        android:icon="@drawable/agent_lee_emblem"');s=once(s,'    <uses-permission android:name="android.permission.INTERNET" />','    <uses-permission android:name="android.permission.INTERNET" />\n    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />');changes['app/src/main/AndroidManifest.xml']=s
s=read('app/build.gradle.kts');s=once(s,'versionCode = 27','versionCode = 28');s=once(s,'versionName = "1.0.0-portable-body-candidate"','versionName = "1.0.0-live-surface-repair-rc1"');changes['app/build.gradle.kts']=s
html='app/src/main/assets/agent_lee_sphere_transparent.html';s=read(html)
s=once(s,'<div class="agent-side-tab" aria-hidden="true"></div>','<button class="agent-side-tab" id="agent-emblem" type="button" aria-label="Open Agent Lee settings" onclick="openDrawer()"><img src="agent-lee-emblem.png" alt="Agent Lee"></button>')
style='''<style id="leeway-shared-emblem-style">
.agent-side-tab{pointer-events:auto!important;border:0!important;background:transparent!important;backdrop-filter:none!important;min-height:0!important;width:68px!important;height:68px!important;padding:0!important;z-index:25!important}
.agent-side-tab img{width:100%;height:100%;object-fit:contain;animation:leeway-elemental-cycle 15s linear infinite}
@keyframes leeway-elemental-cycle{0%,100%{filter:hue-rotate(0deg)}33%{filter:hue-rotate(105deg)}66%{filter:hue-rotate(210deg)}85%{filter:grayscale(1)}}
.hamburger-btn{top:22px!important;left:22px!important;right:auto!important;transform:none!important}
@media(prefers-reduced-motion:reduce){.agent-side-tab img{animation:none}}
</style>'''
s=once(s,'</head>',style+'\n</head>')
s=once(s,'<script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>','<script src="vendor/three-r128.min.js"></script>')
button='''<div class="element-card" onclick="window.LeeWayAndroid.openDiagnostics()"><div><div class="element-name">Device Diagnostics</div><div class="element-motto">This device · live hardware Brain</div></div><div class="element-action">OPEN</div></div>
    <div class="element-card" onclick="window.LeeWayAndroid.enableOverlay()"><div><div class="element-name">Floating Agent Lee</div><div class="element-motto">Enable the logo tab over other apps</div></div><div class="element-action">ENABLE</div></div>
    '''
s=once(s,'<div class="element-card" onclick="openWorkstation()">',button+'<div class="element-card" onclick="openWorkstation()">');changes[html]=s
binding='app/src/main/assets/digital-brain/local-brain-binding.js';s=read(binding)
s=once(s,'      window.__leewayLocalBrainReady={bodyId,rootId,scope:\'READ_ONLY_LOCAL_SQLITE_PROJECTION\'};', '''      window.__leewayLocalBrainReady={bodyId,rootId,scope:'READ_ONLY_LOCAL_SQLITE_PROJECTION'};
      if(native.initialSurface?.()==='hardware'){
        await load(rootId+':system:hardware');
        const started=performance.now();const wait=setInterval(()=>{if(window.__leewayOriginalBrain3D?.ready){clearInterval(wait);window.dispatchEvent(new Event('leeway-enter-local-brain'));}else if(performance.now()-started>10000)clearInterval(wait);},50);
      }''');changes[binding]=s
art=pathlib.Path(a.artwork).read_bytes();assert sha(art)=='31cb7cfb845524a19202ed49e42b0fec71d8bcc4a5462e52469e98a3cca6b3fa'
three=pathlib.Path(a.three).read_bytes();assert sha(three)=='9274bbcec8d96168626c732b5d31c775aa8cfb7eaa0599bec0c175908a2c1ce2'
plan={'state':'DRY_RUN','files':list(changes),'logoSha256':sha(art),'threeSha256':sha(three),'noDataClearing':True,'fullGoldenRelease':False}
if a.apply:
 for p,s in changes.items():(root/p).write_bytes(s.encode('utf-8'))
 for p,data in [('app/src/main/assets/agent-lee-emblem.png',art),('app/src/main/res/drawable/agent_lee_emblem.png',art),('app/src/main/assets/vendor/three-r128.min.js',three)]:
  f=root/p;f.parent.mkdir(parents=True,exist_ok=True);f.write_bytes(data)
 plan['state']='APPLIED_BUILD_AND_LIVE_ACCEPTANCE_REQUIRED';plan['hashes']={p:sha((root/p).read_bytes()) for p in changes}
 (root/'qualification'/'installed-surface-patch.json').write_text(json.dumps(plan,indent=2),encoding='utf8')
print(json.dumps(plan,indent=2))

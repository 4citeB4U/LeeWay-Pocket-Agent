import pathlib, shutil, hashlib, json, re
phone=pathlib.Path(r'E:\LeeWay-Work\agent-lee-fold6-unified')
runtime=pathlib.Path(r'E:\LeeWay-Work\pc-consciousness-live\runtime')
out=phone/'qualification'/'brain-viewer-shell-20261007'
if out.exists(): raise SystemExit('EVIDENCE_DESTINATION_EXISTS')
out.mkdir(parents=True)
def read(p): return p.read_text(encoding='utf-8-sig')
def write(p,s): p.write_text(s,encoding='utf-8',newline='\n')
def once(s,a,b,label):
    if s.count(a)!=1: raise RuntimeError(label)
    return s.replace(a,b)
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
# 1. Phone Brain viewer shell; preserve renderer/data code.
bp=phone/'app/src/main/assets/digital-brain/brain.html'
s=read(bp); shutil.copy2(bp,out/'brain.before.html')
patch='''<style id="leeway-owner-local-window-shell">
html,body,#absoluteNucleusBackground{background:transparent!important}
body{background:transparent!important}
#absoluteNucleusBackground{opacity:.18!important}
#nucleusFallbackImage{opacity:.11!important;background:transparent!important}
#nucleusWaveCanvas{opacity:.18!important}
#top{background:rgba(3,10,18,.58)!important;border-bottom-color:rgba(105,155,190,.22)!important;backdrop-filter:blur(12px)}
#dock{background:rgba(3,10,18,.54)!important;border-top-color:rgba(105,155,190,.20)!important;backdrop-filter:blur(12px)}
#localControls{background:rgba(3,10,18,.48)!important;border:1px solid rgba(125,175,210,.22);backdrop-filter:blur(12px)}
#localClose{position:fixed;left:10px;top:62px;z-index:13000;border-color:rgba(113,228,255,.42)!important;background:rgba(5,16,28,.62)!important;backdrop-filter:blur(10px)}
#localClose::before{content:"← "}
button,input,.node{backdrop-filter:blur(8px)}
#stage,#brainUniverseLayer,#brain3DCanvas{background:transparent!important}
#hud,#settingsPanel,#legend{background:linear-gradient(155deg,rgba(5,11,20,.88),rgba(3,8,15,.84))!important}
@media(max-width:760px){
 #top{padding-left:70px}
 #localClose{top:8px;left:8px}
 #localControls{left:6px;right:6px;bottom:62px}
}
</style>'''
s=once(s,'</head>',patch+'\n</head>','HEAD_BOUNDARY')
write(bp,s)
# 2. local binding: make close deterministic and surface failures visibly without freezing.
lp=phone/'app/src/main/assets/digital-brain/local-brain-binding.js'
ls=read(lp); shutil.copy2(lp,out/'local-binding.before.js')
old="document.getElementById('localClose')?.addEventListener('click',()=>native.close?.());"
new="document.getElementById('localClose')?.addEventListener('click',()=>{try{native.close?.();}catch(error){show('RETURN_TO_AGENT_LEE_FAILED: '+error.message,true);}});"
ls=once(ls,old,new,'CLOSE_BOUNDARY')
write(lp,ls)
# 3. Sync exact Brain viewer assets into current Windows native runtime.
pc_dir=runtime/'machine-consciousness'/'digital-brain'
pc_dir.mkdir(parents=True,exist_ok=True)
for name in ['brain.html','local-brain-binding.js','nucleus.png']:
    src=phone/'app/src/main/assets/digital-brain'/name
    shutil.copy2(src,pc_dir/name)
for name in ['three.module.js','OrbitControls.js','THREE-LICENSE.txt']:
    src=phone/'app/src/main/assets/digital-brain/vendor'/name
    (pc_dir/'vendor').mkdir(parents=True,exist_ok=True)
    shutil.copy2(src,pc_dir/'vendor'/name)
# 4. Sync only living visual presentation from phone sphere into PC while preserving PC-specific routes.
pp=phone/'app/src/main/assets/agent_lee_sphere_transparent.html'
wp=runtime/'machine-consciousness'/'pocket-voice.html'
ps=read(pp); ws=read(wp); shutil.copy2(wp,out/'pc-sphere.before.html')
# living formula style and DOM
style=re.search(r'<style id="leeway-living-brain-style">[\s\S]*?</style>',ps)
if not style: raise RuntimeError('LIVING_STYLE_MISSING')
ws=re.sub(r'<style id="leeway-living-brain-style">[\s\S]*?</style>','',ws)
ws=once(ws,'</head>',style.group(0)+'\n</head>','PC_HEAD')
if '<div id="leeway-formula-thoughts"' not in ws:
    ws=once(ws,'<body>','<body>\n  <div id="leeway-formula-thoughts" aria-hidden="true"></div>','PC_BODY')
# formula scheduling block
fstart=ps.index('    // Sparse visual notation from the LeeWay awareness adapter.')
fend=ps.index('\n\n    function animate()',fstart)
fblock=ps[fstart:fend]
if 'Sparse visual notation from the LeeWay awareness adapter.' not in ws:
    marker='    // --- ANIMATION LOOP (LIVELY TALKING ANIMATION) ---\n    const clock = new THREE.Clock();'
    ws=once(ws,marker,marker+'\n'+fblock.split('    const clock = new THREE.Clock();')[-1] if False else marker,'NOOP')
# replace breathing section if old form exists; otherwise preserve current newer form
old_b='''      const idleBreath = 0.012 * Math.sin(time * 1.18) + 0.004 * Math.sin(time * 0.43);
      const talkPulse = isSpeaking ? (0.035 * Math.sin(time * 8.4) + voiceAmplitude * 0.11) : (voiceAmplitude * 0.06);
      const currentScale = 1.0 + idleBreath + talkPulse;
      brainGroup.scale.set(currentScale * (1.0 + 0.003 * Math.sin(time * 0.79)), currentScale * (1.0 + 0.006 * Math.sin(time * 1.07 + 0.8)), currentScale);'''
new_b='''      const idleBreath = 0.022 * Math.sin(time * 1.18) + 0.007 * Math.sin(time * 0.43);
      const talkPulse = isSpeaking ? (0.038 * Math.sin(time * 8.4) + voiceAmplitude * 0.11) : (voiceAmplitude * 0.06);
      const currentScale = 1.0 + idleBreath + talkPulse;
      brainGroup.scale.set(currentScale * (1.0 + 0.006 * Math.sin(time * 0.79)), currentScale * (1.0 + 0.012 * Math.sin(time * 1.07 + 0.8)), currentScale * (1.0 + 0.004 * Math.sin(time * 0.67)));'''
if old_b in ws: ws=ws.replace(old_b,new_b)
# If PC lacks formula scheduler, insert the phone scheduler before animate.
if 'emitLeeWayFormulaThoughts' not in ws:
    clock='    const clock = new THREE.Clock();'
    if clock not in ws: raise RuntimeError('PC_CLOCK_BOUNDARY')
    formula=fblock.replace('    const clock = new THREE.Clock();\n','')
    ws=ws.replace(clock,clock+'\n'+formula)
write(wp,ws)
receipt={'status':'PATCHED_SOURCE_RESTART_BUILD_REQUIRED','phoneBrainHtml':sha(bp),'phoneBinding':sha(lp),'pcBrainHtml':sha(pc_dir/'brain.html'),'pcSphere':sha(wp),'voiceAuthorityChanged':False,'brainDatabaseChanged':False}
(out/'receipt.json').write_text(json.dumps(receipt,indent=2),encoding='utf8')
print(json.dumps(receipt,indent=2))
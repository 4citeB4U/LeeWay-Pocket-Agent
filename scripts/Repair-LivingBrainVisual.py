"""REGION: LEEWAY.UI.REPAIR; TAG: LIVING_BRAIN_AND_FORMULA_THOUGHTS
WHO: Creator-authorized Agent Lee UI. WHAT: Restore translucent glass, idle breathing and sparse Formula notation.
WHEN: Shared PC/Fold sphere UI repair. WHERE: presentation only; no Formula execution or data authority.
WHY: The avatar should look alive without implying calculations that were not executed.
HOW: Same Three.js scene, subtle anisotropic breath, canonical awareness-adapter notation, 20-30s sparse fade.
FORMULA SOURCE: Leeway-formula-live commit 50d93a25af6b327cd88d52247777957582aff8a7, machine-consciousness-awareness-v0.
LICENSE: MIT
"""
import argparse,pathlib,re,json,hashlib,shutil
p=argparse.ArgumentParser();p.add_argument('--phone',required=True);p.add_argument('--pc-html',required=True);p.add_argument('--out',required=True);p.add_argument('--apply',action='store_true');a=p.parse_args()
phone=pathlib.Path(a.phone);files=[pathlib.Path(a.pc_html),phone/'app/src/main/assets/agent_lee_sphere_transparent.html'];out=pathlib.Path(a.out)
def text(f):return f.read_text(encoding='utf-8-sig')
def once(s,old,new):assert s.count(old)==1,'BOUNDARY:'+old[:100];return s.replace(old,new)
def sha(b):return hashlib.sha256(b).hexdigest()
css='''<style id="leeway-living-brain-style">
/* Presentation-only Formula notation; no Formula execution is implied by this animation. */
#leeway-formula-thoughts{position:fixed;inset:0;pointer-events:none;z-index:5;overflow:hidden}
.formula-thought{position:absolute;color:rgba(224,230,238,.48);font:500 clamp(10px,2.8vw,15px)/1.25 ui-monospace,SFMono-Regular,Consolas,monospace;letter-spacing:.02em;text-shadow:0 0 12px rgba(186,220,244,.18);white-space:nowrap;opacity:0;transform:translate3d(0,8px,0) scale(.98);animation:leewayFormulaThought 5.8s ease-in-out forwards}
@keyframes leewayFormulaThought{0%{opacity:0;transform:translate3d(0,8px,0) scale(.98)}18%,70%{opacity:.72;transform:translate3d(0,0,0) scale(1)}100%{opacity:0;transform:translate3d(0,-7px,0) scale(1.01)}}
.hamburger-btn,.hamburger-btn:hover{background:rgba(4,12,22,.12)!important;border:1px solid rgba(220,238,250,.14)!important;box-shadow:none!important;backdrop-filter:blur(2px)!important;-webkit-backdrop-filter:blur(2px)!important}
@media(prefers-reduced-motion:reduce){.formula-thought{display:none!important}}
</style>'''
formula_js='''
    // Sparse visual notation from the LeeWay awareness adapter. This is avatar expression, NOT an executed Formula result.
    const leewayFormulaThoughts = [
      'PEₜ = εₜᵀ Πₜ εₜ',
      'εₜ = oₜ − ôₜ',
      'Vₜ = −(Fₜ − Fₜ₋₁)/Δₜ',
      'IGₜ = D_KL(bₜ ∥ bₜ prior)',
      'q(π) ∝ exp(−γ G(π))',
      'Bₜ = acknowledged / required',
      'EVB(k) = Gain(k) × Need(k)',
      'belief stability = 1 − JSD(bₜ,bₜ₋₁)/ln(2)'
    ];
    let leewayFormulaTimer = null;
    function emitLeeWayFormulaThoughts(){
      const host=document.getElementById('leeway-formula-thoughts');
      if(!host || document.hidden || document.getElementById('drawer')?.classList.contains('open')) return scheduleLeeWayFormulaThoughts();
      const count=1+Math.floor(Math.random()*4), used=new Set();
      for(let i=0;i<count;i++){
        let index=Math.floor(Math.random()*leewayFormulaThoughts.length);while(used.has(index)&&used.size<leewayFormulaThoughts.length)index=Math.floor(Math.random()*leewayFormulaThoughts.length);used.add(index);
        const el=document.createElement('span');el.className='formula-thought';el.textContent=leewayFormulaThoughts[index];
        let left=8+Math.random()*76, top=12+Math.random()*70;if(left<30&&top<28){left+=28;top+=8;}
        el.style.left=left+'%';el.style.top=top+'%';el.style.animationDelay=(i*.36)+'s';host.appendChild(el);setTimeout(()=>el.remove(),6800+i*360);
      }
      scheduleLeeWayFormulaThoughts();
    }
    function scheduleLeeWayFormulaThoughts(){clearTimeout(leewayFormulaTimer);leewayFormulaTimer=setTimeout(emitLeeWayFormulaThoughts,20000+Math.random()*10000)}
    scheduleLeeWayFormulaThoughts();
'''
changes={}
for f in files:
 s=text(f)
 assert 'id="leeway-living-brain-style"' not in s and 'id="leeway-formula-thoughts"' not in s,'PATCH_ALREADY_PRESENT'
 s=once(s,'</head>',css+'\n</head>')
 m=re.search(r'<body([^>]*)>',s);assert m,'BODY_MISSING';s=s[:m.end()]+'\n  <div id="leeway-formula-thoughts" aria-hidden="true"></div>'+s[m.end():]
 # Lighten the existing glass; preserve exact geometry and radius.
 s=once(s,'      color: 0x071e3d,\n      emissive: 0x0284c7,\n      emissiveIntensity: 0.18,\n      roughness: 0.08,\n      metalness: 0.1,\n      transmission: 0.72,\n      ior: 1.333,\n      transparent: true,\n      opacity: 0.45,\n      side: THREE.FrontSide','      color: 0x9fc8df,\n      emissive: 0x0b6b98,\n      emissiveIntensity: 0.06,\n      roughness: 0.10,\n      metalness: 0.02,\n      transmission: 0.94,\n      ior: 1.333,\n      transparent: true,\n      opacity: 0.16,\n      depthWrite: false,\n      side: THREE.FrontSide')
 s=s.replace('      opacity: 0.18\n    });\n    const wireframeGlobe','      opacity: 0.08\n    });\n    const wireframeGlobe',1)
 # Add actual idle breathing. Speech still adds its amplitude on top.
 old='''      // Lively Breathing & Talking Surface Expansion\n      const talkPulse = isSpeaking ? (0.04 * Math.sin(time * 9.0) + voiceAmplitude * 0.12) : (voiceAmplitude * 0.08);\n      const currentScale = 1.0 + talkPulse;\n      brainGroup.scale.set(currentScale, currentScale, currentScale);'''
 new='''      // Living idle respiration plus speech-driven emphasis. Presentation only; never a health/consciousness claim.\n      const idleBreath = 0.012 * Math.sin(time * 1.18) + 0.004 * Math.sin(time * 0.43);\n      const talkPulse = isSpeaking ? (0.035 * Math.sin(time * 8.4) + voiceAmplitude * 0.11) : (voiceAmplitude * 0.06);\n      const currentScale = 1.0 + idleBreath + talkPulse;\n      brainGroup.scale.set(currentScale * (1.0 + 0.003 * Math.sin(time * 0.79)), currentScale * (1.0 + 0.006 * Math.sin(time * 1.07 + 0.8)), currentScale);\n      glassMesh.scale.setScalar(1.0 + 0.0035 * Math.sin(time * 0.91));\n      brainMaterial.emissiveIntensity = 0.82 + 0.12 * Math.sin(time * 1.31) + voiceAmplitude * 0.65;\n      wireframeMaterial.opacity = 0.48 + 0.10 * Math.sin(time * 0.87) + voiceAmplitude * 0.16;'''
 s=once(s,old,new)
 s=once(s,'    const clock = new THREE.Clock();','    const clock = new THREE.Clock();'+formula_js)
 changes[f]=s
plan={'status':'DRY_RUN','files':{str(f):sha(f.read_bytes()) for f in changes},'formulaSource':'machine-consciousness-awareness-v0','formulaExecution':'NOT_EXECUTED_PRESENTATION_ONLY','idleBreathing':True,'sparseFormulaIntervalSeconds':[20,30],'voiceAuthorityChanged':False}
if a.apply:
 out.mkdir(parents=True,exist_ok=False)
 for i,(f,s) in enumerate(changes.items()):shutil.copy2(f,out/(str(i)+'-'+f.name));f.write_text(s,encoding='utf8',newline='\n')
 plan['status']='APPLIED_TESTS_REQUIRED';plan['after']={str(f):sha(f.read_bytes()) for f in changes};(out/'patch.json').write_text(json.dumps(plan,indent=2))
print(json.dumps(plan,indent=2))

import pathlib,sys
p=pathlib.Path(r'E:\LeeWay-Work\agent-lee-fold6-unified\app\src\main\assets\agent_lee_sphere_transparent.html')
s=p.read_text(encoding='utf-8-sig')
old="""      const idleBreath = 0.012 * Math.sin(time * 1.18) + 0.004 * Math.sin(time * 0.43);
      const talkPulse = isSpeaking ? (0.035 * Math.sin(time * 8.4) + voiceAmplitude * 0.11) : (voiceAmplitude * 0.06);
      const currentScale = 1.0 + idleBreath + talkPulse;
      brainGroup.scale.set(currentScale * (1.0 + 0.003 * Math.sin(time * 0.79)), currentScale * (1.0 + 0.006 * Math.sin(time * 1.07 + 0.8)), currentScale);"""
new="""      const idleBreath = 0.022 * Math.sin(time * 1.18) + 0.007 * Math.sin(time * 0.43);
      const talkPulse = isSpeaking ? (0.038 * Math.sin(time * 8.4) + voiceAmplitude * 0.11) : (voiceAmplitude * 0.06);
      const currentScale = 1.0 + idleBreath + talkPulse;
      brainGroup.scale.set(currentScale * (1.0 + 0.006 * Math.sin(time * 0.79)), currentScale * (1.0 + 0.012 * Math.sin(time * 1.07 + 0.8)), currentScale * (1.0 + 0.004 * Math.sin(time * 0.67)));"""
if s.count(old)!=1: raise SystemExit('BREATH_BOUNDARY_MISMATCH')
s=s.replace(old,new)
anchor="window.__leewayFloating={host,blocked,cancel,runText:agentLeeSpeak,testVoice,metrics:()=>voice?.metrics()||null};"
insert="""function nativeTap(point){
 const x=Number(point?.x),y=Number(point?.y);if(!Number.isFinite(x+y)||x<0||y<0||x>innerWidth||y>innerHeight)return false;
 const target=document.elementFromPoint(x,y);const control=target?.closest?.('button');
 if(control){counts.nativeControlTaps=(counts.nativeControlTaps||0)+1;counts.lastNativeTarget=control.id||control.dataset.setting||'button';control.click();return true;}
 if(!blocked()&&(target===canvas||target?.closest?.('#canvas-container'))){const e={clientX:x,clientY:y};if(hit(e)){counts.sphereTaps++;counts.lastNativeTarget='sphere';Promise.resolve(toggleVoiceInteraction()).catch(err=>report(err.message));return true;}}
 return false;
}
window.__leewayFloating={host,blocked,cancel,nativeTap,runText:agentLeeSpeak,testVoice,metrics:()=>voice?.metrics()||null};"""
if s.count(anchor)!=1: raise SystemExit('NATIVE_TAP_BOUNDARY_MISMATCH')
s=s.replace(anchor,insert)
p.write_text(s,encoding='utf-8',newline='\n')
print('PATCHED',p)

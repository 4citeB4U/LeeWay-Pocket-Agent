"""REGION: LEEWAY.VOICE.QUALIFICATION; TAG: REAL_FABRIC_WAVE_TO_AUTHORIZED_PHONE
WHO: Creator-requested audio test. WHAT: Stage an unchanged Voice Fabric waveform for the app's output adapter.
WHEN: After exact APK install. WHERE: owner-authorized ADB, app-private noBackup test folder.
WHY: Native TTS must not replace the selected Voice Fabric speaker.
HOW: Actual current binding + renderer metadata + PCM hash; no playback until UI button tap. LICENSE: MIT.
"""
import argparse,json,pathlib,subprocess,time,hashlib,urllib.request,urllib.parse,base64,struct
p=argparse.ArgumentParser();p.add_argument('--origin',required=True);p.add_argument('--adb',required=True);p.add_argument('--serial',required=True);p.add_argument('--out',required=True);a=p.parse_args()
u=urllib.parse.urlsplit(a.origin);assert u.scheme=='http' and u.hostname in ('127.0.0.1','localhost')
base=[a.adb,'-s',a.serial];out=pathlib.Path(a.out);out.mkdir(parents=True,exist_ok=True)
def request(route,data=None):
 data=None if data is None else json.dumps(data).encode()
 with urllib.request.urlopen(urllib.request.Request(a.origin+route,data=data,headers={'Content-Type':'application/json'}),timeout=90) as r:return json.load(r)
selected=request('/voice/binding')['voiceBinding'];assert selected['authority']=='LEEWAY_VOICE_FABRIC' and not selected['deviceMayOverride'] and not selected['systemVoiceFallback']
text='Creator, this is LeeWay Voice. This is a phone speaker test, using the same selected Agent Lee voice as the computer.'
result=request('/render-voice',{'text':text})
for key in ['agentId','personaFamily','personaArchetypeId','voicePackageId','provider','voiceId','selectionRevision']:assert result[key]==selected[key],key
assert result['voiceAuthority']=='LEEWAY_VOICE_FABRIC'
wave=base64.b64decode(result.pop('audioContent'),validate=True);assert wave[:4]==b'RIFF' and wave[8:12]==b'WAVE'
assert struct.unpack_from('<I',wave,24)[0]==24000 and struct.unpack_from('<H',wave,34)[0]==16
peak=max(abs(v[0]) for v in struct.iter_unpack('<h',wave[44:]));assert peak>0
assert request('/voice/binding')['voiceBinding']['selectionRevision']==selected['selectionRevision']
now=int(time.time()*1000);meta={**result,'capturedAtMs':now,'expiresAtMs':now+3600000,'audioSha256':hashlib.sha256(wave).hexdigest(),'purpose':'OWNER_REQUESTED_PHONE_OUTPUT_TEST_NOT_CONVERSATION','text':text}
(out/'phone-voice-sample.wav').write_bytes(wave);(out/'phone-voice-sample.json').write_text(json.dumps(meta,indent=2))
subprocess.run(base+['shell','run-as','industries.leeway.pocket','mkdir','-p','no_backup/voice-output-test'],check=True,capture_output=True)
for name,data in [('sample.wav',wave),('sample.json',json.dumps(meta).encode())]:
 local=out/('staged-'+name);local.write_bytes(data);temporary='/data/local/tmp/leeway-owner-voice-'+name
 subprocess.run(base+['push',str(local),temporary],check=True,capture_output=True,timeout=30)
 subprocess.run(base+['shell','run-as','industries.leeway.pocket','cp',temporary,'no_backup/voice-output-test/'+name],check=True,capture_output=True,timeout=30)
 copied=subprocess.check_output(base+['exec-out','run-as','industries.leeway.pocket','cat','no_backup/voice-output-test/'+name],timeout=30)
 assert hashlib.sha256(copied).digest()==hashlib.sha256(data).digest(),'PHONE_READBACK_HASH_MISMATCH'
receipt={'state':'VOICE_FABRIC_SAMPLE_STAGED_NOT_YET_PLAYED','voicePackageId':selected['voicePackageId'],'voiceAuthority':selected['authority'],'selectionRevision':selected['selectionRevision'],'audioSha256':meta['audioSha256'],'audioBytes':len(wave),'peakPcm16':peak,'selectedVoiceChanged':False,'systemTtsUsed':False,'phonePlaybackPerformed':False,'humanAudibilityConfirmed':False}
(out/'phone-voice-stage.json').write_text(json.dumps(receipt,indent=2));print(json.dumps(receipt,indent=2))

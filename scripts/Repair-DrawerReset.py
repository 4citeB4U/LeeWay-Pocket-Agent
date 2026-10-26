import pathlib,shutil,json,hashlib
files=[pathlib.Path(r'E:\LeeWay-Work\agent-lee-fold6-unified\app\src\main\assets\agent_lee_sphere_transparent.html'),pathlib.Path(r'E:\LeeWay-Work\pc-consciousness-live\runtime\machine-consciousness\pocket-voice.html')]
out=pathlib.Path(r'E:\LeeWay-Work\agent-lee-fold6-unified\qualification\drawer-reset-20261007');out.mkdir(parents=True,exist_ok=False)
records=[]
for i,p in enumerate(files):
 s=p.read_text(encoding='utf-8-sig');old="openDrawer=function(){cancel();menu=true;oldOpen();host('panel',true).catch(e=>report(e.message));};";new="openDrawer=function(){cancel();menu=true;oldOpen();$('drawer').scrollTop=0;host('panel',true).catch(e=>report(e.message));};"
 if s.count(old)!=1:raise SystemExit('OPEN_DRAWER_BOUNDARY_CHANGED:'+str(p))
 shutil.copy2(p,out/(str(i)+'-'+p.name));s=s.replace(old,new);p.write_text(s,encoding='utf8',newline='\n');records.append({'path':str(p),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
(out/'receipt.json').write_text(json.dumps({'status':'PATCHED_REBUILD_RESTART_REQUIRED','files':records,'voiceAuthorityChanged':False},indent=2),encoding='utf8');print(json.dumps(records,indent=2))
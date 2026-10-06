"""REGION: LEEWAY.DEPLOYMENT.REPAIR; TAG: INSTALLED_SURFACE_PREFLIGHT
WHO: Owner-authorized engineering. WHAT: Capture exact installed APK and app state before repair.
WHEN: Existing app upgrade. WHERE: Selected ADB device and explicitly supplied local PC Brain.
WHY: Never clear owner data or mistake a built candidate for the installed runtime.
HOW: Same-package stop, bounded backups, hashes, read-only schema inspection. LICENSE: MIT.
"""
import argparse, hashlib, json, pathlib, sqlite3, subprocess, tarfile
p=argparse.ArgumentParser();p.add_argument('--adb',required=True);p.add_argument('--serial',required=True);p.add_argument('--out',required=True);p.add_argument('--pc-brain',required=True)
a=p.parse_args();out=pathlib.Path(a.out);out.mkdir(parents=True,exist_ok=False)
package='industries.leeway.pocket';base=[a.adb,'-s',a.serial]
def call(*args):
 r=subprocess.run(base+list(args),capture_output=True,timeout=60,check=True)
 return r.stdout.decode('utf-8',errors='replace').strip()
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
path=call('shell','pm','path','--user','0',package)
assert path.startswith('package:') and '\n' not in path,'SPLIT_APK_REQUIRES_SEPARATE_BACKUP'
call('shell','am','force-stop','--user','0',package)
subprocess.run(base+['pull',path.removeprefix('package:'),str(out/'installed.apk')],check=True,capture_output=True,timeout=120)
dirs=call('shell','run-as',package,'ls').splitlines();include=[x for x in ['databases','shared_prefs','files'] if x in dirs]
assert 'databases' in include,'EXPECTED_EXISTING_DATABASES_MISSING'
with (out/'private-state.tar').open('wb') as f:
 subprocess.run(base+['exec-out','run-as',package,'tar','-cf','-']+include,stdout=f,stderr=subprocess.PIPE,timeout=120,check=True)
assert (out/'private-state.tar').stat().st_size<512*1024*1024,'BACKUP_SIZE_REVIEW_REQUIRED'
records=[]
with tarfile.open(out/'private-state.tar') as tar:
 for member in tar.getmembers():
  if member.isfile() and member.name.startswith('databases/') and member.name.endswith('.db'):
   name=pathlib.PurePosixPath(member.name).name;target=out/name
   data=tar.extractfile(member).read();target.write_bytes(data)
   with sqlite3.connect(target.as_uri()+'?mode=ro',uri=True) as db:
    tables=[x[0] for x in db.execute("SELECT name FROM sqlite_master WHERE type='table'")]
    info={'name':name,'bytes':len(data),'sha256':digest(target),'integrity':db.execute('PRAGMA integrity_check').fetchone()[0],'tables':tables}
    if 'nodes' in tables:
     info['nodes']=db.execute('SELECT count(*) FROM nodes').fetchone()[0]
     info['roots']=db.execute('SELECT id,title FROM nodes WHERE parent_id IS NULL').fetchall()
    records.append(info)
pc=pathlib.Path(a.pc_brain).resolve()
with sqlite3.connect(pc.as_uri()+'?mode=ro',uri=True) as db:
 tables=[x[0] for x in db.execute("SELECT name FROM sqlite_master WHERE type='table'")]
 pcinfo={'path':str(pc),'bytes':pc.stat().st_size,'tables':tables}
 pcinfo['nodesSchema']=db.execute('PRAGMA table_info(nodes)').fetchall()
 pcinfo['nodesCount']=db.execute('SELECT count(*) FROM nodes').fetchone()[0]
 pcinfo['roots']=db.execute('SELECT id,type,title FROM nodes WHERE parent_id IS NULL LIMIT 20').fetchall()
result={'schemaVersion':'leeway.installed-surfaces.preflight.v1','phonePackage':package,'installedApkSha256':digest(out/'installed.apk'),'privateBackupSha256':digest(out/'private-state.tar'),'phoneDatabases':records,'pcBrain':pcinfo,'appStoppedForConsistentBackup':True,'phoneDataCleared':False,'installedCandidate':False}
(out/'preflight.json').write_text(json.dumps(result,indent=2),encoding='utf8');print(json.dumps(result,indent=2))

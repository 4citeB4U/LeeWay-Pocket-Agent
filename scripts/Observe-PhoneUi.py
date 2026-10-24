"""REGION: LEEWAY.DEVICE.QUALIFICATION; TAG: OBSERVED_UI_ACTION_AND_SCREEN
WHO: Creator-authorized Agent Lee. WHAT: Capture the selected phone and act only on observed UI bounds.
WHEN: User-requested overlay/speaker qualification. WHERE: explicit authorized ADB serial.
WHY: No assumed coordinates, permission bypass or synthetic screenshots.
HOW: Real hierarchy and screencap; optional exact node activation; never log unrelated text. LICENSE: MIT.
"""
import argparse,json,pathlib,re,subprocess,xml.etree.ElementTree as ET,time,hashlib
p=argparse.ArgumentParser();p.add_argument('--adb',required=True);p.add_argument('--serial',required=True);p.add_argument('--out',required=True);p.add_argument('--name',required=True);p.add_argument('--text');p.add_argument('--description');p.add_argument('--enable-switch',action='store_true');p.add_argument('--tap',action='store_true');a=p.parse_args()
assert re.fullmatch(r'[A-Za-z0-9._-]+',a.name)
out=pathlib.Path(a.out);out.mkdir(parents=True,exist_ok=True);base=[a.adb,'-s',a.serial]
def call(args):return subprocess.run(base+args,capture_output=True,timeout=25,check=True).stdout
call(['shell','uiautomator','dump','/data/local/tmp/leeway-owner-ui.xml']);raw=call(['exec-out','cat','/data/local/tmp/leeway-owner-ui.xml']);tree=ET.fromstring(raw)
result={'schema':'leeway.observed-phone-ui.v1','action':None,'screenRecorded':True,'target':a.text or a.description}
if a.text or a.description:
 key='text' if a.text else 'content-desc';value=a.text or a.description;matches=[n for n in tree.iter('node') if n.get(key)==value]
 assert len(matches)==1,'UI_TARGET_NOT_UNIQUE: '+str(len(matches))
 target=matches[0]
 if a.enable_switch:
  parents={c:parent for parent in tree.iter() for c in parent};node=target
  while node is not None:
   switches=[n for n in node.iter('node') if n.get('checkable')=='true' and n.get('class')=='android.widget.Switch']
   if len(switches)==1:target=switches[0];break
   node=parents.get(node)
  assert node is not None,'TARGET_SWITCH_NOT_FOUND'
  result['checkedBefore']=target.get('checked')=='true'
 coords=list(map(int,re.findall(r'\d+',target.get('bounds',''))));assert len(coords)==4
 result['observedBounds']=coords
 if a.tap or (a.enable_switch and not result['checkedBefore']):
  assert target.get('enabled')=='true'
  call(['shell','input','tap',str((coords[0]+coords[2])//2),str((coords[1]+coords[3])//2)]);result['action']='OBSERVED_UI_TAP';time.sleep(.8)
image=call(['exec-out','screencap','-p']);index=image.find(b'\x89PNG');assert 0<=index<1024;image=image[index:];(out/(a.name+'.png')).write_bytes(image)
result['imageSha256']=hashlib.sha256(image).hexdigest();(out/(a.name+'.json')).write_text(json.dumps(result,indent=2));print(json.dumps(result))

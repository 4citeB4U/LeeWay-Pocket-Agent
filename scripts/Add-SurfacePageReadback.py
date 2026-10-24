import argparse,pathlib,json,hashlib,shutil
p=argparse.ArgumentParser();p.add_argument('--host',required=True);p.add_argument('--binding',required=True);p.add_argument('--out',required=True);p.add_argument('--apply',action='store_true');a=p.parse_args()
host=pathlib.Path(a.host);binding=pathlib.Path(a.binding);out=pathlib.Path(a.out)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
b=json.loads(binding.read_text(encoding='utf-8-sig'));assert sha(host)==b['nativeAgentUi']['sha256']
s=host.read_text(encoding='utf-8-sig')
needle="""   observation['state']='NATIVE_SURFACE_VISIBLE'
  except Exception as error:observation.update(state='NATIVE_SURFACE_SHOW_FAILED',error={'type':type(error).__name__,'message':str(error)})
  report()
"""
replacement="""   observation['state']='NATIVE_SURFACE_VISIBLE'
   def inspect_page():
    time.sleep(.6)
    try:
     page=target.evaluate_js(\"({url:location.href,title:document.title,viewport:{width:innerWidth,height:innerHeight,dpr:devicePixelRatio},background:getComputedStyle(document.body).backgroundColor,brainReady:window.__leewayLocalBrainReady||null,brainFailure:window.__leewayLocalBrainFailure||null,originalBrain3D:window.__leewayOriginalBrain3D||null,nodeCount:document.querySelectorAll('#nodes .node').length,galaxyActive:document.body.classList.contains('galaxy-active'),returnButton:(document.getElementById('leewayReturnAgent')||document.getElementById('localClose'))?.getBoundingClientRect().toJSON()||null,controlsMenu:document.getElementById('brainControlsMenu')?.open??null})\")
     observation['page']=page
    except Exception as error:observation['pageError']=type(error).__name__+': '+str(error)
    report();receipt('SURFACE_PAGE_OBSERVED',{'surfaceTitle':target.title,'page':observation.get('page'),'pageError':observation.get('pageError')})
   threading.Thread(target=inspect_page,daemon=True,name='LeeWaySurfacePageReadback').start()
  except Exception as error:observation.update(state='NATIVE_SURFACE_SHOW_FAILED',error={'type':type(error).__name__,'message':str(error)})
  report()
"""
if needle not in s:raise SystemExit('SURFACE_SHOW_BOUNDARY_CHANGED')
s=s.replace(needle,replacement,1)
plan={'status':'DRY_RUN','before':sha(host),'behaviorChanged':False}
if a.apply:
 out.mkdir(parents=True,exist_ok=False);shutil.copy2(host,out/'agent-ui-host.before.py');shutil.copy2(binding,out/'binding.before.json');host.write_text(s,encoding='utf8',newline='\n');b['nativeAgentUi']['sha256']=sha(host);binding.write_text(json.dumps(b,indent=2),encoding='utf8');plan.update(status='APPLIED_RESTART_REQUIRED',after=sha(host));(out/'receipt.json').write_text(json.dumps(plan,indent=2),encoding='utf8')
print(json.dumps(plan,indent=2))
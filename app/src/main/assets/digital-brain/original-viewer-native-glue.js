/*
REGION: LEEWAY.BRAIN.VIEWER
TAG: NATIVE_DATA_ADAPTATION_OF_ORIGINAL_GALAXY
WHO: Owner-local viewer; WHAT: Bind the retained renderer to opaque Brain identities.
WHEN: Viewer initialization; WHERE: same original page, no new graph authority.
WHY: Remove old physical-root assumptions and embedded machine data without redrawing the Brain.
HOW: Original layout, renderNodes, draw and HUD; replace data ingress and navigation only.
LICENSE: MIT
*/
let VIEW_ROOT=null;
let localBinding=null;
let historyTraversal=false;
function mapLocalNode(n){return {id:n.id,label:n.label||n.title||n.id,desc:n.type||'object',
 domain:n.type==='file'||n.type==='directory'?'fs':'system',type:n.type,status:n.status,
 parent:n.parent_id,child_count:n.child_count,expandable:n.expandable,source:'This device Digital Brain',
 source_path:n.source_path||'',metadata:n.metadata||{},origin:'OWNER_LOCAL_BRAIN_SQLITE'};}
function currentViewNode(id){return N[id];}
function viewFailure(error){const st=document.getElementById('liveStatus');if(st){st.textContent=error.message||'LOCAL_BRAIN_BLOCKED';st.style.color='#ff6577';}}
function safeView(promise){Promise.resolve(promise).catch(viewFailure);}
// These are renderer projection identities, never replacement device or filesystem identities.
buildGalaxy=function(){if(VIEW_ROOT){scope=VIEW_ROOT;buildScope();}};
hemisphereFor=function(id){let cur=N[id];const seen=new Set();while(cur&&!seen.has(cur.id)){seen.add(cur.id);if(cur.id===VIEW_ROOT+':system')return'L';if(cur.id===VIEW_ROOT+':user')return'R';cur=cur.parent?N[cur.parent]:null;}return null;};
filesystemPathForNode=function(id){return N[id]?.source_path||null;};
isFilesystemNavigationNode=function(id){return N[id]?.type==='directory'||N[id]?.type==='file';};
expandFilesystemNode=async function(id,{offset=0}={}){return localBinding.load(id,offset??0);};
hydratePhysicalFilePreview=async function(id){const el=document.getElementById('physicalFileText');if(el)el.textContent='Metadata record only. File content access is not bound to this read-only viewer.';};
const originalSelectNode=selectNode;
selectNode=function(id){if(!N[id])return;originalSelectNode(id);if(localBinding)safeView(localBinding.inspect(id));};
enterNode=async function(id){if(!N[id])throw Error('VIEWER_NODE_NOT_CACHED');if(!canEnterUniverse(id)){selectNode(id);return;}return localBinding.load(id);};
goHome=function(){if(localBinding){history=[];historyTraversal=true;safeView(localBinding.home().finally(()=>historyTraversal=false));}};
goBack=function(){const id=history.pop();if(id&&localBinding){historyTraversal=true;safeView(localBinding.load(id).finally(()=>historyTraversal=false));}};
updateCrumbs=function(){crumbs.replaceChildren();if(!scope||!N[scope])return;for(const id of ancestry(scope)){const b=document.createElement('button');b.className='crumb';b.textContent=N[id]?.label||id;b.onclick=()=>safeView(localBinding.load(id));crumbs.appendChild(b);}};
// Keep original card geometry. Narrow touch layouts enter expandable nodes on one tap.
nodesLayer.addEventListener('click',event=>{
  const node=event.target.closest?.('.node');
  if(innerWidth>760||!node)return;
  const id=node.dataset.id;
  if(id&&canEnterUniverse(id)){event.preventDefault();event.stopImmediatePropagation();safeView(enterNode(id));}
},true);
previewSectionForNode=function(id){
  const n=N[id];if(!n||n.type!=='file')return '';
  return '<section class="hudSection"><div class="hudSectionTitle">FILE METADATA RECORD</div><p>'+esc(n.label)+'</p><p class="sourcePath">'+esc(n.source_path||'Source location not supplied in this record')+'</p><p>Content preview is not bound. No file contents or content hash are claimed.</p></section>';
};
const hardwareButton=document.getElementById('wdDiagBtn');if(hardwareButton)hardwareButton.onclick=()=>safeView(localBinding.load(VIEW_ROOT+':system:hardware'));
const originalHud=renderHud;
renderHud=function(id){originalHud(id);const button=document.getElementById('enterBtn');if(button)button.onclick=()=>safeView(enterNode(id));};
window.__leewayOriginalRenderer={source:'RECOVERED_GALAXY_RENDERER',nativeReadOnly:true};
localBinding=window.LeeWayLocalBrainBinding({
 upsert(n){N[n.id]=mapLocalNode(n);},
 children(id,children,total,offset){C[id]=children;N[id].child_count=total;N[id].filesystemWindowOffset=offset;},
 root(id){VIEW_ROOT=id;scope=id;history=[];},
 enter(id){if(!historyTraversal&&scope&&scope!==id)history.push(scope);scope=id;selected=null;hud.classList.add('hidden');camera.yaw=0;camera.pitch=0;buildScope();},
 select(id){selectNode(id);},
 relationships(id,links){
  R[id]=[];
  for(const edge of links){const other=edge.source===id?edge.target:edge.source;if(other&&N[other])R[id].push({other,predicate:edge.predicate,dir:edge.source===id?'out':'in',kind:'LOCAL_RECORDED',confidence:null});}
 }
});
window.__leewayLocalBrain=localBinding;
document.getElementById('homeBtn').onclick=goHome;document.getElementById('backBtn').onclick=goBack;
for(const id of ['harnessBtn','harnessM','dataBtn','dataM']){const button=document.getElementById(id);if(button){button.disabled=true;button.title='Capability binding is not supplied by this read-only Brain projection';}}
for(const [button,suffix] of [['leftM',':system'],['rightM',':user'],['appsM',':system:applications'],['runtimeM',':system:runtime']]){
 const el=document.getElementById(button);if(el)el.onclick=()=>safeView(localBinding.load(VIEW_ROOT+suffix));
}
const oldSearch=document.getElementById('search');if(oldSearch){oldSearch.disabled=true;oldSearch.hidden=true;}
const formulaStatus=document.getElementById('formulaV2Status');if(formulaStatus)formulaStatus.textContent='FORMULA · NOT EXECUTED';
const inventoryStatus=document.getElementById('inventoryStatus');if(inventoryStatus)inventoryStatus.textContent='OWNER-LOCAL RECORDS · BOUNDED VIEW';
const organizationStatus=document.getElementById('organizationStatus');if(organizationStatus)organizationStatus.textContent='DIRECT PARENT/CHILD RECORDS';
pulsesPaused=true;
document.getElementById('pulseBtn').textContent='Animate relationships';
document.getElementById('pulseM').textContent='Animate';
window.addEventListener('resize',resize);
localBinding.start().then(()=>{resize();cancelAnimationFrame(raf);raf=requestAnimationFrame(loop);}).catch(viewFailure);

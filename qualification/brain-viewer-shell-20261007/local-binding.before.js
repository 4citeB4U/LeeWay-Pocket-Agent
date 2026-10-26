/*
REGION: LEEWAY.BRAIN.VIEWER_BINDING
TAG: ORIGINAL_RENDERER_OWNER_LOCAL_READS
WHO: Owner-local native host; WHAT: Adapt the original recursive renderer to the existing Brain.
WHEN: Root/descent/page/search/inspector; WHERE: viewer projection only.
WHY: No embedded workstation data, inferred bindings, synthetic execution or alternate Brain.
HOW: Bounded native queries; original renderer/layout/geometry; fail visibly when unbound.
LICENSE: MIT
*/
'use strict';
window.LeeWayLocalBrainBinding = function installLocalBrainBinding(renderer) {
  const native=window.LeeWayBrainView;
  let bodyId=null,rootId=null,active=null,generation=0,searchGeneration=0;
  const cache=new Map();
  const status=document.getElementById('liveStatus');
  function show(message,failed=false){if(status){status.textContent=message;status.style.color=failed?'#ff6577':'#6fe7aa';}}
  async function query(operation,args={}){
    if(!native || typeof native.query!=='function')throw Error('LOCAL_BRAIN_PROVIDER_UNBOUND');
    const raw=await native.query(operation,JSON.stringify(args));
    if(typeof raw!=='string'||raw.length>1048576)throw Error('VIEWER_RESPONSE_LIMIT');
    const envelope=JSON.parse(raw);
    if(envelope.ok!==true)throw Error(envelope.error||'LOCAL_BRAIN_QUERY_BLOCKED');
    if(envelope.source!=='OWNER_LOCAL_BRAIN_SQLITE')throw Error('VIEWER_SOURCE_REJECTED');
    if(typeof envelope.bodyId!=='string'||!envelope.bodyId)throw Error('VIEWER_BODY_REQUIRED');
    if(bodyId && envelope.bodyId!==bodyId)throw Error('VIEWER_BODY_CHANGED');
    return envelope;
  }
  function admit(node){
    if(!node||typeof node.id!=='string'||!(node.id===rootId||node.id.startsWith(rootId+':')))throw Error('VIEWER_NODE_OUTSIDE_BODY');
    if(node.parent_id!=null && !(node.parent_id===rootId||node.parent_id.startsWith(rootId+':')))throw Error('VIEWER_PARENT_OUTSIDE_BODY');
    if(node.status==='tombstoned')throw Error('VIEWER_TOMBSTONE_REJECTED');
    renderer.upsert(node);cache.set(node.id,node);return node;
  }
  async function load(id,offset=0,{navigate=true}={}){
    if(!Number.isInteger(offset)||offset<0)throw Error('VIEWER_PAGE_INVALID');
    const token=++generation;
    try {
      const {result}=await query('children',{id,offset,limit:128});
      if(token!==generation)return null;
      if(result.node?.id!==id||!Array.isArray(result.nodes)||result.nodes.length>128||result.offset!==offset||!Number.isSafeInteger(result.total)||result.total<0)throw Error('VIEWER_PAGE_CONTRACT_INVALID');
      if(result.nodes.some(n=>n.parent_id!==id)||new Set(result.nodes.map(n=>n.id)).size!==result.nodes.length)throw Error('VIEWER_RELATION_IS_NOT_CHILD');
      if(result.hasMore!== (offset+result.nodes.length<result.total))throw Error('VIEWER_PAGE_COUNT_MISMATCH');
      admit(result.node);result.nodes.forEach(admit);
      renderer.children(id,result.nodes.map(n=>n.id),result.total,offset);
      active={id,offset,total:result.total,size:result.nodes.length,hasMore:result.hasMore};
      if(navigate)renderer.enter(id);
      paintPage();show('LOCAL BRAIN · '+bodyId);return result;
    }catch(error){show(error.message,true);throw error;}
  }
  function paintPage(){
    const text=document.getElementById('localPageStatus'),prev=document.getElementById('localPrev'),next=document.getElementById('localNext');
    if(text && active)text.textContent=active.total?`${active.offset+1}–${active.offset+active.size} of ${active.total}`:'Empty universe';
    if(prev)prev.disabled=!active||active.offset===0;
    if(next)next.disabled=!active||!active.hasMore;
  }
  async function inspect(id){
    try{
      const {result}=await query('node',{id});admit(result.node);
      if(!Array.isArray(result.ancestors)||result.ancestors.length>128)throw Error('VIEWER_ANCESTRY_INVALID');
      result.ancestors.forEach(admit);
      const hud=document.getElementById('hud');if(!hud)return;
      const old=hud.querySelector('[data-local-brain-evidence]');old?.remove();
      const section=document.createElement('section');section.dataset.localBrainEvidence='';section.className='hudSection';
      const title=document.createElement('strong');title.textContent='Local Brain evidence';section.appendChild(title);
      const details=document.createElement('dl');
      const metadata=result.node.metadata||{};
      for(const [key,value] of Object.entries(metadata).slice(0,32)){
        const dt=document.createElement('dt'),dd=document.createElement('dd');dt.textContent=key;
        dd.textContent=(typeof value==='string'?value:JSON.stringify(value)).slice(0,4096);details.append(dt,dd);
      }
      section.appendChild(details);
      for(const evidence of (result.provenance||[]).slice(0,24)){
        const item=document.createElement('p');item.textContent=[evidence.kind,evidence.description].filter(Boolean).join(' · ');section.appendChild(item);
      }
      const note=document.createElement('p');note.textContent='Read-only record projection. Content extraction, Formula placement and physical execution are not inferred from this view.';section.appendChild(note);hud.appendChild(section);
      renderer.relationships(id,result.links||[]);
      return result;
    }catch(error){show(error.message,true);throw error;}
  }
  async function search(text){
    const token=++searchGeneration;if(text.trim().length<2)return[];
    const {result}=await query('search',{query:text.trim()});if(token!==searchGeneration)return[];
    if(!Array.isArray(result.nodes)||result.nodes.length>32)throw Error('VIEWER_SEARCH_LIMIT');
    result.nodes.forEach(admit);return result.nodes;
  }
  async function focus(id){const result=await inspect(id);const parent=result.node.parent_id;if(parent){const offset=result.pageOffset;if(!Number.isInteger(offset)||offset<0||offset%128!==0)throw Error('VIEWER_SEARCH_PAGE_INVALID');const page=await load(parent,offset);if(!page?.nodes.some(n=>n.id===id))throw Error('VIEWER_SEARCH_RESULT_NOT_IN_PAGE');}renderer.select(id);}
  async function start(){
    try{
      const envelope=await query('root');bodyId=envelope.bodyId;rootId=envelope.result.node?.id;
      if(rootId!=='brain:'+bodyId)throw Error('VIEWER_ROOT_IDENTITY_MISMATCH');
      admit(envelope.result.node);renderer.root(rootId);await load(rootId);
      document.getElementById('localPrev')?.addEventListener('click',()=>{if(active)load(active.id,Math.max(0,active.offset-128)).catch(()=>{});});
      document.getElementById('localNext')?.addEventListener('click',()=>{if(active?.hasMore)load(active.id,active.offset+active.size).catch(()=>{});});
      document.getElementById('localRefresh')?.addEventListener('click',()=>{if(active)load(active.id,active.offset).catch(()=>{});});
      document.getElementById('localClose')?.addEventListener('click',()=>native.close?.());
      document.getElementById('localEnter')?.addEventListener('click',()=>window.dispatchEvent(new Event('leeway-enter-local-brain')));
      document.getElementById('localSearch')?.addEventListener('input',async event=>{
        const host=document.getElementById('localSearchResults');if(!host)return;host.replaceChildren();
        try {for(const n of await search(event.target.value)){const button=document.createElement('button');button.type='button';button.textContent=n.label||n.id;button.onclick=()=>{focus(n.id).catch(()=>{});host.replaceChildren();};host.appendChild(button);}}
        catch(error){show(error.message,true);}
      });
      window.__leewayLocalBrainReady={bodyId,rootId,scope:'READ_ONLY_LOCAL_SQLITE_PROJECTION'};
      if(native.initialSurface?.()==='hardware'){
        await load(rootId+':system:hardware');
        const started=performance.now();const wait=setInterval(()=>{if(window.__leewayOriginalBrain3D?.ready){clearInterval(wait);window.dispatchEvent(new Event('leeway-enter-local-brain'));}else if(performance.now()-started>10000)clearInterval(wait);},50);
      }
      return {bodyId,rootId};
    }catch(error){show(error.message,true);window.__leewayLocalBrainFailure=error.message;throw error;}
  }
  return Object.freeze({start,load,inspect,search,focus,home:()=>load(rootId),getRoot:()=>rootId,getActive:()=>active});
};

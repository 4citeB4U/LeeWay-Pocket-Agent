/* REGION LEEWAY.UI | TAG A2_THREE_REFERENCE_SCREENS | LICENSE MIT */
(()=>{'use strict';
const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const html=(tag,cls,content)=>{const el=document.createElement(tag);if(cls)el.className=cls;if(content!==undefined)el.innerHTML=content;return el};
function common(key){
 const w=html('div','lw-screen');const shell=html('div','lw-shell');
 const top=html('div','lw-top');const close=html('button','lw-close','× &nbsp; Close Window');close.type='button';close.onclick=()=>{if(window.LeeWayAndroid?.closeSurface){window.LeeWayAndroid.closeSurface();return;}document.getElementById('surface-panel').classList.remove('open','leeway-three-open');window.__leewayFloating?.host('panel',false)};top.append(close,html('div','lw-brand','LEEWAY  /  AGENT LEE'));shell.append(top);w.append(shell);return{w,shell};
}
function footer(shell){shell.append(html('footer','lw-foot','<span>LEEWAY · AGENT LEE</span><span>TRUSTED · TRANSPARENT · VERIFIED</span>'))}
function about(shell){
 shell.append(html('h1','lw-heading','About LeeWay'),html('div','lw-subtitle','Logically Enhanced Engineering for Workload, Authority &amp; Yield.'),html('p','', 'LeeWay is designed to increase human capability without replacing human responsibility. Agent Lee is the user-facing assistant and coordinator across your authorized devices, with explicit permissions and inspectable results.'));
 const rows=[
 ['▣','Your device, your information','Each device has its own Digital Brain to organize authorized file observations, relationships, provenance and physical-device information. Missing permissions and unavailable sensors must remain visible. Continuum is LeeWay’s governed database fabric, with approved federation and local device state.'],
 ['◎','Models are optional','LeeWay does not require an LLM for its implemented deterministic duties. Models remain tools, not owners of identity, permissions or voice. Unsupported requests must be disclosed.'],
 ['▥','One Agent Lee voice','Voice belongs to the shared LeeWay Voice system. Cross-device selection and audibility require their own verification.'],
 ['⚙','Work and services','Agent VT provides a bounded Virtual Tablet. Skills and authorized Device/Commander capabilities supply execution; evidence and receipts record what actually happened.']
 ];
 rows.forEach(([icon,title,body],i)=>shell.append(html('section','lw-panel lw-about-card', '<div class="lw-symbol">'+icon+'</div><div><h3>'+esc(title)+'</h3><p>'+esc(body)+'</p></div>')));
 shell.append(html('section','lw-panel lw-warning','<strong>This installation is under qualification.</strong><p>A component’s presence does not prove every service is connected. Owner control, transparent failure reporting and tested recovery remain required.</p>'));footer(shell);
}
function readSource(key){try{const fn=key==='evidence'?'evidenceSummary':'authoritySummary';const raw=window.LeeWayAndroid?.[fn]?.();return raw?JSON.parse(raw):null}catch(e){return{error:String(e)}}}
async function source(key){const local=readSource(key);if(local)return local;if(key==='evidence'){try{const r=await fetch('/owner-evidence');if(r.ok)return r.json()}catch{}}return null}
function statGrid(shell,values){const grid=html('div','lw-grid');values.forEach(([value,label])=>grid.append(html('div','lw-panel','<div class="lw-stat">'+esc(value)+'</div><div class="lw-muted">'+esc(label)+'</div>')));shell.append(grid)}
async function evidence(shell){
 const data=await source('evidence');const groups=Array.isArray(data?.groups)?data.groups:[];
 shell.append(html('h1','lw-heading','▱ Evidence &amp; Receipts'),html('p','', 'Owner-scoped existing records, grouped by source. No fabricated receipts or invented remote device records.'));
 shell.append(html('div','lw-subtitle','Scope: '+esc(data?.scope||'SOURCE NOT CONNECTED')+' — Read only'));
 statGrid(shell,[[groups.length,'Matching Categories'],[data?.observedFiles??'—','Observed Files'],[Math.min(groups.length,80),'Showing (Current)'],['Read Only','Verified scope not asserted']]);
 const search=html('input','lw-search');search.placeholder='Search receipt category or filename…';search.type='search';search.setAttribute('aria-label','Search evidence');shell.append(search);
 const two=html('div','lw-two');const side=html('nav','lw-side');const records=html('div','lw-records');two.append(side,records);shell.append(two);
 const kinds=['All Categories',...new Set(groups.map(g=>String(g.category||'').split('/')[0]).filter(Boolean))].slice(0,12);let selected='All Categories';
 function refresh(){side.replaceChildren();kinds.forEach(k=>{const b=html('button','',esc(k));b.type='button';b.setAttribute('aria-pressed',String(k===selected));b.onclick=()=>{selected=k;refresh()};side.append(b)});records.replaceChildren();const q=search.value.toLowerCase();const found=groups.filter(g=>(selected==='All Categories'||String(g.category||'').startsWith(selected))&&(String(g.category||'').toLowerCase().includes(q)||(g.recent||[]).some(f=>String(f.name||'').toLowerCase().includes(q))));records.append(html('div','lw-record-row','<strong>'+found.length+' matching categories</strong><span>FILES</span>'));found.slice(0,80).forEach(g=>{const row=html('div','lw-record-row');row.append(html('span','', '▱ '+esc(g.category||'Unnamed category')),html('span','',esc(g.observedFiles??(g.recent||[]).length)));records.append(row)});if(!data||data.error)records.append(html('p','lw-panel','Evidence data unavailable; no substitute records generated.'))}
 search.addEventListener('input',refresh);refresh();footer(shell);
}
async function authority(shell){
 const data=await source('authority');const perms=Array.isArray(data?.permissions)?data.permissions:[];
 shell.append(html('h1','lw-heading','⬡ Authority &amp; Permissions'),html('p','', 'Manage Agent Lee’s authority, device access and permission boundaries.'),html('div','lw-subtitle','Scope: device-owner permissions — observation only'));
 statGrid(shell,[[perms.length||'—','Observed permission categories'],['—','Connected devices not confirmed'],['—','Authority domains not confirmed'],['—','Pending requests not confirmed']]);
 shell.append(html('section','lw-panel lw-scope','<strong>Agent Lee operates only within approved permissions.</strong><p>No access is granted by opening this screen.</p>'));
 const grid=html('div','lw-auth-group');const box=html('section','lw-panel');box.append(html('h3','','Permission Categories'));if(perms.length){perms.forEach(p=>box.append(html('div','lw-record-row','<span>'+esc(p.label)+'</span><strong>'+esc(p.granted?'Granted':'Not granted')+'</strong>')))}else box.append(html('p','','Device permission inventory is not connected or unavailable.'));
 const scope=html('section','lw-panel');scope.append(html('h3','','Authority Scope'),html('p','','Owner-scoped. No permission is inferred from visual design.'));const b=html('button','lw-action','Android app permissions');b.onclick=()=>window.LeeWayAndroid?.openPermissionSettings?.();scope.append(b);
 grid.append(box,scope);shell.append(grid);footer(shell);
}
window.__leewayThreeScreens=async(key,article)=>{if(!['about','evidence','authority'].includes(key))return false;article.replaceChildren();const panel=document.getElementById('surface-panel');panel.classList.add('leeway-three-open');const {w,shell}=common(key);article.append(w);if(key==='about')about(shell);else if(key==='evidence')await evidence(shell);else await authority(shell);return true;};
})();

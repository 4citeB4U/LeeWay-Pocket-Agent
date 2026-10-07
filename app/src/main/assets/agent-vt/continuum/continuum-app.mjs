/* REGION: LEEWAY.CONTINUUM.UI; TAG: LEEWAY.CONTINUUM.UI.DATA.CONTROLLER
WHAT: Connect the supplied sphere to owner-scoped records and source-linked retrieval.
WHY: The user and Agent Lee must inspect the same stored information.
WHO: Leeway Industries / Creator-authorized Agent Lee. WHERE: Existing Continuum workstation surface.
WHEN: 2026-10-07. HOW: A same-origin capability API, truthful empty/error states, bounded rendering.
AGENTS: ASSESS EXECUTE VERIFY. LICENSE: MIT */
import { mountSphere, UNIVERSES } from './continuum-sphere.mjs';

const $ = id => document.getElementById(id);
const titleCase = s => String(s).toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
const categoryMap = new Map(UNIVERSES.map(x => [x.id, x]));
const order = ['people','knowledge','experience','time','work','places','devices','finance','health','media','network','creative','security'];
const state = { universeId:null,query:'',record:null,records:[],cursor:null,total:null,counts:new Map(),status:null,sequence:0,previewSequence:0,paused:matchMedia('(prefers-reduced-motion: reduce)').matches };
const apiBase = new URL(document.querySelector('meta[name="continuum-api"]').content, location.href);
if (apiBase.origin !== location.origin) throw new Error('CONTINUUM_SAME_ORIGIN_REQUIRED');
const sphere = mountSphere($('sphere'), { sphereScale:1.02, autoRotate:!state.paused, reducedMotion:state.paused, maxDpr:1.5, onSelect(selection) {
  if (selection.kind === 'continuum') return goHome();
  if (selection.recordId) return inspect(selection.recordId);
  if (categoryMap.has(selection.universeId)) openUniverse(selection.universeId);
} });
function endpoint(route, params={}) { const u=new URL(apiBase.pathname.replace(/\/$/,'')+'/'+route,apiBase);for(const [k,v]of Object.entries(params))if(v!==null&&v!==undefined&&v!=='')u.searchParams.set(k,String(v));return u; }
async function api(route,params={},options={}) {
  const response=await fetch(endpoint(route,params),{credentials:'same-origin',cache:'no-store',...options});
  const value=await response.json();if(!response.ok||value.ok===false){const error=new Error(value.error||value.reason||value.state||'CONTINUUM_REQUEST_FAILED');error.committed=value.committed===true;error.requestId=value.requestId;error.recordId=value.recordId;throw error;}
  return value.result??value;
}
const formatBytes=n=>n===null||n===undefined?'Not measured':n<1024?n+' B':n<1048576?(n/1024).toFixed(1)+' KiB':(n/1048576).toFixed(1)+' MiB';
const formatCount=n=>Number.isSafeInteger(n)&&n>=0?n.toLocaleString():'—';
function verificationText(verification){
  if(typeof verification==='string')return verification;
  if(!verification||typeof verification!=='object')return 'No verification claim';
  return Object.entries(verification).map(([key,value])=>key+': '+(typeof value==='object'?JSON.stringify(value):String(value))).join(' · ')||'No verification claim';
}
function message(text){$('message').textContent=text||'';}
function statusText(text,error=false){$('connection').dataset.state=error?'error':'connected';$('connection').querySelector('span').textContent=text;}
function updateSphere(){sphere.setData({universes:UNIVERSES.map(u=>({id:u.id,label:u.label,count:state.counts.get(u.id)?.count??null,items:state.universeId===u.id?state.records.slice(0,12).map(r=>({id:r.recordId,label:r.title})):[]}))});}
function renderUniverses(){
  $('universes').replaceChildren();
  for(const id of order){const u=categoryMap.get(id),entry=state.counts.get(id),b=document.createElement('button');b.className='universe';b.style.setProperty('--color',u.color);b.dataset.universe=id;
    const icon=document.createElement('i');icon.textContent='⬡';icon.setAttribute('aria-hidden','true');
    const label=document.createElement('span');label.textContent=titleCase(u.label);const count=document.createElement('small');count.textContent=entry?formatCount(entry.count)+' records':'No provider mapped';label.append(count);b.append(icon,label);b.onclick=()=>openUniverse(id);$('universes').append(b);
  }
}
function canRequestWrite(){return state.status?.initialized!==false&&state.status?.writeExecutorBound===true&&state.status?.writeAdmissionBound===true&&state.universeId!=='all'&&state.counts.has(state.universeId)&&(!Array.isArray(state.status.writeViewIds)||state.status.writeViewIds.includes(state.universeId));}
function updateIntake(){const enabled=canRequestWrite();$('add').disabled=!enabled;$('add').title=enabled?'Request governed storage of an original file':state.status?.writeExecutorBound&&state.status?.writeAdmissionBound?'Open a collection that accepts file imports':state.status?.writeBlockReason||'Governed storage admission is not bound';}
function heading(){
  const u=categoryMap.get(state.universeId);document.documentElement.style.setProperty('--accent',u?.color||'#48dbc5');
  $('heading').textContent=state.query?'Search results':u?titleCase(u.label):state.universeId==='all'?'All records':'Explore your information';
  $('eyebrow').textContent=state.query?'SOURCE-LINKED RETRIEVAL':u?'CONTINUUM UNIVERSE':'YOUR CONTINUUM';
  $('crumb-current').textContent=u?' / '+titleCase(u.label):'';
  $('scope-description').textContent=u?(state.counts.get(u.id)?.description||'Inspect records supplied by the connected owner-scoped provider for this universe.'):'Inspect retained content, recorded experiences, and device file references from their existing owners.';
  $('sphere-heading').textContent=u?titleCase(u.label)+' · your connected records':'One place to see what you know.';
  $('back').disabled=!state.universeId&&!state.query&&!state.record;
  $('universes').hidden=Boolean(state.universeId||state.query);$('records-region').hidden=!$('universes').hidden;
  $('record-count').textContent=formatCount(u?state.counts.get(u.id)?.count:state.status?.recordCount);updateIntake();
}
function renderRecords(){
  const container=$('records');container.replaceChildren();
  for(const r of state.records){const b=document.createElement('button');b.className='record';b.dataset.recordId=r.recordId;const name=document.createElement('span');name.textContent=r.title||r.recordId;
    const sub=document.createElement('span');sub.className='record-subtitle';sub.textContent=[r.collection||r.universeId,r.storageKind==='INDEX_REFERENCE'?'File index reference':r.storageKind==='RETAINED_EVENT'?'Retained event':r.mimeType,r.createdAt].filter(Boolean).join(' · ');
    b.append(name,sub);if(r.snippet){const excerpt=document.createElement('span');excerpt.className='record-snippet';excerpt.textContent=r.snippet;b.append(excerpt);}b.onclick=()=>inspect(r.recordId);container.append(b);
  }
  $('result-count').textContent=state.records.length+' shown'+(state.total===null?'':' / '+formatCount(state.total));
  $('list-caption').textContent=state.query?'Matching stored records':'Available records';$('load-more').hidden=!state.cursor;
  if(!state.records.length)message(state.query?'No stored records match this search.':state.counts.has(state.universeId)?'No records are currently stored in this view.':'This universe has no connected data provider yet.');
  updateSphere();
}
async function loadRecords(append=false){
  const ticket=++state.sequence;if(!append){state.records=[];state.cursor=null;state.total=null;renderRecords();}message('Reading stored records…');$('load-more').disabled=true;
  try{const result=state.query?await api('search',{query:state.query,viewId:state.universeId,limit:32}):await api('records',{viewId:state.universeId,limit:50,cursor:append?state.cursor:null});
    if(ticket!==state.sequence)return;const rows=result.records||result.hits?.map(h=>({...h.record,...h,recordId:h.recordId||h.record?.recordId,title:h.title||h.record?.title}))||[];
    state.records=append?[...state.records,...rows]:rows;state.cursor=result.nextCursor||null;state.total=Number.isInteger(result.total)?result.total:null;message('');renderRecords();
  }catch(e){if(ticket!==state.sequence)return;state.records=[];state.cursor=null;renderRecords();message('Unable to read this view: '+e.message);}
  finally{if(ticket===state.sequence)$('load-more').disabled=false;}
}
function closeRecord(){state.previewSequence++;state.record=null;$('inspector').hidden=true;$('record-content').hidden=true;$('record-content').textContent='';$('record-receipt').hidden=true;$('record-receipt').textContent='';heading();}
async function openUniverse(id){if(!categoryMap.has(id))return;closeRecord();state.universeId=id;state.query='';$('search').value='';state.cursor=null;sphere.selectUniverse(id);heading();await loadRecords();}
function goHome(){state.sequence++;closeRecord();state.universeId=null;state.query='';state.records=[];state.cursor=null;$('search').value='';sphere.selectUniverse(null);heading();message('');updateSphere();}
async function refresh(){
  message('Reading provider state…');
  try{const [status,counts]=await Promise.all([api('status'),api('counts')]);state.status=status;state.counts=new Map((counts.views||counts.universes||[]).map(u=>[u.id||u.universeId,u]));
    if(!Number.isSafeInteger(state.status.recordCount)&&Number.isSafeInteger(counts.total))state.status.recordCount=counts.total;
    $('body-name').textContent=status.bodyId||'Unbound';statusText(status.label||(status.state==='READ_ONLY'?'Connected · read only':'Connected'));
    updateIntake();$('gate').hidden=false;$('gate').textContent=status.writeBlockReason||(status.writeExecutorBound===true&&status.writeAdmissionBound===true?'Files you add are saved as original bytes and checked after storage. Each operation retains its own verification record.':'New intake requires a bound owner storage operation. Existing records remain available for inspection.');
    $('retrieval-mode').textContent=status.retrievalMode||'Source-linked retrieval';renderUniverses();updateSphere();heading();message('');if(state.universeId||state.query)await loadRecords();
  }catch(e){state.status=null;state.counts.clear();state.records=[];state.cursor=null;state.sequence++;$('add').disabled=true;statusText('Connection unavailable',true);$('record-count').textContent='—';$('body-name').textContent='—';$('gate').hidden=false;$('gate').textContent='The database provider could not be read. No sample records are substituted.';renderUniverses();updateSphere();if(state.universeId||state.query)renderRecords();message('Connection failed: '+e.message);}
}
async function inspect(recordId){
  const ticket=++state.previewSequence;message('Opening stored record…');
  try{const value=await api('record',{recordId});if(ticket!==state.previewSequence)return;const record=value.record||value;state.record=record;$('record-title').textContent=record.title||recordId;
    $('record-kind').textContent=(record.storageKind||'STORED RECORD').replaceAll('_',' ');$('record-state').textContent=verificationText(record.verification);
    const details=[['Record identity',record.recordId],['Device body',record.bodyId],['Continuum universe',record.universeId],['Collection',record.collection],['Stored byte length',formatBytes(record.byteLength)],['Stored content SHA-256',record.contentSha256||'Not supplied'],['Source',typeof record.source==='object'?JSON.stringify(record.source):record.source],['Created / captured',record.createdAt||'Not supplied'],['Receipt',record.receiptId||'No receipt supplied by source']];
    const dl=$('record-metadata');dl.replaceChildren();for(const [label,val]of details){const dt=document.createElement('dt'),dd=document.createElement('dd');dt.textContent=label;dd.textContent=String(val??'Not supplied');dl.append(dt,dd);}
    $('record-content').hidden=true;$('record-content').textContent='';$('content-note').textContent=record.storageKind==='INDEX_REFERENCE'?'The retained content is an index record. Its hash does not verify the original file or prove that the original bytes were ingested.':'';
    $('preview-record').disabled=false;$('download-record').disabled=state.status?.downloadAllowed===false;$('receipt-record').disabled=!record.receiptId||typeof record.verification?.receiptSha256!=='string';$('record-receipt').hidden=true;$('record-receipt').textContent='';$('inspector').hidden=false;$('close-record').focus();message('');heading();
  }catch(e){if(ticket===state.previewSequence)message('Unable to inspect record: '+e.message);}
}
async function storedBytes(){
  const record=state.record;if(!record)throw Error('RECORD_REQUIRED');const r=await fetch(endpoint('content',{recordId:record.recordId}),{cache:'no-store',credentials:'same-origin'});
  if(!r.ok){let reason='CONTENT_UNAVAILABLE';try{reason=(await r.json()).error||reason;}catch{}throw Error(reason);}
  const bytes=await r.arrayBuffer();if(Number.isSafeInteger(record.byteLength)&&bytes.byteLength!==record.byteLength)throw Error('CONTENT_LENGTH_MISMATCH');if(record.contentSha256){const actual=Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',bytes))).map(n=>n.toString(16).padStart(2,'0')).join('');if(actual!==record.contentSha256)throw Error('CONTENT_HASH_MISMATCH');}
  return{record,bytes,mime:r.headers.get('content-type')||record.mimeType||'application/octet-stream'};
}
$('preview-record').onclick=async()=>{const ticket=state.previewSequence;$('preview-record').disabled=true;try{const{bytes,mime}=await storedBytes();if(ticket!==state.previewSequence)return;if(!/^(text\/|application\/(json|xml))/.test(mime)){message('This binary original is available with Download. A text preview is not provided.');return;}$('record-content').textContent=new TextDecoder('utf-8',{fatal:false}).decode(bytes.slice(0,98304));$('record-content').hidden=false;if(bytes.byteLength>98304)$('content-note').textContent='Preview limited to 96 KiB. Download retains all stored bytes.';}catch(e){if(ticket===state.previewSequence)message('Content read failed: '+e.message);}finally{if(ticket===state.previewSequence)$('preview-record').disabled=false;}};
$('download-record').onclick=async()=>{const ticket=state.previewSequence;$('download-record').disabled=true;try{const{record,bytes,mime}=await storedBytes();if(ticket!==state.previewSequence)return;const u=URL.createObjectURL(new Blob([bytes],{type:mime})),a=document.createElement('a');a.href=u;a.download=record.storageKind==='INDEX_REFERENCE'?record.recordId.replace(/[^a-z0-9_.-]/gi,'_')+'.json':record.title||'continuum-record';a.click();setTimeout(()=>URL.revokeObjectURL(u),1000);}catch(e){if(ticket===state.previewSequence)message('Download failed: '+e.message);}finally{if(ticket===state.previewSequence)$('download-record').disabled=state.status?.downloadAllowed===false;}};
function canonicalReceipt(value){return value===null||typeof value!=='object'?JSON.stringify(value):Array.isArray(value)?'['+value.map(canonicalReceipt).join(',')+']':'{'+Object.keys(value).sort().map(key=>JSON.stringify(key)+':'+canonicalReceipt(value[key])).join(',')+'}';}
$('receipt-record').onclick=async()=>{const ticket=state.previewSequence,record=state.record;if(!record?.receiptId)return;$('receipt-record').disabled=true;try{const result=await api('receipt',{receiptId:record.receiptId}),receipt=result.receipt||result,{receiptSha256,...payload}=receipt;if(receipt.receiptId!==record.receiptId||receipt.bodyId!==record.bodyId||receipt.recordId!==record.recordId||receiptSha256!==record.verification?.receiptSha256||await sha256Bytes(new TextEncoder().encode(canonicalReceipt(payload)))!==receiptSha256)throw Error('RECEIPT_HASH_OR_SCOPE_MISMATCH');if(ticket!==state.previewSequence)return;$('record-receipt').textContent=JSON.stringify(receipt,null,2);$('record-receipt').hidden=false;message('Storage receipt hash and record scope checked.');}catch(e){if(ticket===state.previewSequence)message('Storage receipt unavailable: '+e.message);}finally{if(ticket===state.previewSequence)$('receipt-record').disabled=false;}};
$('search-form').onsubmit=e=>{e.preventDefault();const query=$('search').value.trim();if(query.length<2){message('Enter at least two characters to search stored records.');return;}closeRecord();state.query=query;heading();loadRecords();};
$('home').onclick=goHome;$('back').onclick=()=>state.record?closeRecord():goHome();$('close-record').onclick=closeRecord;$('refresh').onclick=refresh;$('load-more').onclick=()=>loadRecords(true);
$('all-records').onclick=()=>{closeRecord();state.universeId='all';state.query='';$('search').value='';sphere.selectUniverse(null);heading();loadRecords();};
$('view-mode').onclick=()=>{const list=$('workspace').classList.toggle('list-mode');$('view-mode').textContent=list?'Sphere view':'List view';$('view-mode').setAttribute('aria-pressed',String(list));};
$('motion').onclick=()=>{state.paused=!state.paused;sphere.setMotion?.({autoRotate:!state.paused,reducedMotion:state.paused});if(!sphere.setMotion){message('Motion follows the device reduced-motion setting.');return;}$('motion').textContent=state.paused?'Resume rotation':'Pause rotation';$('motion').setAttribute('aria-pressed',String(state.paused));};
$('add').onclick=()=>{if(canRequestWrite())$('file-input').click();};
const pendingImports=new Map();
async function sha256Bytes(bytes){return Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',bytes))).map(n=>n.toString(16).padStart(2,'0')).join('');}
function importRequest(key){let id=pendingImports.get(key);try{id=id||sessionStorage.getItem(key);}catch{}if(!id)id=crypto.randomUUID();pendingImports.set(key,id);try{sessionStorage.setItem(key,id);}catch{}return id;}
function completeImport(key){pendingImports.delete(key);try{sessionStorage.removeItem(key);}catch{}}
$('file-input').onchange=async()=>{
  const file=$('file-input').files?.[0];if(!file)return;
  if(!canRequestWrite()){message('Open a collection with owner intake enabled before adding a file.');$('file-input').value='';return;}
  const viewId=state.universeId,bodyId=state.status.bodyId,mimeType=file.type||'application/octet-stream';
  const limit=state.status.importMaxBytes;if(Number.isSafeInteger(limit)&&file.size>limit){message('This file exceeds the '+formatBytes(limit)+' import limit.');$('file-input').value='';return;}
  message('Storing and verifying '+file.name+'…');$('add').disabled=true;let requestId,key;
  try{
    const bytes=await file.arrayBuffer(),contentSha256=await sha256Bytes(bytes);
    key='continuum.import.'+await sha256Bytes(new TextEncoder().encode(JSON.stringify({bodyId,viewId,title:file.name,mimeType,contentSha256})));
    requestId=importRequest(key);
    const result=await api('import',{title:file.name,mimeType,viewId,requestId},{method:'POST',headers:{'Content-Type':'application/octet-stream','X-Continuum-Request':'owner-import'},body:bytes});
    const stored=result.record||result;
    if(stored.contentSha256!==contentSha256||stored.byteLength!==bytes.byteLength){const error=new Error('CONTINUUM_IMPORT_RESULT_MISMATCH');error.committed=true;error.recordId=stored.recordId;throw error;}
    completeImport(key);await refresh();message('Stored original bytes and received persistence evidence.');
  }catch(e){message((e.committed?'Bytes were committed, but verification needs reconciliation. ':'Import was not confirmed. ')+e.message+(requestId?' · Request '+requestId:'')+(requestId?' · Selecting the same file again will reuse this request ID.':''));}
  finally{$('file-input').value='';updateIntake();}
};
$('return-agent').onclick=()=>{if(parent!==window){parent.postMessage({type:'leeway.continuum.close'},location.origin);return;}if(window.LeeWayContinuumHost?.close){window.LeeWayContinuumHost.close();return;}if(location.hostname==='appassets.androidplatform.net'){location.assign('/agent-vt/continuum/close');return;}history.back();};
window.addEventListener('keydown',e=>{if(e.key==='Escape'&&state.record)closeRecord();});
window.addEventListener('pagehide',()=>{state.sequence++;state.previewSequence++;sphere.dispose();},{once:true});
window.__continuum={refresh,openUniverse,goHome,inspect,snapshot:()=>({universeId:state.universeId,query:state.query,recordId:state.record?.recordId||null,recordCount:state.records.length,total:state.total,bodyId:state.status?.bodyId||null,connected:Boolean(state.status),sphere:sphere.getState()})};
$('motion').textContent=state.paused?'Resume rotation':'Pause rotation';$('motion').setAttribute('aria-pressed',String(state.paused));heading();renderUniverses();refresh();

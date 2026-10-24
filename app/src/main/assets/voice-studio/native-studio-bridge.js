/* REGION: LEEWAY.VOICE.ANDROID; TAG: ORIGINAL_STUDIO_TYPED_PAIRED_API_TRANSPORT
WHO: Owner in the bundled canonical Studio. WHAT: Carry only its allowed JSON APIs through the existing paired runtime adapter.
No credentials, arbitrary URLs, native commands or voice defaults are exposed. LICENSE: MIT. */
(()=>{'use strict';
 const native=window.LeeWayVoiceStudio;if(!native?.request)return;
 const originalFetch=window.fetch.bind(window),pending=new Map();let sequence=0;
 const allowed=new Set(['GET /api/local/status','GET /api/local/voices','GET /api/provider/status','POST /api/local/synthesize','GET /api/agent-lee/selection/session','POST /api/agent-lee/selection']);
 window.__leewayStudioReply=(id,raw)=>{const request=pending.get(String(id));if(!request)return;pending.delete(String(id));clearTimeout(request.timer);try{const result=JSON.parse(raw);request.resolve(new Response(typeof result.body==='string'?result.body:JSON.stringify(result.body),{status:result.status,headers:{'Content-Type':'application/json','Cache-Control':'no-store'}}));}catch(e){request.reject(e);}};
 window.fetch=function(input,init={}){
  const url=new URL(typeof input==='string'||input instanceof URL?String(input):input.url,location.href);
  if(!url.pathname.startsWith('/api/'))return originalFetch(input,init);
  const method=String(init.method||(input instanceof Request?input.method:'GET')).toUpperCase();
  if(url.origin!==location.origin||url.search||!allowed.has(method+' '+url.pathname))return Promise.resolve(new Response(JSON.stringify({error:'VOICE_STUDIO_API_NOT_ADMITTED_ON_PAIRED_ROUTE'}),{status:403,headers:{'Content-Type':'application/json'}}));
  const body=init.body===undefined?'':init.body;
  if(typeof body!=='string'||body.length>8192||pending.size>=4)return Promise.reject(Error('VOICE_STUDIO_REQUEST_BOUNDARY'));
  const headers=new Headers(init.headers),csrf=headers.get('X-LeeWay-Owner-CSRF')||'',id=String(++sequence);
  return new Promise((resolve,reject)=>{
   const cancel=()=>{const item=pending.get(id);if(item){pending.delete(id);clearTimeout(item.timer);reject(new DOMException('Aborted','AbortError'));}};
   if(init.signal?.aborted){cancel();reject(new DOMException('Aborted','AbortError'));return;}
   const timer=setTimeout(()=>{pending.delete(id);reject(Error('PAIRED_VOICE_STUDIO_REQUEST_TIMEOUT'));},165000);
   pending.set(id,{resolve,reject,timer});init.signal?.addEventListener('abort',cancel,{once:true});
   try{native.request(id,method,url.pathname,body,csrf)}catch(e){pending.delete(id);clearTimeout(timer);reject(e)}
  });
 };
 addEventListener('DOMContentLoaded',()=>{const note=document.createElement('p');note.className='status';note.textContent='Owner-paired Voice Studio: voice generation and shared publication use your paired PC. Local auditions and tuning run here. This is not standalone-phone model execution.';document.getElementById('studioMain')?.prepend(note);});
})();

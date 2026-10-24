/* Existing paired model inventory transport. One read operation; no caller URL,
 * model command, credentials or selection is exposed. LICENSE: Existing terms. */
(()=>{'use strict';
  const native=window.LeeWayModels;if(!native?.readInventory)return;
  const originalFetch=window.fetch.bind(window),pending=new Map();let sequence=0;
  window.__leewayModelsReply=(id,raw)=>{
    const item=pending.get(String(id));if(!item)return;pending.delete(String(id));item.cleanup();
    try{
      if(typeof raw!=='string'||raw.length>2500000)throw Error('MODEL_INVENTORY_RESPONSE_LIMIT');
      const result=JSON.parse(raw);
      item.resolve(new Response(JSON.stringify(result.body),{status:result.status,headers:{'Content-Type':'application/json','Cache-Control':'no-store'}}));
    }catch(error){item.reject(error);}
  };
  window.fetch=function(input,init={}){
    const url=new URL(typeof input==='string'||input instanceof URL?String(input):input.url,location.href);
    if(!url.pathname.startsWith('/api/'))return originalFetch(input,init);
    const method=String(init.method||(input instanceof Request?input.method:'GET')).toUpperCase();
    if(url.origin!==location.origin||url.pathname!=='/api/models/inventory'||url.search||url.hash||method!=='GET'||init.body!==undefined){
      return Promise.resolve(new Response('{"error":"MODEL_INVENTORY_REQUEST_NOT_ADMITTED"}',{status:403,headers:{'Content-Type':'application/json'}}));
    }
    if(pending.size>=2)return Promise.reject(Error('MODEL_INVENTORY_BUSY'));
    const id=String(++sequence);
    return new Promise((resolve,reject)=>{
      if(init.signal?.aborted){reject(new DOMException('Aborted','AbortError'));return;}
      const cancel=()=>{const item=pending.get(id);if(item){pending.delete(id);item.cleanup();reject(new DOMException('Aborted','AbortError'));}};
      const timer=setTimeout(()=>{const item=pending.get(id);if(item){pending.delete(id);item.cleanup();reject(Error('PAIRED_MODEL_INVENTORY_TIMEOUT'));}},12000);
      const cleanup=()=>{clearTimeout(timer);init.signal?.removeEventListener('abort',cancel);};
      pending.set(id,{resolve,reject,cleanup});init.signal?.addEventListener('abort',cancel,{once:true});
      try{native.readInventory(id);}catch(error){pending.delete(id);cleanup();reject(error);}
    });
  };
})();

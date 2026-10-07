/* LeeWay Continuum verified original-file preview.
 * All inputs are bytes already returned and SHA-256 checked by storedBytes().
 * No HTML file execution, no external media URL, no unverified source hydration.
 */
const text=(el,value)=>{el.textContent=value;return el};
const create=(tag,cls,content)=>{
 const e=document.createElement(tag);if(cls)e.className=cls;
 if(content!==undefined)text(e,content);
 return e;
};
const ext=r=>String(r?.title||'').split('.').at(-1).toLowerCase();
function canonicalMime(record,headerMime){
 const declared=String(record?.mimeType||headerMime||'').split(';')[0].trim().toLowerCase();
 if(declared.startsWith('image/')||declared.startsWith('audio/')||declared.startsWith('video/')||declared==='application/pdf'
 ||declared==='application/vnd.openxmlformats-officedocument.wordprocessingml.document'||declared==='application/msword'
 ||declared.startsWith('text/')||declared==='application/json'||declared==='application/xml')return declared;
 const extensions={pdf:'application/pdf',png:'image/png',jpg:'image/jpeg',jpeg:'image/jpeg',gif:'image/gif',webp:'image/webp',
  mp3:'audio/mpeg',wav:'audio/wav',ogg:'audio/ogg',m4a:'audio/mp4',mp4:'video/mp4',webm:'video/webm',
  docx:'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  txt:'text/plain',md:'text/plain',json:'application/json',xml:'application/xml'};
 return extensions[ext(record)]||declared||'application/octet-stream';
}
const isWord=(mime,r)=>mime==='application/vnd.openxmlformats-officedocument.wordprocessingml.document'||ext(r)==='docx';
const originalSizeLimit=48*1024*1024;
export function createContinuumMediaViewer(host,{getVerifiedBytes,onInfo}={}){
 if(!(host instanceof HTMLElement)||typeof getVerifiedBytes!=='function')throw TypeError('VERIFIED_MEDIA_VIEWER_CONTRACT_REQUIRED');
 let generation=0,urls=[],media=[];
 const revoke=()=>{for(const m of media){try{m.pause()}catch{}m.removeAttribute('src');m.load?.();}media=[];for(const url of urls)URL.revokeObjectURL(url);urls=[]};
 const clear=()=>{generation++;revoke();host.replaceChildren();host.hidden=true;};
 const urlFor=(bytes,mime)=>{const uri=URL.createObjectURL(new Blob([bytes],{type:mime}));urls.push(uri);return uri};
 const note=(value)=>{const p=create('p','viewer-note',value);host.append(p);onInfo?.(value)};
 const show=async(record)=>{
  clear();const ticket=generation;
  host.hidden=false;host.append(create('p','viewer-loading','Verifying original bytes before display…'));
  try{
   const result=await getVerifiedBytes(record);
   if(ticket!==generation)return{state:'SUPERSEDED'};
   if(result.bytes.byteLength>originalSizeLimit)throw Error('PREVIEW_SIZE_BUDGET_EXCEEDED');
   const mime=canonicalMime(record,result.mime),title=String(record.title||record.recordId);
   host.replaceChildren();
   const head=create('div','viewer-file-head');
   head.append(create('strong','viewer-file-name',title),create('span','viewer-file-format',mime));
   host.append(head);
   if(record.storageKind==='INDEX_REFERENCE'){
    note('This is the stored index representation. It does not prove that the original referenced file was imported or remains accessible.');
   }
   if(mime.startsWith('image/')){
    const img=create('img','viewer-image');img.src=urlFor(result.bytes,mime);img.alt=title;
    img.decoding='async';host.append(img);return{state:'VERIFIED_MEDIA_RENDERED',format:'image'};
   }
   if(mime.startsWith('video/')){
    const v=create('video','viewer-video');v.controls=true;v.preload='metadata';v.playsInline=true;v.src=urlFor(result.bytes,mime);
    v.setAttribute('aria-label','Video player for '+title);media.push(v);host.append(v);return{state:'VERIFIED_MEDIA_RENDERED',format:'video'};
   }
   if(mime.startsWith('audio/')){
    const v=create('audio','viewer-audio');v.controls=true;v.preload='metadata';v.src=urlFor(result.bytes,mime);
    v.setAttribute('aria-label','Audio player for '+title);media.push(v);host.append(v);
    const ctrl=create('div','viewer-audio-actions');
    for(const [name,offset]of [['Back 10 seconds',-10],['Forward 10 seconds',10]]){
     const button=create('button','subtle',name);button.type='button';
     button.onclick=()=>{if(Number.isFinite(v.duration))v.currentTime=Math.min(Math.max(0,v.currentTime+offset),v.duration)};
     ctrl.append(button);
    }
    const label=create('label','viewer-speed','Playback speed ');
    const speed=create('select');speed.setAttribute('aria-label','Audio playback speed');
    for(const rate of [.5,.75,1,1.25,1.5,2]){
      const option=create('option',null,rate+'×');option.value=String(rate);if(rate===1)option.selected=true;speed.append(option);
    }
    speed.onchange=()=>{v.playbackRate=Number(speed.value)};label.append(speed);ctrl.append(label);host.append(ctrl);
    return{state:'VERIFIED_MEDIA_RENDERED',format:'audio'};
   }
   if(mime==='application/pdf'){
    const bytes=new Uint8Array(result.bytes,0,Math.min(8,result.bytes.byteLength));
    if(String.fromCharCode(...bytes.slice(0,4))!=='%PDF')throw Error('PDF_MAGIC_INVALID');
    const iframe=create('iframe','viewer-pdf');iframe.title='PDF viewer: '+title;iframe.src=urlFor(result.bytes,mime);
    iframe.setAttribute('loading','lazy');host.append(iframe);
    note('PDF viewing uses the device browser’s local PDF reader. Download remains available.');
    return{state:'VERIFIED_MEDIA_RENDERED',format:'pdf'};
   }
   if(isWord(mime,record)){
    if(ext(record)==='doc')throw Error('LEGACY_DOC_BINARY_VIEW_UNAVAILABLE');
    const bytes=new Uint8Array(result.bytes,0,4);
    if(bytes[0]!==0x50||bytes[1]!==0x4b)throw Error('DOCX_ZIP_SIGNATURE_INVALID');
    if(!globalThis.mammoth?.convertToHtml||!globalThis.DOMPurify?.sanitize)throw Error('WORD_LOCAL_VIEWER_NOT_LOADED');
    const resultDoc=await globalThis.mammoth.convertToHtml({arrayBuffer:result.bytes});
    if(ticket!==generation)return{state:'SUPERSEDED'};
    const node=create('div','viewer-word');
    const clean=globalThis.DOMPurify.sanitize(resultDoc.value,{
      USE_PROFILES:{html:true},
      FORBID_TAGS:['style','form','iframe','script','object','embed','video','audio','svg','math'],
      FORBID_ATTR:['srcset','autoplay','onload','onclick','onerror'],
      ALLOW_DATA_ATTR:false
    });
    node.innerHTML=clean;
    for(const a of node.querySelectorAll('a')){a.removeAttribute('target');a.removeAttribute('href')}
    for(const img of node.querySelectorAll('img')){if(!img.src.startsWith('data:image/'))img.removeAttribute('src')}
    host.append(node);
    note('Word document content is rendered locally from the verified DOCX. Complex Office layouts may differ; Download preserves the original.');
    if(resultDoc.messages?.length)note('Word reader reported '+resultDoc.messages.length+' formatting or content notices.');
    return{state:'VERIFIED_MEDIA_RENDERED',format:'word'};
   }
   if(mime.startsWith('text/')||['application/json','application/xml'].includes(mime)){
    const limit=1048576;
    const body=new Uint8Array(result.bytes.slice(0,limit));
    const decoded=new TextDecoder('utf-8',{fatal:false}).decode(body);
    const pre=create('pre','viewer-text',decoded);pre.tabIndex=0;host.append(pre);
    if(result.bytes.byteLength>limit)note('Showing the first 1 MiB of the verified record. Download preserves all stored bytes.');
    return{state:'VERIFIED_MEDIA_RENDERED',format:'text'};
   }
   note('The original bytes passed the existing integrity check, but no safe inline viewer is available for this file type. Use Download.');
   return{state:'VERIFIED_MEDIA_UNSUPPORTED_FORMAT',format:mime};
  }catch(e){
   if(ticket!==generation)return{state:'SUPERSEDED'};
   host.replaceChildren();note('File preview unavailable: '+(e instanceof Error?e.message:'VERIFIED_CONTENT_READ_FAILED'));
   return{state:'PREVIEW_BLOCKED',reason:e instanceof Error?e.message:'UNAVAILABLE'};
  }
 };
 return Object.freeze({show,clear,dispose:clear,snapshot:()=>({visible:!host.hidden,mediaCount:media.length,generation})});
}

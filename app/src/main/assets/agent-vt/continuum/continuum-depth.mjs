/* LeeWay Continuum — hierarchical visual navigation over verified owner records.
 * Collections are presentation groupings; directories only appear when an actual
 * parent record or recorded relativePath supplies evidence. Never generate files.
 */
export const FOCUS_PAGE_SIZE=12;
const text=v=>typeof v==='string'&&v.trim()?v.trim():null;
export function recordedSegments(record){
 // Indexed references may cite a source registry path without containing that
 // original file. Only owner-retained original files can supply folder ancestry.
 if(record?.storageKind==='INDEX_REFERENCE'||(record?.kind!=='file'&&record?.storageKind!=='RETAINED_ORIGINAL'))return [];
 const raw=text(record?.relativePath)||text(record?.source?.relativePath)||text(record?.source?.recordedRelativePath);
 if(!raw||raw.length>1024||raw.includes('\0'))return [];
 const tokens=raw.replaceAll('\\','/').split('/').filter(Boolean);
 if(tokens.some(s=>s==='.'||s==='..'||s.length>220)||tokens.length>18)return [];
 return tokens.slice(0,-1);
}
export function collectionName(record){return text(record?.collection)||'Unassigned records'}
export function recordsForPath(records,path=[]){
 let rows=records;
 for(const node of path){
  if(node.type==='collection')rows=rows.filter(r=>collectionName(r)===node.value);
  else if(node.type==='folder')rows=rows.filter(r=>recordedSegments(r).slice(0,node.depth).join('/')===node.value);
  else if(node.type==='parent')rows=rows.filter(r=>r.source?.parentSourceRecordId===node.value);
  else if(node.type==='directory')rows=rows.filter(r=>r.parentRecordId===node.recordId||r.source?.parentRecordId===node.recordId);
  else return [];
 }
 return rows;
}
const sortName=(a,b)=>a.label.localeCompare(b.label,undefined,{numeric:true,sensitivity:'base'});
export function visibleHierarchy(records,path=[]){
 if(!Array.isArray(records)||!Array.isArray(path))throw TypeError('CONTINUUM_HIERARCHY_INPUT_INVALID');
 const items=records.filter(r=>r&&typeof r.recordId==='string'&&r.recordId.length>0);
 const rows=recordsForPath(items,path);
 if(path.length===0){
  const groups=new Map();
  for(const record of rows){
   const collection=collectionName(record),group=groups.get(collection)||{type:'collection',key:'collection:'+collection,label:collection,value:collection,loaded:0,description:'COLLECTION · source-backed'};
   group.loaded++;groups.set(collection,group);
  }
  const groupsSorted=[...groups.values()].sort(sortName);
  const recordPreviews=rows.map(toRecordNode);
  return{nodes:[...groupsSorted,...recordPreviews],totalLoaded:rows.length,scopeLabel:'UNIVERSE',hasMoreLoaded:false,groupCount:groups.size};
 }
 const folders=new Map(),files=[],dirRecords=[];
 const folderDepth=path.filter(n=>n.type==='folder').length;
 const prefix=path.filter(n=>n.type==='folder').at(-1)?.value||'';
 const pathParts=prefix?prefix.split('/'):[];
 const ancestors=new Map();
 for(const r of rows){
  const parentId=r.source?.parentSourceRecordId;
  const alreadySelected=path.some(x=>x.type==='parent'&&x.value===parentId);
  if(typeof parentId==='string'&&parentId.length>0&&parentId.length<=4096&&!alreadySelected){
   const label=typeof r.source.parentLabel==='string'&&r.source.parentLabel.trim()?r.source.parentLabel.trim():parentId.split('::').at(-1);
   const existing=ancestors.get(parentId)||{type:'parent',key:'parent:'+parentId,label,value:parentId,loaded:0,description:'ACTUAL BRAIN PARENT'};
   existing.loaded++;ancestors.set(parentId,existing);continue;
  }
  const parts=recordedSegments(r);
  if(parts.length>pathParts.length&&parts.slice(0,pathParts.length).join('/')===prefix){
   const next=parts[pathParts.length];
   const newPath=[...pathParts,next].join('/');
   const prev=folders.get(newPath)||{type:'folder',key:'folder:'+newPath,label:next,value:newPath,depth:pathParts.length+1,loaded:0,description:'RECORDED SOURCE PATH'};
   prev.loaded++;folders.set(newPath,prev);continue;
  }
  if(r.kind==='directory'||r.kind==='folder'){
   dirRecords.push({type:'directory',key:'directory:'+r.recordId,label:text(r.title)||'Unnamed directory',recordId:r.recordId,description:'RECORDED DIRECTORY'});
   continue;
  }
  files.push(toRecordNode(r));
 }
 const parentNodes=[...ancestors.values()];
 const labelCounts=new Map();
 for(const parent of parentNodes)labelCounts.set(parent.label,(labelCounts.get(parent.label)||0)+1);
 for(const parent of parentNodes){
  if(labelCounts.get(parent.label)>1){
   const parts=parent.value.split('::').filter(Boolean);
   const qualifier=parts.at(-2)||parts.at(-3)||'distinct parent';
   parent.label=parent.label+' · '+qualifier;
  }
 }
 const nodes=parentNodes.sort(sortName).concat([...folders.values()].sort(sortName),dirRecords.sort(sortName),files);
 return{nodes,totalLoaded:rows.length,scopeLabel:path.at(-1)?.label||'COLLECTION',hasMoreLoaded:false,groupCount:ancestors.size+folders.size+dirRecords.length};
}
export function toRecordNode(record){
 const mime=typeof record.mimeType==='string'?record.mimeType.toLowerCase():'';
 const type=record.storageKind==='INDEX_REFERENCE'?'INDEXED REFERENCE':
   mime.startsWith('video/')?'VIDEO':mime.startsWith('audio/')?'AUDIO':
   mime.startsWith('image/')?'IMAGE':mime.includes('pdf')?'PDF':
   mime.includes('word')||/\.(docx|doc)$/i.test(record.title||'')?'WORD DOCUMENT':
   mime.startsWith('text/')?'TEXT':'SOURCE RECORD';
 return{type:'record',key:'record:'+record.recordId,label:text(record.title)||record.recordId,recordId:record.recordId,description:type,
   collection:collectionName(record),mimeType:record.mimeType||null,sourceKind:record.storageKind||record.kind||'RECORD'};
}
export function canopyPositions(count,mobile=false){
 if(!Number.isInteger(count)||count<0||count>FOCUS_PAGE_SIZE)throw RangeError('FOCUS_RENDER_BUDGET_EXCEEDED');
 const sizes=mobile?[2,3,2,3,2]:[4,5,3];
 const out=[];
 for(let row=0;row<sizes.length&&out.length<count;row++){
  const countInRow=Math.min(sizes[row],count-out.length);
  const planned=sizes[row],pitch=mobile?0.19:0.30;
  for(let col=0;col<countInRow;col++){
   const nx=(col-(planned-1)/2)*(mobile?0.30:0.19);
   const ny=(row-(sizes.length-1)/2)*pitch;
   const r=Math.min(.95,Math.hypot(nx,ny)*1.25);
   const curvature=1-Math.sqrt(Math.max(0,1-r*r));
   out.push({x:50+nx*100,y:50+ny*100,z:Math.round(-180*curvature),
     rotateY:Math.round(-26*nx),rotateX:Math.round(18*ny),
     opacity:Math.max(.78,1-.22*curvature)});
  }
 }
 return out;
}
export function canopyPage(nodes,page=0,size=FOCUS_PAGE_SIZE){
 if(!Number.isInteger(page)||page<0||!Number.isInteger(size)||size<1)throw TypeError('PAGE_INVALID');
 const count=Math.ceil(nodes.length/size);
 return{items:nodes.slice(page*size,(page+1)*size),page,totalPages:count,hasNext:page+1<count,hasPrev:page>0};
}

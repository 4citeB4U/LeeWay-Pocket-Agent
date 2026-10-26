$ErrorActionPreference='Stop'
$pc='E:\LeeWay-Work\pc-consciousness-live\runtime\machine-consciousness\pocket-voice.html'
$backup='E:\LeeWay-Work\c3-vision-lane-a-qualification\qualification\pc-golden-before-direct-vision-widget-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')+'.html'
$s=[IO.File]::ReadAllText($pc)
$old="if(key==='vision'){const panel=document.getElementById('surface-panel');if(!panel)return;panel.replaceChildren();const back=document.createElement('button');back.textContent='Back to Agent Lee';back.onclick=()=>{panel.classList.remove('open');panel.replaceChildren();window.__leewayFloating?.host?.('panel',false)};const viewer=document.createElement('iframe');viewer.title='Agent Lee Vision';viewer.src='https://leeway-agent-lee-vision.vercel.app/';viewer.allow='camera; microphone; fullscreen';viewer.style.cssText='width:100%;height:77vh;border:0;background:transparent;border-radius:0';back.style.cssText='position:fixed;right:12px;top:12px;z-index:1000;background:rgba(7,23,41,.55);border:0;border-radius:50%;color:#fff';panel.append(back,viewer);panel.classList.add('open');await window.__leewayFloating?.host?.('panel',true);return;}"
if(([regex]::Matches($s,[regex]::Escape($old))).Count -ne 1){throw 'PC_VISION_ROUTE_DRIFT'}
Copy-Item $pc $backup
$replacement=@'
if(key==='vision'){
 const previous=document.getElementById('leeway-vision-eye-widget');
 if(previous){previous.remove();return;}
 const host=document.createElement('div');host.id='leeway-vision-eye-widget';host.setAttribute('role','dialog');host.setAttribute('aria-label','Agent Lee floating Vision Eye');
 host.style.cssText='position:fixed;left:12vw;top:9vh;width:min(76vw,1050px);height:min(77vh,810px);z-index:1600;background:transparent;border:0;box-shadow:none;resize:both;overflow:hidden;min-width:300px;min-height:250px';
 const eye=document.createElement('iframe');eye.title='Agent Lee Vision';eye.src='https://leeway-agent-lee-vision.vercel.app/';eye.allow='camera;fullscreen';eye.style.cssText='display:block;width:100%;height:100%;border:0;background:transparent;border-radius:0';
 const grip=document.createElement('button');grip.type='button';grip.textContent='Move Vision';grip.title='Drag floating Eye';grip.style.cssText='position:absolute;top:4px;left:4px;z-index:2;background:rgba(4,20,38,.32);color:#fff;border:0;border-radius:16px;cursor:move;padding:5px 9px;opacity:.25';grip.onpointerenter=()=>grip.style.opacity='1';grip.onpointerleave=()=>grip.style.opacity='.25';
 let offset=null;grip.onpointerdown=e=>{offset={x:e.clientX-host.offsetLeft,y:e.clientY-host.offsetTop};grip.setPointerCapture(e.pointerId)};grip.onpointermove=e=>{if(!offset)return;host.style.left=Math.max(0,e.clientX-offset.x)+'px';host.style.top=Math.max(0,e.clientY-offset.y)+'px'};grip.onpointerup=()=>offset=null;grip.onpointercancel=()=>offset=null;
 const close=document.createElement('button');close.type='button';close.textContent='×';close.title='Close Vision';close.setAttribute('aria-label','Close Vision');close.style.cssText='position:absolute;right:4px;top:4px;z-index:2;background:rgba(120,10,22,.68);color:white;border:0;border-radius:50%;width:34px;height:34px';close.onclick=()=>host.remove();
 host.append(eye,grip,close);document.body.appendChild(host);return;
}
'@
$s=$s.Replace($old,$replacement)
[IO.File]::WriteAllText($pc,$s,[Text.UTF8Encoding]::new($false))
[pscustomobject]@{status='PC_EXISTING_EYE_DIRECT_WIDGET_MOUNT';backup=$backup;sha256=(Get-FileHash $pc).Hash}|ConvertTo-Json

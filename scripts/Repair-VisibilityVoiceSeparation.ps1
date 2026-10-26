<# REGION: LEEWAY.UI.BEHAVIOR; TAG: VISIBILITY_NOT_VOICE_AUTHORITY
WHO: Creator-authorized Agent Lee. WHAT: Make edge emblem a show/collapse toggle only.
WHEN: Regression where hidden UI disabled interaction. WHERE: existing Windows edge tab/native host.
WHY: LeeWay Voice is independent of visual presence. HOW: named toggle event + existing host show/hide; no voice binding mutation.
LICENSE: MIT #>
[CmdletBinding()]param([Parameter(Mandatory=$true)][string]$BodyRoot,[Parameter(Mandatory=$true)][string]$DeviceRoot,[Parameter(Mandatory=$true)][string]$EvidenceRoot)
Set-StrictMode -Version Latest;$ErrorActionPreference='Stop'
$edge=Join-Path $BodyRoot 'runtime\machine-consciousness\Start-Pocket-Edge-Overlay.ps1'
$hostFile=Join-Path $DeviceRoot 'providers\desktop-commander\native-host\agent-ui-host.py'
$bindingPath=Join-Path $BodyRoot 'host-commander.binding.v1.json'
if(Test-Path $EvidenceRoot){throw 'EVIDENCE_ROOT_EXISTS'};New-Item -ItemType Directory -Path $EvidenceRoot|Out-Null
Copy-Item $edge (Join-Path $EvidenceRoot 'edge.before.ps1');Copy-Item $hostFile (Join-Path $EvidenceRoot 'host.before.py');Copy-Item $bindingPath (Join-Path $EvidenceRoot 'binding.before.json')
$e=[IO.File]::ReadAllText($edge)
$old="if(!`$script:moved){Start-Process powershell.exe -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-WindowStyle','Hidden','-File',('`"'+`$launcher+'`"')) -WindowStyle Hidden}"
$new=@"
if(!`$script:moved){
 try{
  `$binding=Get-Content (Join-Path `$root 'host-commander.binding.v1.json') -Raw|ConvertFrom-Json
  `$hash=[Security.Cryptography.SHA256]::Create();try{`$id=([BitConverter]::ToString(`$hash.ComputeHash([Text.Encoding]::UTF8.GetBytes((Join-Path `$root 'host-commander.binding.v1.json').ToLowerInvariant())))).Replace('-','').ToLowerInvariant().Substring(0,24)}finally{`$hash.Dispose()}
  try{`$toggle=[Threading.EventWaitHandle]::OpenExisting('Local\\LeeWayAgentLeeToggle_'+`$id);try{[void]`$toggle.Set()}finally{`$toggle.Dispose()}}
  catch{Start-Process powershell.exe -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-WindowStyle','Hidden','-File',('`"'+`$launcher+'`"')) -WindowStyle Hidden}
 }catch{Start-Process powershell.exe -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-WindowStyle','Hidden','-File',('`"'+`$launcher+'`"')) -WindowStyle Hidden}
}
"@
if(([regex]::Matches($e,[regex]::Escape($old))).Count -ne 1){throw 'EDGE_TOGGLE_BOUNDARY_CHANGED'};$e=$e.Replace($old,$new);[IO.File]::WriteAllText($edge,$e,(New-Object Text.UTF8Encoding($false)))
$h=[IO.File]::ReadAllText($hostFile)
$old="wake=kernel32.CreateEventW(None,False,False,wake_name)"
$new="wake=kernel32.CreateEventW(None,False,False,wake_name)`ntoggle_name='Local\\\\LeeWayAgentLeeToggle_'+hashlib.sha256(str(binding_path).casefold().encode()).hexdigest()[:24]`ntoggle=kernel32.CreateEventW(None,False,False,toggle_name)"
if(([regex]::Matches($h,[regex]::Escape($old))).Count -ne 1){throw 'HOST_TOGGLE_CREATE_BOUNDARY'};$h=$h.Replace($old,$new)
$old="   if kernel32.WaitForSingleObject(ctypes.c_void_p(wake),0)==0:`n    api.geometry('panel',False);window.show();window.restore();style_native()"
$new="   if kernel32.WaitForSingleObject(ctypes.c_void_p(wake),0)==0:`n    api.geometry('panel',False);window.show();window.restore();style_native()`n   if kernel32.WaitForSingleObject(ctypes.c_void_p(toggle),0)==0:`n    if window.hidden:window.show();window.restore();style_native()`n    else:window.hide()`n    receipt('NATIVE_VISIBILITY_TOGGLED',{'visible':not window.hidden,'voiceAuthorityChanged':False})"
if(([regex]::Matches($h,[regex]::Escape($old))).Count -ne 1){throw 'HOST_TOGGLE_LOOP_BOUNDARY'};$h=$h.Replace($old,$new)
$old="kernel32.CloseHandle(ctypes.c_void_p(wake));kernel32.CloseHandle(ctypes.c_void_p(lock))"
$new="kernel32.CloseHandle(ctypes.c_void_p(toggle));kernel32.CloseHandle(ctypes.c_void_p(wake));kernel32.CloseHandle(ctypes.c_void_p(lock))"
if(([regex]::Matches($h,[regex]::Escape($old))).Count -ne 1){throw 'HOST_CLOSE_BOUNDARY'};$h=$h.Replace($old,$new);[IO.File]::WriteAllText($hostFile,$h,(New-Object Text.UTF8Encoding($false)))
python -m py_compile $hostFile;if($LASTEXITCODE -ne 0){throw 'HOST_PARSE_FAILED'}
$t=$null;$errs=$null;[void][Management.Automation.Language.Parser]::ParseFile($edge,[ref]$t,[ref]$errs);if($errs.Count){throw 'EDGE_PARSE_FAILED'}
$b=Get-Content $bindingPath -Raw|ConvertFrom-Json;$b.nativeAgentUi.sha256=(Get-FileHash $hostFile -Algorithm SHA256).Hash;$b|ConvertTo-Json -Depth 10|Set-Content $bindingPath -Encoding UTF8
[ordered]@{status='VISIBILITY_TOGGLE_SOURCE_REPAIRED_RESTART_REQUIRED';voiceBindingChanged=$false;edgeSha256=(Get-FileHash $edge -Algorithm SHA256).Hash;hostSha256=(Get-FileHash $hostFile -Algorithm SHA256).Hash}|ConvertTo-Json|Set-Content (Join-Path $EvidenceRoot 'receipt.json') -Encoding UTF8

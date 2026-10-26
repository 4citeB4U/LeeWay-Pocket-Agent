<# REGION: LEEWAY.NATIVE.REPAIR; TAG: INSTALLED_INTERACTION_AND_VT_GATE
WHO: Creator-authorized Agent Lee. WHAT: Run exact-source repair and actual native input qualification.
WHEN: Current failed installed UI. WHERE: Resolved existing host and VT lineage only.
WHY: A compiled/rendered shell is not a usable workstation. HOW: Parse/hash/dry-run/backup/apply/restart/probe.
LICENSE: MIT #>
[CmdletBinding()]
param([Parameter(Mandatory=$true)][ValidateSet('Prepare','Apply','Restart','Probe')][string]$Phase,[Parameter(Mandatory=$true)][string]$BodyRoot,[Parameter(Mandatory=$true)][string]$DeviceRoot,[Parameter(Mandatory=$true)][string]$VtRoot,[Parameter(Mandatory=$true)][string]$EvidenceRoot,[ValidateSet('observe','menu','close','shrink','grow','drag','expand','sphere','workstation')][string]$Action='observe')
Set-StrictMode -Version Latest;$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$patch=Join-Path $PSScriptRoot 'Repair-InteractionAndVt.py'
$receipt=Join-Path $BodyRoot 'receipts\native-agent-ui\host-receipt.json'
if($Phase -in @('Prepare','Apply')){
 Add-Type -AssemblyName System.Speech
 $speech=[System.Speech.Recognition.SpeechRecognitionEngine].Assembly.Location
 python -m py_compile $patch;if($LASTEXITCODE -ne 0){throw 'PATCH_PARSE_FAILED'}
 Write-Output ('PATCH_SHA256='+(Get-FileHash $patch -Algorithm SHA256).Hash)
 $args=@('--body',$BodyRoot,'--device',$DeviceRoot,'--vt-preview-root',$VtRoot,'--speech-assembly',$speech,'--out',(Join-Path $EvidenceRoot 'backup'))
 if($Phase -eq 'Apply'){$args+='--apply'}
 python $patch @args;if($LASTEXITCODE -ne 0){throw 'SOURCE_PATCH_FAILED'}
 if($Phase -eq 'Apply'){
  $b=Get-Content (Join-Path $BodyRoot 'host-commander.binding.v1.json') -Raw|ConvertFrom-Json
  python -m py_compile $b.nativeAgentUi.source;if($LASTEXITCODE -ne 0){throw 'NATIVE_HOST_PARSE_FAILED'}
  $t=$null;$e=$null;[void][Management.Automation.Language.Parser]::ParseFile((Join-Path $BodyRoot 'Start-Agent-Lee.ps1'),[ref]$t,[ref]$e);if($e.Count){throw 'LAUNCHER_PARSE_FAILED'}
 }
}elseif($Phase -eq 'Restart'){
 $patchRecord=Get-Content (Join-Path $EvidenceRoot 'backup\patch.json') -Raw|ConvertFrom-Json
 foreach($entry in $patchRecord.outputs.PSObject.Properties){if((Get-FileHash -LiteralPath $entry.Name -Algorithm SHA256).Hash.ToLowerInvariant() -ne $entry.Value){throw 'SOURCE_CHANGED_AFTER_REPAIR'}}
 $binding=Get-Content (Join-Path $BodyRoot 'host-commander.binding.v1.json') -Raw|ConvertFrom-Json
 $existing=@(Get-CimInstance Win32_Process|Where-Object{$_.Name -match '^python(w)?\.exe$' -and $_.CommandLine -and $_.CommandLine.Contains($binding.nativeAgentUi.source)})
 if($existing.Count -gt 1){throw 'LIVE_HOST_OWNER_AMBIGUOUS'}
 Copy-Item -LiteralPath $receipt -Destination (Join-Path $EvidenceRoot 'before-restart-host.json')
 if($existing.Count -eq 1){Stop-Process -Id $existing[0].ProcessId}else{Write-Output 'PREVIOUS_HOST_ALREADY_EXITED_NO_OTHER_PROCESS_STOPPED'}
 Start-Sleep -Seconds 2
 $geometry=Join-Path $BodyRoot 'agent-ui-geometry.json'
 if(Test-Path $geometry){Copy-Item $geometry (Join-Path $EvidenceRoot 'before-restart-geometry.json');$g=Get-Content $geometry -Raw|ConvertFrom-Json;$g.width=320;$g.height=320;$g|ConvertTo-Json|Set-Content $geometry -Encoding UTF8}
 & (Join-Path $BodyRoot 'Start-Agent-Lee.ps1')
 'RESTARTED_EXISTING_HOST_NATIVE_ACCEPTANCE_PENDING'
}else{
 $script=Join-Path $PSScriptRoot 'Probe-InstalledInteraction.py'
 python -m py_compile $script;if($LASTEXITCODE -ne 0){throw 'PROBE_PARSE_FAILED'}
 python $script --receipt $receipt --out (Join-Path $EvidenceRoot 'after') --action $Action
 if($LASTEXITCODE -ne 0){throw ('ACTUAL_NATIVE_INPUT_FAILED:'+ $Action)}
}

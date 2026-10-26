<# REGION: LEEWAY.DEVICE.LAUNCHER; TAG: NATIVE_FLOATING_AGENT
WHO: Device owner. WHAT: Open Agent Lee through its qualified native device adapter.
WHEN: Existing shortcut. WHERE: installed body bootstrap; real source locations live in its device binding.
WHY: Never silently reopen an opaque browser application. HOW: Hash admission, live identity and one native-host mutex.
LICENSE: MIT #>
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$bindingPath=Join-Path $PSScriptRoot 'host-commander.binding.v1.json'
$binding=Get-Content -LiteralPath $bindingPath -Raw|ConvertFrom-Json
$ui=[Uri]$binding.agentUiUri
if(!$ui.IsLoopback -or $ui.Scheme -ne 'http'){throw 'OWNER_LOCAL_UI_BINDING_REQUIRED'}
$hostBinding=$binding.nativeAgentUi
if($hostBinding.provider -ne 'LEEWAY_DEVICE_BRIDGE_NATIVE_UI' -or !(Test-Path -LiteralPath $hostBinding.source)){throw 'NATIVE_AGENT_UI_NOT_BOUND'}
if((Get-FileHash -LiteralPath $hostBinding.source -Algorithm SHA256).Hash -ne $hostBinding.sha256){throw 'NATIVE_AGENT_UI_SOURCE_INTEGRITY_FAILED'}
$origin=$ui.GetLeftPart([UriPartial]::Authority)
try{$health=Invoke-RestMethod ($origin+'/health') -TimeoutSec 8}catch{
 $bootstrap=Join-Path $PSScriptRoot 'runtime\machine-consciousness\Start-LeeWay-Machine-Consciousness.ps1'
 if(!(Test-Path $bootstrap)){throw 'EXISTING_RUNTIME_BOOTSTRAP_MISSING'}
 & $bootstrap -NoUI|Out-Null
 $health=Invoke-RestMethod ($origin+'/health') -TimeoutSec 12
}
if($health.identity -ne 'LEEWAY_MACHINE_CONSCIOUSNESS'){throw 'CARRIER_IDENTITY_MISMATCH'}
$python=(Get-Command python -ErrorAction Stop).Source
$evidence=Join-Path $PSScriptRoot 'receipts\native-agent-ui'
[void][IO.Directory]::CreateDirectory($evidence)
$tabSource=Join-Path $PSScriptRoot 'runtime\machine-consciousness\Start-Pocket-Edge-Overlay.ps1'
$tabOwners=@(Get-CimInstance Win32_Process|Where-Object{$_.Name -eq 'powershell.exe' -and $_.CommandLine -and $_.CommandLine.Contains($tabSource) -and $_.CommandLine -notmatch '-Command'})
if($tabOwners.Count -eq 0){
 if(!(Test-Path $tabSource)){throw 'EXISTING_EXTERNAL_TAB_SOURCE_MISSING'}
 Start-Process powershell.exe -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',('"'+$tabSource+'"'),'-EvidenceDirectory',('"'+(Join-Path $evidence 'external-tab')+'"')) -WindowStyle Hidden
}
$existing=@(Get-CimInstance Win32_Process|Where-Object{$_.Name -match '^python(w)?\.exe$' -and $_.CommandLine -and $_.CommandLine.Contains($hostBinding.source)})
if($existing.Count -gt 1){throw 'MULTIPLE_NATIVE_UI_OWNERS_REQUIRE_REVIEW'}
if($existing.Count -eq 1){Write-Output ('EXISTING_NATIVE_AGENT_UI_PID='+$existing[0].ProcessId);return}
$p=Start-Process -FilePath $python -ArgumentList @('-u',('"'+$hostBinding.source+'"'),'--binding',('"'+$bindingPath+'"'),'--evidence',('"'+$evidence+'"')) -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $evidence 'stdout.log') -RedirectStandardError (Join-Path $evidence 'stderr.log')
Write-Output ('NATIVE_AGENT_UI_PID='+$p.Id)
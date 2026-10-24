<#
REGION: LEEWAY.DEVICE.DEPLOYMENT
TAG: EXISTING_LAUNCHER_NATIVE_HOST_BINDING
WHO: Creator-authorized Agent Lee. WHAT: Replace browser-app launch with the qualified native Device Bridge host.
WHEN: Owner-requested installed repair. WHERE: Existing body bootstrap and existing device binding only.
WHY: Leaving the old launcher recreates the opaque application on the next click.
HOW: Exact source hashes, local backups, parsed payload, observed resource binding, no system-wide path identity.
LICENSE: MIT
#>
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$BodyRoot,[Parameter(Mandatory=$true)][string]$NativeHost,[Parameter(Mandatory=$true)][string]$EvidenceRoot,[switch]$Apply)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$launcher=Join-Path $BodyRoot 'Start-Agent-Lee.ps1'
$bindingPath=Join-Path $BodyRoot 'host-commander.binding.v1.json'
if((Get-FileHash $launcher -Algorithm SHA256).Hash -ne 'D34C1B898F94966379A402DC71E32E67B4E2B688CB2AB553C82213B943774B78'){throw 'OLD_LAUNCHER_CHANGED'}
if((Get-FileHash $NativeHost -Algorithm SHA256).Hash -ne '00D07E26CC53E79DAE6EC75745ABC1CEA03E9D747838CBF5027A5B86A37B2045'){throw 'NATIVE_HOST_CHANGED_SINCE_VISUAL_QUALIFICATION'}
$binding=Get-Content $bindingPath -Raw|ConvertFrom-Json
$descriptor=[ordered]@{provider='LEEWAY_DEVICE_BRIDGE_NATIVE_UI';source=(Get-Item $NativeHost).FullName;sha256=(Get-FileHash $NativeHost -Algorithm SHA256).Hash;ownership='DEVICE_LOCAL_OBSERVED_BINDING';mode='TRANSPARENT_FRAMELESS_TOPMOST'}
$binding|Add-Member -NotePropertyName nativeAgentUi -NotePropertyValue $descriptor -Force
$payload=@'
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
$existing=@(Get-CimInstance Win32_Process|Where-Object{$_.Name -match '^python(w)?\.exe$' -and $_.CommandLine -and $_.CommandLine.Contains($hostBinding.source)})
if($existing.Count -gt 1){throw 'MULTIPLE_NATIVE_UI_OWNERS_REQUIRE_REVIEW'}
if($existing.Count -eq 1){Write-Output ('EXISTING_NATIVE_AGENT_UI_PID='+$existing[0].ProcessId);return}
$p=Start-Process -FilePath $python -ArgumentList @('-u',('"'+$hostBinding.source+'"'),'--binding',('"'+$bindingPath+'"'),'--evidence',('"'+$evidence+'"')) -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $evidence 'stdout.log') -RedirectStandardError (Join-Path $evidence 'stderr.log')
Write-Output ('NATIVE_AGENT_UI_PID='+$p.Id)
'@
$tokens=$null;$errors=$null;[void][Management.Automation.Language.Parser]::ParseInput($payload,[ref]$tokens,[ref]$errors)
if($errors.Count){throw 'NATIVE_LAUNCH_PAYLOAD_PARSE_FAILED'}
$plan=[ordered]@{state='DRY_RUN';descriptor=$descriptor;browserFallback=$false;voiceSelectionChanged=$false;ownerDataCleared=$false}
if(!$Apply){$plan|ConvertTo-Json -Depth 5;return}
[void][IO.Directory]::CreateDirectory($EvidenceRoot)
Copy-Item $launcher (Join-Path $EvidenceRoot 'launcher-before-native.ps1')
Copy-Item $bindingPath (Join-Path $EvidenceRoot 'binding-before-native.json')
$utf8=New-Object Text.UTF8Encoding($false)
[IO.File]::WriteAllText($launcher,$payload,$utf8)
$binding|ConvertTo-Json -Depth 12|Set-Content -LiteralPath $bindingPath -Encoding UTF8
$sourceCopy=Join-Path $BodyRoot 'runtime\machine-consciousness\deployment\windows\Start-Agent-Lee.ps1'
[IO.File]::WriteAllText($sourceCopy,$payload,$utf8)
$plan.state='NATIVE_LAUNCHER_INSTALLED'
$plan['launcherSha256']=(Get-FileHash $launcher -Algorithm SHA256).Hash
$plan['bindingSha256']=(Get-FileHash $bindingPath -Algorithm SHA256).Hash
$plan|ConvertTo-Json -Depth 7|Set-Content -LiteralPath (Join-Path $EvidenceRoot 'launcher-repair.json') -Encoding UTF8
$plan|ConvertTo-Json -Depth 7

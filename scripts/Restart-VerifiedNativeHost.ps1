<# REGION: LEEWAY.NATIVE.REPAIR; TAG: SINGLE_VERIFIED_HOST_RESTART
WHO: Creator-authorized repair. WHAT: Restart only the bound native Agent Lee host after a verified source patch.
WHEN: Repair retest. WHERE: Existing binding/launcher. WHY: Source edits do not update a running process.
HOW: Hash check, zero/one owner check, compile, stop exact owner, existing launcher, readback. LICENSE: MIT #>
[CmdletBinding()]param([Parameter(Mandatory=$true)][string]$BodyRoot)
Set-StrictMode -Version Latest;$ErrorActionPreference='Stop'
$b=Get-Content (Join-Path $BodyRoot 'host-commander.binding.v1.json') -Raw|ConvertFrom-Json
if($b.nativeAgentUi.provider -ne 'LEEWAY_DEVICE_BRIDGE_NATIVE_UI'){throw 'OWNER_PROVIDER_MISMATCH'}
if((Get-FileHash $b.nativeAgentUi.source -Algorithm SHA256).Hash -ne $b.nativeAgentUi.sha256){throw 'NATIVE_SOURCE_HASH_MISMATCH'}
python -m py_compile $b.nativeAgentUi.source;if($LASTEXITCODE -ne 0){throw 'NATIVE_SOURCE_PARSE_FAILED'}
$owners=@(Get-CimInstance Win32_Process|Where-Object{$_.Name -match '^python(w)?\.exe$' -and $_.CommandLine -and $_.CommandLine.Contains($b.nativeAgentUi.source)})
if($owners.Count -gt 1){throw 'MULTIPLE_OWNERS_REVIEW_REQUIRED'}
if($owners.Count -eq 1){Write-Output ('STOP_BOUND_HOST_PID='+$owners[0].ProcessId);Stop-Process -Id $owners[0].ProcessId;Start-Sleep -Seconds 1}
& (Join-Path $BodyRoot 'Start-Agent-Lee.ps1')
Write-Output ('ADMITTED_SOURCE_SHA256='+$b.nativeAgentUi.sha256)

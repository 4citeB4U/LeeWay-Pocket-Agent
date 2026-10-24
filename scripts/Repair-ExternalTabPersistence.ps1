<# REGION: LEEWAY.DEVICES.UI.REPAIR; TAG: KEEP_NATIVE_TAB_ON_CURRENT_LAUNCH_PATH
WHO: Creator-authorized Agent Lee. WHAT: Restore the existing external tab and include it in the corrected launcher.
WHEN: Actual installed repair after transient volume absence. WHERE: Existing Windows body only.
WHY: Old background tab process was no longer present; evidence I/O must not kill the visual window.
HOW: Exact edits, startup artwork hash, bounded error reporting, one existing mutex-protected tab. LICENSE: MIT #>
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$BodyRoot,[Parameter(Mandatory=$true)][string]$EvidenceRoot,[switch]$Apply)
Set-StrictMode -Version Latest;$ErrorActionPreference='Stop'
$p=Join-Path $BodyRoot 'runtime\machine-consciousness\Start-Pocket-Edge-Overlay.ps1';$launcher=Join-Path $BodyRoot 'Start-Agent-Lee.ps1'
$s=[IO.File]::ReadAllText($p).Replace("`r`n","`n");$l=[IO.File]::ReadAllText($launcher).Replace("`r`n","`n")
function Once([string]$v,[string]$a,[string]$b){if(([regex]::Matches($v,[regex]::Escape($a))).Count-ne 1){throw ('EXACT_BOUNDARY_CHANGED:'+ $a)};$v.Replace($a,$b)}
$s=Once $s '$original=[Drawing.Bitmap]::FromFile($logo)' '$artworkHash=(Get-FileHash $logo -Algorithm SHA256).Hash
$original=[Drawing.Bitmap]::FromFile($logo)'
$s=Once $s 'artworkSha256=(Get-FileHash $logo -Algorithm SHA256).Hash' 'artworkSha256=$artworkHash'
$s=Once $s ' if($EvidenceDirectory){' ' if($EvidenceDirectory -and [IO.Directory]::Exists($EvidenceDirectory)){try{'
$s=Once $s "`n }`n})" "`n }catch{`$script:lastEvidenceFailure=`$_.Exception.GetType().Name}}`n})"
$s=Once $s 'Set-Content $saved ([string]$window.Top);if(!$script:moved)' 'try{Set-Content $saved ([string]$window.Top)}catch{$script:lastEvidenceFailure=$_.Exception.GetType().Name};if(!$script:moved)'
$l=Once $l '$existing=@(Get-CimInstance Win32_Process' @'
$tabSource=Join-Path $PSScriptRoot 'runtime\machine-consciousness\Start-Pocket-Edge-Overlay.ps1'
$tabOwners=@(Get-CimInstance Win32_Process|Where-Object{$_.Name -eq 'powershell.exe' -and $_.CommandLine -and $_.CommandLine.Contains($tabSource) -and $_.CommandLine -notmatch '-Command'})
if($tabOwners.Count -eq 0){
 if(!(Test-Path $tabSource)){throw 'EXISTING_EXTERNAL_TAB_SOURCE_MISSING'}
 Start-Process powershell.exe -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',('"'+$tabSource+'"'),'-EvidenceDirectory',('"'+(Join-Path $evidence 'external-tab')+'"')) -WindowStyle Hidden
}
$existing=@(Get-CimInstance Win32_Process
'@
foreach($text in @($s,$l)){$t=$null;$e=$null;[void][Management.Automation.Language.Parser]::ParseInput($text,[ref]$t,[ref]$e);if($e.Count){throw 'PERSISTENCE_PATCH_PARSE_FAILED'}}
if(!$Apply){[pscustomobject]@{status='DRY_RUN';tabSource=$p;launcher=$launcher;newService=$false}|ConvertTo-Json;return}
[void][IO.Directory]::CreateDirectory($EvidenceRoot);Copy-Item $p (Join-Path $EvidenceRoot 'external-tab.before.ps1');Copy-Item $launcher (Join-Path $EvidenceRoot 'launcher.before-tab-binding.ps1')
$encoding=New-Object Text.UTF8Encoding($false);[IO.File]::WriteAllText($p,$s,$encoding);[IO.File]::WriteAllText($launcher,$l,$encoding)
[IO.File]::WriteAllText((Join-Path $BodyRoot 'runtime\machine-consciousness\deployment\windows\Start-Agent-Lee.ps1'),$l,$encoding)
[ordered]@{status='EXTERNAL_TAB_REBOUND_TO_NATIVE_LAUNCHER';tabSha256=(Get-FileHash $p -Algorithm SHA256).Hash;launcherSha256=(Get-FileHash $launcher -Algorithm SHA256).Hash;sourceIdentityChanged=$false}|ConvertTo-Json|Set-Content (Join-Path $EvidenceRoot 'external-tab-binding.json') -Encoding UTF8
& $launcher

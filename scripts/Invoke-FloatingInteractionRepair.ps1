<#
REGION: LEEWAY.DEVICE.UI.REPAIR
TAG: OWNER_APPROVED_FLOATING_INTERACTION_REPAIR
WHO: Creator-authorized Agent Lee. WHAT: Restore installed floating host/interaction, not a new runtime.
WHEN: Oct 6 screenshot correction. WHERE: Explicit inspected host bindings; no compiled drive/device identity.
WHY: An opaque browser window and nested tag violate the approved product behavior.
HOW: Inspect -> backup -> exact-source patch -> native qualification -> bounded restart -> receipt.
LICENSE: MIT
#>
[CmdletBinding()]
param([Parameter(Mandatory=$true)][ValidateSet('Backup','StartVoice','InspectSpeech')][string]$Phase,[Parameter(Mandatory=$true)][string]$BodyRoot,[Parameter(Mandatory=$true)][string]$EvidenceRoot)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$runtime=Join-Path $BodyRoot 'runtime'
if((& git -C $runtime remote get-url origin|Out-String).Trim() -ne 'https://github.com/4citeB4U/Leeway-Runtime-Fabric.git'){throw 'RUNTIME_ORIGIN_MISMATCH'}
if($Phase -eq 'Backup'){
 if(Test-Path $EvidenceRoot){throw 'BACKUP_DESTINATION_EXISTS'}
 [void][IO.Directory]::CreateDirectory($EvidenceRoot)
 $files=@('Start-Agent-Lee.ps1','host-commander.binding.v1.json','runtime\machine-consciousness\pocket-voice.html','runtime\machine-consciousness\carrier-server.mjs','runtime\machine-consciousness\Start-Pocket-Edge-Overlay.ps1','runtime\machine-consciousness\pc-brain-view.mjs')
 $records=@();$i=0
 foreach($rel in $files){$p=Join-Path $BodyRoot $rel;$name=([string]$i)+'-'+[IO.Path]::GetFileName($rel);Copy-Item -LiteralPath $p -Destination (Join-Path $EvidenceRoot $name);$records+=@{resource=$rel;backup=$name;sha256=(Get-FileHash $p -Algorithm SHA256).Hash};$i++}
 [ordered]@{status='ORIGINAL_INSTALLED_FILES_BACKED_UP';sourceCommit=(& git -C $runtime rev-parse HEAD|Out-String).Trim();files=$records;phoneCleared=$false;goldenPromoted=$false}|ConvertTo-Json -Depth 7|Set-Content (Join-Path $EvidenceRoot 'backup-manifest.json') -Encoding UTF8
 'BACKUP_COMPLETE'
}elseif($Phase -eq 'StartVoice'){
 $script=Join-Path $BodyRoot 'voice\adapters\start-studio.py'
 & python $script --check;if($LASTEXITCODE -ne 0){throw 'EXISTING_VOICE_PREFLIGHT_FAILED'}
 $p=Start-Process (Get-Command python).Source -ArgumentList @('-u',('"'+$script+'"')) -WorkingDirectory (Join-Path $BodyRoot 'voice') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $EvidenceRoot 'voice.stdout.log') -RedirectStandardError (Join-Path $EvidenceRoot 'voice.stderr.log')
 [ordered]@{pid=$p.Id;sourceHash=(Get-FileHash $script -Algorithm SHA256).Hash;voiceSelectionChanged=$false;status='EXISTING_VOICE_SUPERVISOR_STARTED_NOT_YET_AUDIBLE'}|ConvertTo-Json|Set-Content (Join-Path $EvidenceRoot 'voice-start.json') -Encoding UTF8
 Write-Output ('VOICE_SUPERVISOR_PID='+$p.Id)
}else{
 Add-Type -AssemblyName System.Speech
 [System.Speech.Recognition.SpeechRecognitionEngine]::InstalledRecognizers()|Select-Object Id,Name,Culture|ConvertTo-Json
}

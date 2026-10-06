<#
REGION: LEEWAY.POCKET.RELEASE_QUALIFICATION
TAG: EXTEND_EXISTING_SINGLE_APK_GATE
WHO: Creator-authorized Agent Lee
WHAT: Extend, not replace, the current single-APK qualification entrypoint.
WHEN: Golden customer package definition; WHERE: existing Pocket repository.
WHY: Fail with explicit missing artifact/profile/evidence requirements instead of a misleading score.
HOW: Verify source hash, parse replacement, preserve rollback and rehash.
LICENSE: MIT
#>
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if((& git -C $root remote get-url origin|Out-String).Trim() -ne 'https://github.com/4citeB4U/LeeWay-Pocket-Agent.git'){throw 'POCKET_ORIGIN_REQUIRED'}
$path=Join-Path $root 'Verify-SingleApk.ps1'
if((Get-FileHash $path -Algorithm SHA256).Hash -ne '5A9310DD12810629885ECEECA4BF7D2CFE5C5CF97FBF0B665123B295CD2D8DFF'){throw 'EXISTING_GATE_CHANGED_REVIEW_REQUIRED'}
$old=[IO.File]::ReadAllText($path)
$new=@'
<#
REGION: LEEWAY.POCKET.RELEASE_QUALIFICATION
TAG: SINGLE_APK_SOURCE_AND_GOLDEN_EVIDENCE_GATE
WHO: Creator-authorized release harness; WHAT: Preserve source checks and require golden evidence.
WHEN: Before distribution; WHERE: Pocket build tooling, never a new runtime.
WHY: Compiling, existing filenames and a census percentage do not establish customer acceptance.
HOW: Source-only checks remain labelled; full request delegates to hash-bound evidence assessment.
LICENSE: MIT
#>
[CmdletBinding()]
param([switch]$SourceOnly,[string]$ApkPath,[string]$SourceCommit,[string[]]$SupportedProfiles=@(),[string]$EvidenceDossier,[string]$EvidenceRoot,[string]$AssessmentOut)
$ErrorActionPreference='Stop'
$root=Join-Path $PSScriptRoot 'app\src\main'
$forbidden=@('industries\.leeway\.devicebridge','industries\.leeway\.readaloud','N8nBridge','n8n_endpoint','n8n_token')
$hits=Get-ChildItem $root -Recurse -File|Select-String -Pattern $forbidden -ErrorAction SilentlyContinue
if($hits){$hits|Format-Table Path,LineNumber,Pattern -AutoSize;throw 'SINGLE_APK_EXTERNAL_DEPENDENCY_FAIL'}
$required=@(
 'assets\agent_lee_sphere_transparent.html',
 'java\industries\leeway\pocket\FoldDigitalBrain.kt',
 'java\industries\leeway\pocket\LeeWayBodyDatabases.kt',
 'java\industries\leeway\pocket\LeeWayDeviceService.kt',
 'java\industries\leeway\pocket\LeeWayAutomation.kt',
 'java\industries\leeway\pocket\UnifiedAgentLeeRuntime.kt',
 'java\industries\leeway\pocket\PocketOverlayService.kt'
)
foreach($r in $required){if(!(Test-Path (Join-Path $root $r))){throw "SINGLE_APK_REQUIRED_COMPONENT_MISSING:$r"}}
$db=Get-Content (Join-Path $root 'java\industries\leeway\pocket\LeeWayBodyDatabases.kt') -Raw
# Relative implementation structure only. A particular device's database filename is not a universal requirement.
foreach($name in @('BrainDb','ContinuumDb','LdwmdDb')){if($db -notmatch ('class\s+'+[regex]::Escape($name)+'\b')){throw "SINGLE_APK_DATABASE_STRUCTURE_MISSING:$name"}}
'LEEWAY_SINGLE_APK_SOURCE_STRUCTURE_PASS_NOT_RUNTIME_ACCEPTANCE'
if($SourceOnly){return}
if(!$ApkPath -or !$SourceCommit){throw 'GOLDEN_ARTIFACT_AND_SOURCE_IDENTITY_REQUIRED'}
$args=@((Join-Path $PSScriptRoot 'scripts\golden-release-gate.mjs'),'--apk',$ApkPath,'--source-commit',$SourceCommit)
if($SupportedProfiles.Count){$args+=@('--profiles',($SupportedProfiles -join ','))}
if($EvidenceDossier){$args+=@('--dossier',$EvidenceDossier)}
if($EvidenceRoot){$args+=@('--evidence-root',$EvidenceRoot)}
if($AssessmentOut){$args+=@('--out',$AssessmentOut)}
& node @args
$code=$LASTEXITCODE
if($code -ne 0){throw 'GOLDEN_CUSTOMER_RELEASE_BLOCKED_SEE_ASSESSMENT'}
# This source entrypoint never signs, publishes, merges, installs or overrides the existing Veritas release authority.
'GOLDEN_EVIDENCE_ASSESSED_RELEASE_PROMOTION_NOT_EXECUTED'
'@
$t=$null;$e=$null;$null=[System.Management.Automation.Language.Parser]::ParseInput($new,[ref]$t,[ref]$e)
if($e.Count){throw 'GOLDEN_GATE_PARSE_FAILED'}
$backup=Join-Path $root ('qualification\gate-rollback-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ')+'.ps1')
[IO.File]::WriteAllText($backup,$old,(New-Object Text.UTF8Encoding($false)))
[IO.File]::WriteAllText($path,$new,(New-Object Text.UTF8Encoding($false)))
[pscustomobject]@{state='SOURCE_GATE_EXTENDED_NOT_RELEASE_ACCEPTED';beforeSha256='5A9310DD12810629885ECEECA4BF7D2CFE5C5CF97FBF0B665123B295CD2D8DFF';afterSha256=(Get-FileHash $path -Algorithm SHA256).Hash;backup=$backup;phoneModified=$false}|ConvertTo-Json

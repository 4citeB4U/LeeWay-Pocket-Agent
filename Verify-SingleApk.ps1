param([switch]$SourceOnly)
$ErrorActionPreference='Stop'
$root=Join-Path $PSScriptRoot 'app\src\main'
$forbidden=@('industries\.leeway\.devicebridge','industries\.leeway\.readaloud','N8nBridge','n8n_endpoint','n8n_token')
$hits=Get-ChildItem $root -Recurse -File|Select-String -Pattern $forbidden -ErrorAction SilentlyContinue
if($hits){$hits|Format-Table Path,LineNumber,Line -AutoSize;throw 'SINGLE_APK_EXTERNAL_DEPENDENCY_FAIL'}
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
foreach($name in @('leeway-digital-brain-phone-fold6.db','leeway-continuum-phone-fold6.db','leeway-ldwmd-phone-fold6.db')){if($db -notmatch [regex]::Escape($name)){throw "SINGLE_APK_DATABASE_MISSING:$name"}}
'LEEWAY_SINGLE_APK_SOURCE_STRUCTURE_PASS_NOT_RUNTIME_ACCEPTANCE'
if(-not $SourceOnly){throw 'FULL_APK_RUNTIME_ACCEPTANCE_NOT_PROVEN'}

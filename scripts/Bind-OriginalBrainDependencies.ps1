<#
REGION: LEEWAY.BRAIN.PACKAGING
TAG: PIN_ORIGINAL_THREE_RENDERER_DEPENDENCIES
WHO: Creator-authorized build; WHAT: Package the exact Three r160 dependency named by original import map.
WHEN: Viewer build; WHERE: existing APK assets, not global install or runtime network download.
WHY: The local Brain renderer must not need CDN access or load unpinned remote scripts.
HOW: Primary upstream URLs, expected Git blob SHA, SHA-256 manifest and retained upstream license.
LICENSE: MIT
#>
[CmdletBinding()]
param([switch]$Apply)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if((& git -C $root remote get-url origin|Out-String).Trim() -ne 'https://github.com/4citeB4U/LeeWay-Pocket-Agent.git'){throw 'SOURCE_ORIGIN_MISMATCH'}
$files=@(
 @{source='build/three.module.js';target='three.module.js';blob='0bcc7a286da2c115853ceec9deea19923e10ddc1'},
 @{source='examples/jsm/controls/OrbitControls.js';target='OrbitControls.js';blob='f29e7feb0fe53082099bf718233e2c1baa8a5627'},
 @{source='LICENSE';target='THREE-LICENSE.txt';blob='d07e209686512b9ac93d7df5481a4a6f622093e7'}
)
if(!$Apply){$files|ConvertTo-Json;return}
$dest=Join-Path $root 'app/src/main/assets/digital-brain/vendor'
$null=[IO.Directory]::CreateDirectory($dest)
$records=@()
foreach($entry in $files){
 $path=Join-Path $dest $entry.target
 if(Test-Path -LiteralPath $path){throw ('DEPENDENCY_EXISTS_VERIFY_BEFORE_REPLACING:'+ $entry.target)}
 $temp=$path+'.candidate'
 $url='https://raw.githubusercontent.com/mrdoob/three.js/r160/'+$entry.source
 Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $temp -TimeoutSec 40
 $actual=(& git -C $root hash-object --no-filters $temp|Out-String).Trim()
 if($actual -ne $entry.blob){throw ('UPSTREAM_BLOB_MISMATCH:'+ $entry.source)}
 Move-Item -LiteralPath $temp -Destination $path
 $records+=@{source=$url;gitBlob=$actual;target='vendor/'+$entry.target;sha256=(Get-FileHash $path -Algorithm SHA256).Hash;bytes=(Get-Item $path).Length}
}
[ordered]@{schemaVersion='leeway.viewer-dependency-lock.v1';threeVersion='0.160.0';runtimeNetworkRequired=$false;dependencies=$records}|ConvertTo-Json -Depth 8|Set-Content (Join-Path $root 'contracts/original-brain-viewer-dependencies.v1.json') -Encoding UTF8
$records|ConvertTo-Json -Depth 8

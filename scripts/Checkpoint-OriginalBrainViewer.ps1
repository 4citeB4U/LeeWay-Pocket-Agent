<#
REGION: LEEWAY.BRAIN.QUALIFICATION
TAG: ORIGINAL_VIEWER_SCOPE_AND_EVIDENCE_CLOSURE
WHO: Creator-authorized Agent Lee; WHAT: Record build, host SQL, protocol and browser qualifications.
WHEN: After repair/retest; WHERE: current source and scoped qualification outputs only.
WHY: A rendered browser fixture is not an installed phone, live SQLite bridge or Golden release.
HOW: Exact source and artifact hashes; preserved test evidence and bounded APK class/asset inspection.
LICENSE: MIT
#>
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$QualificationDirectory,[Parameter(Mandatory=$true)][string]$BrowserDirectory)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if((& git -C $root remote get-url origin|Out-String).Trim() -ne 'https://github.com/4citeB4U/LeeWay-Pocket-Agent.git'){throw 'SOURCE_ORIGIN_MISMATCH'}
$allowed=(Join-Path $root 'qualification')+[IO.Path]::DirectorySeparatorChar
foreach($p in @($QualificationDirectory,$BrowserDirectory)){if(!(Get-Item $p).FullName.StartsWith($allowed,[StringComparison]::OrdinalIgnoreCase)){throw 'QUALIFICATION_SCOPE_INVALID'}}
$q=Get-Content (Join-Path $QualificationDirectory 'receipt.json') -Raw|ConvertFrom-Json
$b=Get-Content (Join-Path $BrowserDirectory 'browser-receipt.json') -Raw|ConvertFrom-Json
if($q.status -ne 'PASS_SCOPED_SOFTWARE_QUALIFICATION_ONLY' -or !$q.sourceStable -or $q.fixedIdentityHits -ne 0){throw 'BUILD_QUALIFICATION_REQUIRED'}
if($b.status -ne 'BROWSER_FIXTURE_PASS_NOT_DEVICE_ACCEPTANCE' -or @($b.errors).Count -ne 0 -or @($b.tests).Count -ne 23){throw 'BROWSER_QUALIFICATION_REQUIRED'}
$manifest=Join-Path $QualificationDirectory 'source-hashes.json'
if((Get-FileHash $manifest -Algorithm SHA256).Hash -ne $q.sourceManifestSha256){throw 'SOURCE_MANIFEST_CHANGED'}
foreach($entry in (Get-Content $manifest -Raw|ConvertFrom-Json)){if((Get-FileHash -LiteralPath (Join-Path $root $entry.path) -Algorithm SHA256).Hash -ne $entry.sha256){throw ('QUALIFIED_SOURCE_CHANGED:'+ $entry.path)}}
$apk=Join-Path $root 'app/build/outputs/apk/debug/app-debug.apk'
if((Get-FileHash $apk -Algorithm SHA256).Hash -ne $q.apkSha256){throw 'APK_CHANGED_AFTER_QUALIFICATION'}
$tap=Get-Content (Join-Path $QualificationDirectory 'node-tests.tap') -Raw
$nodePass=[int]([regex]::Match($tap,'(?m)^# pass (\d+)').Groups[1].Value)
$nodeFail=[int]([regex]::Match($tap,'(?m)^# fail (\d+)').Groups[1].Value)
$junit=[int](($q.junitSuites|Measure-Object tests -Sum).Sum)
if($nodePass -ne 47 -or $nodeFail -ne 0 -or $junit -ne 105){throw 'QUALIFICATION_COUNTS_CHANGED'}
if(@($q.junitSuites|Where-Object{$_.failures -or $_.errors -or $_.skipped}).Count){throw 'JUNIT_INCOMPLETE'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::OpenRead($apk)
$classes=[ordered]@{'Lindustries/leeway/brain/BrainViewer;'=$false;'Lindustries/leeway/pocket/AndroidBrainViewer;'=$false;'Lindustries/leeway/pocket/DigitalBrainActivity;'=$false}
$assets=@()
try {
 foreach($entry in $zip.Entries|Where-Object{$_.FullName -match '^classes\d*\.dex$'}){
  $stream=$entry.Open();$buffer=New-Object IO.MemoryStream
  try{$stream.CopyTo($buffer);$text=[Text.Encoding]::ASCII.GetString($buffer.ToArray())}finally{$stream.Dispose();$buffer.Dispose()}
  foreach($key in @($classes.Keys)){if($text.Contains($key)){$classes[$key]=$true}}
 }
 foreach($name in @('brain.html','local-brain-binding.js','nucleus.png','vendor/three.module.js','vendor/OrbitControls.js','vendor/THREE-LICENSE.txt')){
  $entry=$zip.GetEntry('assets/digital-brain/'+$name);if(!$entry){throw ('VIEWER_APK_ASSET_MISSING:'+ $name)}
  $stream=$entry.Open();$sha=[Security.Cryptography.SHA256]::Create()
  try{$h=([BitConverter]::ToString($sha.ComputeHash($stream))).Replace('-','')}finally{$stream.Dispose();$sha.Dispose()}
  if($h -ne (Get-FileHash -LiteralPath (Join-Path $root ('app/src/main/assets/digital-brain/'+$name)) -Algorithm SHA256).Hash){throw 'PACKAGED_ASSET_HASH_MISMATCH'}
  $assets+=@{path=$name;sha256=$h;uncompressedBytes=$entry.Length;apkCompressedBytes=$entry.CompressedLength}
 }
}finally{$zip.Dispose()}
if($classes.Values -contains $false){throw 'VIEWER_CLASSES_NOT_PACKAGED'}
$run=$q.runId.Replace('portable-qualification-','original-viewer-')
$dest=Join-Path $root ('docs/qualification/'+$run)
if(Test-Path -LiteralPath $dest){throw 'EVIDENCE_DESTINATION_EXISTS'}
$null=[IO.Directory]::CreateDirectory($dest)
foreach($name in @('receipt.json','source-hashes.json','node-tests.tap','gradle.log')){Copy-Item -LiteralPath (Join-Path $QualificationDirectory $name) -Destination (Join-Path $dest $name)}
$browserDest=Join-Path $dest 'browser';$null=[IO.Directory]::CreateDirectory($browserDest)
Copy-Item -LiteralPath (Join-Path $BrowserDirectory 'browser-receipt.json') -Destination (Join-Path $browserDest 'browser-receipt.json')
foreach($image in $b.screenshots){$p=Join-Path $BrowserDirectory $image.name;if((Get-FileHash $p -Algorithm SHA256).Hash.ToLowerInvariant() -ne $image.sha256){throw 'BROWSER_SCREENSHOT_CHANGED'};Copy-Item $p (Join-Path $browserDest $image.name)}
$reports=Join-Path $dest 'junit';$null=[IO.Directory]::CreateDirectory($reports)
foreach($folder in @('brain-core/build/test-results/test','app/build/test-results/testDebugUnitTest')){
 foreach($file in Get-ChildItem -LiteralPath (Join-Path $root $folder) -Filter 'TEST-*.xml' -File){
  [xml]$xml=Get-Content -LiteralPath $file.FullName -Raw;$record=@($q.junitSuites|Where-Object{$_.suite -eq $xml.testsuite.name})
  if($record.Count -ne 1 -or (Get-FileHash $file.FullName -Algorithm SHA256).Hash -ne $record[0].reportSha256){throw 'JUNIT_EVIDENCE_CHANGED'}
  Copy-Item -LiteralPath $file.FullName -Destination (Join-Path $reports $file.Name)
 }
}
$projection=Get-Content (Join-Path $root 'contracts/original-brain-viewer-projection.v1.json') -Raw|ConvertFrom-Json
$receipt=[ordered]@{
 schemaVersion='leeway.original-brain-viewer-checkpoint.v1'
 status='SCOPED_SOFTWARE_AND_BROWSER_PASS_PRODUCT_BLOCKED'
 sourceBase=$q.sourceBase
 sourceManifestSha256=$q.sourceManifestSha256
 qualificationReceiptSha256=(Get-FileHash (Join-Path $dest 'receipt.json') -Algorithm SHA256).Hash
 browserReceiptSha256=(Get-FileHash (Join-Path $browserDest 'browser-receipt.json') -Algorithm SHA256).Hash
 nodeTestsPassed=$nodePass
 jvmTestsPassed=$junit
 browserChecksPassed=@($b.tests).Count
 aggregateChecksPassed=$nodePass+$junit+@($b.tests).Count
 skipped=0
 failures=0
 browserData='EXPLICIT_FIXTURE_RECORDS_NOT_PHONE_DATABASE'
 hostSql='ACTUAL_ADAPTER_SQL_EXECUTED_WITH_FIXTURE_ROWS_NOT_ANDROID_SQLITE'
 androidNativeBridge='COMPILED_AND_PACKAGED_NOT_EXECUTED_ON_PHONE'
 shapeReuse='ORIGINAL_3D_SCRIPT_RETAINED_BYTE_FOR_BYTE_WITH_VIEWPORT_ADAPTER_EXTENSION'
 recoveredRendererSha256=$projection.recoveredOriginalSha256
 originalNucleusSha256=$projection.originalNucleusSha256
 olderGoldenLockMismatchRemains=$true
 visualPromotion='CANDIDATE_NOT_GOLDEN_OR_CREATOR_DEVICE_PARITY_APPROVAL'
 readonlyViewer='ROOT_CHILDREN_NODE_RELATIONSHIP_PROVENANCE_SEARCH_ONLY'
 externalRuntimeNetworkRequired=$false
 voiceIdentityChanged=$false
 fullCustomerCasesAdmitted=0
 apkSha256=$q.apkSha256
 apkBytes=$q.apkBytes
 packagedClasses=$classes
 packagedAssets=$assets
 leewayCondensationApplied=$false
 installed=$false
 releasePromoted=$false
 formulaExecution='NOT_EXECUTED'
 learningLedgerUpdated=$false
}
$receiptPath=Join-Path $dest 'viewer-checkpoint.json';$receipt|ConvertTo-Json -Depth 10|Set-Content -LiteralPath $receiptPath -Encoding UTF8
$receipt|ConvertTo-Json -Depth 10
Write-Output ('VIEWER_EVIDENCE_DIRECTORY='+$dest)
Write-Output ('VIEWER_RECEIPT_SHA256='+(Get-FileHash $receiptPath -Algorithm SHA256).Hash)

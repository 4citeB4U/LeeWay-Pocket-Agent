<#
REGION: LEEWAY.BRAIN.QUALIFICATION
TAG: INGESTION_EVIDENCE_CHECKPOINT
WHO: Creator-authorized Agent Lee.
WHAT: Record actual build/test results and reject incomplete customer release.
WHEN: After ingestion qualification; WHERE: current verified repository only.
WHY: Compiled adapters and host tests are not phone or full-product acceptance.
HOW: Match source and artifact hashes, inspect APK classes, preserve logs and explicitly scoped receipt.
LICENSE: MIT
#>
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$QualificationDirectory)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if((& git -C $root remote get-url origin|Out-String).Trim() -ne 'https://github.com/4citeB4U/LeeWay-Pocket-Agent.git'){throw 'SOURCE_ORIGIN_MISMATCH'}
if((& git -C $root branch --show-current|Out-String).Trim() -ne 'repair/portable-golden-brain-bootstrap'){throw 'BRANCH_MISMATCH'}
$dir=(Get-Item -LiteralPath $QualificationDirectory).FullName
$allowed=Join-Path $root 'qualification'
if(!$dir.StartsWith($allowed+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'QUALIFICATION_OUTSIDE_ALLOWED_SCOPE'}
$q=Get-Content (Join-Path $dir 'receipt.json') -Raw|ConvertFrom-Json
if($q.status -ne 'PASS_SCOPED_SOFTWARE_QUALIFICATION_ONLY' -or !$q.sourceStable -or $q.fixedIdentityHits -ne 0){throw 'QUALIFICATION_NOT_PASSED'}
$manifest=Join-Path $dir 'source-hashes.json'
if((Get-FileHash $manifest -Algorithm SHA256).Hash -ne $q.sourceManifestSha256){throw 'SOURCE_MANIFEST_CHANGED'}
foreach($entry in (Get-Content $manifest -Raw|ConvertFrom-Json)){if((Get-FileHash -LiteralPath (Join-Path $root $entry.path) -Algorithm SHA256).Hash -ne $entry.sha256){throw ('SOURCE_CHANGED:'+ $entry.path)}}
$apk=Join-Path $root 'app/build/outputs/apk/debug/app-debug.apk'
if((Get-FileHash $apk -Algorithm SHA256).Hash -ne $q.apkSha256){throw 'QUALIFIED_APK_CHANGED'}
$tap=Get-Content (Join-Path $dir 'node-tests.tap') -Raw
$nodePass=[int]([regex]::Match($tap,'(?m)^# pass (\d+)').Groups[1].Value)
$nodeFail=[int]([regex]::Match($tap,'(?m)^# fail (\d+)').Groups[1].Value)
$nodeSkipped=[int]([regex]::Match($tap,'(?m)^# skipped (\d+)').Groups[1].Value)
if($nodePass -ne 29 -or $nodeFail -or $nodeSkipped){throw 'NODE_COUNTS_UNEXPECTED'}
$junit=[int](($q.junitSuites|Measure-Object tests -Sum).Sum)
if(@($q.junitSuites|Where-Object{$_.failures -or $_.errors -or $_.skipped}).Count -or $junit -ne 86){throw 'JUNIT_COUNTS_UNEXPECTED'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::OpenRead($apk)
$classes=[ordered]@{
 'Lindustries/leeway/brain/BrainIngestion;'=$false
 'Lindustries/leeway/pocket/AndroidBrainIngestion;'=$false
 'Lindustries/leeway/pocket/NativeFileCensus;'=$false
}
try {
 foreach($entry in $zip.Entries|Where-Object{$_.FullName -match '^classes\d*\.dex$'}){
  $stream=$entry.Open();$buffer=New-Object IO.MemoryStream
  try{$stream.CopyTo($buffer);$text=[Text.Encoding]::ASCII.GetString($buffer.ToArray())}finally{$stream.Dispose();$buffer.Dispose()}
  foreach($key in @($classes.Keys)){if($text.Contains($key)){$classes[$key]=$true}}
 }
}finally{$zip.Dispose()}
if($classes.Values -contains $false){throw 'INGESTION_CLASSES_NOT_PACKAGED'}
$evidence=Join-Path $root ('docs/qualification/brain-ingestion-'+$q.runId.Replace('portable-qualification-',''))
if(Test-Path -LiteralPath $evidence){throw 'EVIDENCE_DESTINATION_ALREADY_EXISTS'}
$null=[IO.Directory]::CreateDirectory($evidence)
foreach($name in @('receipt.json','source-hashes.json','node-tests.tap','gradle.log')){Copy-Item -LiteralPath (Join-Path $dir $name) -Destination (Join-Path $evidence $name)}
$reports=Join-Path $evidence 'junit';$null=[IO.Directory]::CreateDirectory($reports)
foreach($folder in @('brain-core/build/test-results/test','app/build/test-results/testDebugUnitTest')){
 foreach($file in Get-ChildItem -LiteralPath (Join-Path $root $folder) -Filter 'TEST-*.xml' -File){
  [xml]$xml=Get-Content -LiteralPath $file.FullName -Raw
  $record=@($q.junitSuites|Where-Object{$_.suite -eq $xml.testsuite.name})
  if($record.Count -ne 1 -or (Get-FileHash $file.FullName -Algorithm SHA256).Hash -ne $record[0].reportSha256){throw 'JUNIT_REPORT_CHANGED'}
  Copy-Item -LiteralPath $file.FullName -Destination (Join-Path $reports $file.Name)
 }
}
$checkpoint=[ordered]@{
 schemaVersion='leeway.brain-ingestion-qualification.v1'
 status='SCOPED_SOFTWARE_PASS_PRODUCT_BLOCKED'
 sourceBase=$q.sourceBase
 sourceManifestSha256=$q.sourceManifestSha256
 qualificationReceiptSha256=(Get-FileHash (Join-Path $evidence 'receipt.json') -Algorithm SHA256).Hash
 nodeTests=$nodePass
 jvmTests=$junit
 totalSoftwareTests=$nodePass+$junit
 failures=0
 skipped=0
 newSharedReconciliationTests=19
 nativeCensusTests=15
 nativeCensusScope='HOST_TEMPORARY_FILES_PLUS_EXPLICIT_ROOT_IDENTITY_FIXTURES; NOT_ANDROID_RUNTIME'
 sharedStoreScope='TRANSACTIONAL_TEST_STORE; ANDROID_SQLITE_RUNTIME_NOT_EXECUTED'
 deployedSensorScope='NOT_DEPLOYED; APP_PRIVATE_FILES_ONLY_WHEN_QUALIFIED'
 androidLifecycle='COMPILED_FILEOBSERVER_AND_EXISTING_OVERLAY_SERVICE_NOT_DEVICE_TESTED'
 freshness180Seconds='NOT_MEASURED_ON_DEVICE'
 sourceContentExtraction='NOT_IMPLEMENTED_BY_THIS_METADATA_CENSUS'
 originalRecursiveViewer='NOT_YET_INTEGRATED'
 continuum='CANONICAL_RUNTIME_IDENTIFIED_NOT_BOUND_IN_APK'
 ldwmd='CANONICAL_RUNTIME_IDENTIFIED_NOT_BOUND_IN_APK'
 apkSha256=$q.apkSha256
 apkBytes=$q.apkBytes
 packagedClasses=$classes
 formulaExecution='NOT_EXECUTED'
 installed=$false
 releasePromoted=$false
 learningLedgerUpdated=$false
}
$checkpoint|ConvertTo-Json -Depth 10|Set-Content -LiteralPath (Join-Path $evidence 'ingestion-checkpoint.json') -Encoding UTF8
$checkpoint|ConvertTo-Json -Depth 10
Write-Output ('EVIDENCE_DIRECTORY='+$evidence)

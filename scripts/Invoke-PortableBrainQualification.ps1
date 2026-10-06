<#
REGION: LEEWAY.BRAIN.QUALIFICATION
TAG: EXECUTE_PORTABLE_AND_NATIVE_BUILD_PROOF
WHO: Creator-authorized Agent Lee.
WHAT: Execute the repaired core, existing Android tests, build and rejection gate.
WHEN: After the scoped source repair; WHERE: verified source checkout and private qualification output.
WHY: File existence is not compilation, behavior or device acceptance.
HOW: Record native exit codes, TAP/JUnit results, source hashes and actual APK bytes; never install.
LICENSE: MIT
#>
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$GradlePath,[switch]$Online)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if((& git -C $root remote get-url origin|Out-String).Trim() -ne 'https://github.com/4citeB4U/LeeWay-Pocket-Agent.git'){throw 'SOURCE_ORIGIN_MISMATCH'}
$run='portable-qualification-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ')
$dir=Join-Path $root ('qualification/'+$run)
$null=[IO.Directory]::CreateDirectory($dir)
$inputs=@(Get-ChildItem (Join-Path $root 'app/src'),(Join-Path $root 'brain-core/src'),(Join-Path $root 'contracts'),(Join-Path $root 'tests') -Recurse -File|Where-Object {$_.Extension -in '.kt','.java','.html','.json','.xml','.mjs'})
$inputs+=@('build.gradle.kts','settings.gradle.kts','app/build.gradle.kts','brain-core/build.gradle.kts','scripts/golden-release-gate.mjs','Verify-SingleApk.ps1'|ForEach-Object{Get-Item (Join-Path $root $_)})
$before=@($inputs|Sort-Object FullName -Unique|ForEach-Object{[ordered]@{path=$_.FullName.Substring($root.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash}})
$before|ConvertTo-Json -Depth 5|Set-Content (Join-Path $dir 'source-hashes.json') -Encoding UTF8
$receipt=[ordered]@{schemaVersion='leeway.portable-brain-qualification.v1';runId=$run;sourceBase=(& git -C $root rev-parse HEAD|Out-String).Trim();sourceManifestSha256=(Get-FileHash (Join-Path $dir 'source-hashes.json') -Algorithm SHA256).Hash;scope='PORTABLE_BOOTSTRAP_AND_BUILD_NOT_FULL_PRODUCT_OR_DEVICE_ACCEPTANCE';status='RUNNING';nodeTestExit=$null;gradleExit=$null;fixedIdentityHits=$null;phoneModified=$false;installed=$false;releasePromoted=$false;formulaExecution='NOT_EXECUTED';learningLedgerUpdated=$false}
Push-Location $root
try {
 $oldPreference=$ErrorActionPreference;$ErrorActionPreference='Continue'
 & node --test --test-reporter=tap tests/golden-release-gate.test.mjs tests/sphere-voice-boundary.test.mjs tests/portable-brain-wiring.test.mjs *> (Join-Path $dir 'node-tests.tap')
 $receipt.nodeTestExit=$LASTEXITCODE;$ErrorActionPreference=$oldPreference
 if($receipt.nodeTestExit -ne 0){throw 'NODE_REGRESSIONS_FAILED'}
 & '.\Verify-SingleApk.ps1' -SourceOnly
 $hits=@(Get-ChildItem 'app/src/main','brain-core/src/main' -Recurse -File|Where-Object {$_.Extension -in '.kt','.java','.html','.json','.xml'}|Select-String -Pattern 'phone-fold6|FoldDigitalBrain|Fold6')
 $receipt.fixedIdentityHits=$hits.Count
 if($hits.Count){throw 'FIXED_IDENTITY_REMAINS_IN_BUILD_INPUTS'}
 $gradleArgs=@(':brain-core:test',':app:testDebugUnitTest',':app:assembleDebug','--console=plain')
 if(!$Online){$gradleArgs+='--offline'}
 $ErrorActionPreference='Continue'
 & $GradlePath @gradleArgs *> (Join-Path $dir 'gradle.log')
 $receipt.gradleExit=$LASTEXITCODE;$ErrorActionPreference=$oldPreference
 if($receipt.gradleExit -ne 0){throw 'GRADLE_QUALIFICATION_FAILED'}
 $reports=@(Get-ChildItem 'brain-core/build/test-results/test','app/build/test-results/testDebugUnitTest' -Filter 'TEST-*.xml' -File)
 if(!$reports.Count){throw 'JUNIT_REPORTS_MISSING'}
 $suites=@(foreach($f in $reports){[xml]$xml=Get-Content -LiteralPath $f.FullName -Raw;[ordered]@{suite=[string]$xml.testsuite.name;tests=[int]$xml.testsuite.tests;failures=[int]$xml.testsuite.failures;errors=[int]$xml.testsuite.errors;skipped=[int]$xml.testsuite.skipped;reportSha256=(Get-FileHash $f.FullName -Algorithm SHA256).Hash}})
 if(@($suites|Where-Object{$_.failures -or $_.errors -or $_.skipped}).Count){throw 'JUNIT_INCOMPLETE_OR_FAILED'}
 $receipt['junitSuites']=$suites
 $apk=Join-Path $root 'app/build/outputs/apk/debug/app-debug.apk'
 if(!(Test-Path -LiteralPath $apk)){throw 'BUILT_APK_MISSING'}
 $receipt['apkSha256']=(Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash
 $receipt['apkBytes']=(Get-Item -LiteralPath $apk).Length
 $receipt['artifactKind']='DEBUG_CANDIDATE_NOT_CUSTOMER_RELEASE'
 Add-Type -AssemblyName System.IO.Compression.FileSystem
 $zip=[IO.Compression.ZipFile]::OpenRead($apk)
 try {
  $corePresent=$false;$adapterPresent=$false;$oldPresent=$false
  foreach($entry in $zip.Entries|Where-Object{$_.FullName -match '^classes\d*\.dex$'}){
   $stream=$entry.Open();$buffer=New-Object IO.MemoryStream
   try{$stream.CopyTo($buffer);$text=[Text.Encoding]::ASCII.GetString($buffer.ToArray())}finally{$stream.Dispose();$buffer.Dispose()}
   if($text.Contains('Lindustries/leeway/brain/DigitalBrain;')){$corePresent=$true}
   if($text.Contains('Lindustries/leeway/pocket/AndroidDigitalBrainAdapter;')){$adapterPresent=$true}
   if($text.Contains('FoldDigitalBrain') -or $text.Contains('phone-fold6')){$oldPresent=$true}
  }
  $receipt['apkCorePresent']=$corePresent;$receipt['apkAdapterPresent']=$adapterPresent;$receipt['apkFixedIdentityPresent']=$oldPresent
  if(!$corePresent -or !$adapterPresent -or $oldPresent){throw 'APK_IMPLEMENTATION_IDENTITY_MISMATCH'}
 } finally {$zip.Dispose()}
 foreach($entry in $before){if((Get-FileHash -LiteralPath (Join-Path $root $entry.path) -Algorithm SHA256).Hash -ne $entry.sha256){throw 'SOURCE_CHANGED_DURING_QUALIFICATION'}}
 $receipt['sourceStable']=$true
 $receipt.status='PASS_SCOPED_SOFTWARE_QUALIFICATION_ONLY'
} catch {
 $receipt.status='FAILED';$receipt['error']=$_.Exception.Message
} finally {
 Pop-Location
 foreach($name in @('node-tests.tap','gradle.log')){if(Test-Path (Join-Path $dir $name)){$receipt[$name+'Sha256']=(Get-FileHash (Join-Path $dir $name) -Algorithm SHA256).Hash}}
 $rp=Join-Path $dir 'receipt.json';$receipt|ConvertTo-Json -Depth 12|Set-Content -LiteralPath $rp -Encoding UTF8
 Write-Output ('QUALIFICATION_DIR='+$dir);Write-Output ('STATUS='+$receipt.status)
 if($receipt.Contains('error')){Write-Output ('ERROR='+$receipt.error);if(Test-Path (Join-Path $dir 'gradle.log')){Get-Content (Join-Path $dir 'gradle.log') -Tail 65}}
 Write-Output ('RECEIPT_SHA256='+(Get-FileHash $rp -Algorithm SHA256).Hash)
}
if($receipt.status -eq 'FAILED'){exit 1}
$receipt|ConvertTo-Json -Depth 12

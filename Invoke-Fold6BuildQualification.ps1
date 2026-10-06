<#
REGION: LEEWAY.POCKET.QUALIFICATION
TAG: FOLD6-BUILD-QUALIFICATION
WHO: Creator-authorized Agent Lee engineering session
WHAT: Build and test the existing unified Android candidate without installing it.
WHEN: Explicit continuation of the single-APK build.
WHERE: Canonical Pocket checkout resolved from this script's directory.
WHY: Record compiler/test evidence separately from functional deployment readiness.
HOW: Verify origin/toolchain; hash source; execute Gradle; preserve log, exit and receipt.
LICENSE: MIT
#>
[CmdletBinding()]
param([switch]$Offline)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
$expectedOrigin='https://github.com/4citeB4U/LeeWay-Pocket-Agent.git'
$origin=(& git -C $root remote get-url origin | Out-String).Trim()
if($LASTEXITCODE -ne 0 -or $origin -ne $expectedOrigin){throw 'POCKET_ORIGIN_NOT_VERIFIED'}
$head=(& git -C $root rev-parse HEAD | Out-String).Trim()
if($LASTEXITCODE -ne 0){throw 'POCKET_HEAD_UNAVAILABLE'}
$sdk=Join-Path $env:LOCALAPPDATA 'Android\Sdk'
if(!(Test-Path -LiteralPath (Join-Path $sdk 'platforms\android-35\android.jar'))){throw 'ANDROID_35_SDK_MISSING'}
$gradle=Join-Path (Split-Path $root -Parent) 'tool-cache\gradle-8.9\bin\gradle.bat'
if(!(Test-Path -LiteralPath $gradle)){throw 'VERIFIED_GRADLE_89_MISSING'}
$runId='fold6-build-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ')
$evidence=Join-Path $root ('qualification\'+$runId)
$null=New-Item -ItemType Directory -Path $evidence
$sourceFiles=@(Get-ChildItem (Join-Path $root 'app\src') -Recurse -File)
$sourceFiles+=Get-Item (Join-Path $root 'app\build.gradle.kts'),(Join-Path $root 'build.gradle.kts'),(Join-Path $root 'settings.gradle.kts')
$hashes=@($sourceFiles | Sort-Object FullName | ForEach-Object {
 [ordered]@{path=$_.FullName.Substring($root.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash;bytes=$_.Length}
})
$hashFile=Join-Path $evidence 'source-sha256.json'
$hashes | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $hashFile -Encoding UTF8
$env:ANDROID_HOME=$sdk
$env:ANDROID_SDK_ROOT=$sdk
$log=Join-Path $evidence 'gradle.log'
$args=@(':app:assembleDebug',':app:testDebugUnitTest','--console=plain','--no-daemon')
if($Offline){$args+='--offline'}
$started=[DateTime]::UtcNow.ToString('o')
Push-Location $root
try {
 # Use PowerShell redirection to retain both Gradle streams. Native exit code is authoritative.
 $ErrorActionPreference='Continue'
 & $gradle @args *> $log
 $code=$LASTEXITCODE
} finally {Pop-Location; $ErrorActionPreference='Stop'}
$artifact=Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
$apk=$null
if($code -eq 0 -and (Test-Path -LiteralPath $artifact)){
 $apk=[ordered]@{path=$artifact;sha256=(Get-FileHash -LiteralPath $artifact -Algorithm SHA256).Hash;bytes=(Get-Item -LiteralPath $artifact).Length}
}
$changed=@($hashes | Where-Object { !(Test-Path -LiteralPath (Join-Path $root $_.path)) -or (Get-FileHash -LiteralPath (Join-Path $root $_.path) -Algorithm SHA256).Hash -ne $_.sha256 })
$result=[ordered]@{
 schemaVersion='leeway.pocket.build-qualification.v1';runId=$runId;sourceOrigin=$origin;baseCommit=$head;
 startedAt=$started;finishedAt=[DateTime]::UtcNow.ToString('o');gradleExitCode=$code;
 status=$(if($code -eq 0 -and $null -ne $apk -and $changed.Count -eq 0){'BUILD_AND_UNIT_TESTS_PASSED'}else{'FAILED'});
 sourceManifestSha256=(Get-FileHash -LiteralPath $hashFile -Algorithm SHA256).Hash;
 sourceChangedDuringBuild=$changed.Count;apk=$apk;logSha256=(Get-FileHash -LiteralPath $log -Algorithm SHA256).Hash;
 installed=$false;deviceAcceptance='NOT_EXECUTED';completeness='NOT_PROVEN';formulaExecution='NOT_EXECUTED';learningLedgerUpdated=$false
}
$receipt=Join-Path $evidence 'build-receipt.json'
$result | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $receipt -Encoding UTF8
$result | ConvertTo-Json -Depth 8
Write-Output ('RECEIPT='+$receipt)
Get-Content -LiteralPath $log -Tail 65
if($code -ne 0 -or $changed.Count -gt 0){exit 1}
